#include <WiFi.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>
#include <iostream>
using namespace std;

// WiFi credentials
const char* ssid = "ENGG2K3K";
IPAddress staticIP(10, 20, 30, 117);
IPAddress gateway(10, 20, 30, 145);
IPAddress subnet(255, 255, 255, 0);
WiFiUDP udp;
char packetBuffer[1000];
unsigned int localPort = 9999;  //Todo change needed

string carriageState;
char netState = 'i';
unsigned long timeSent;
unsigned long seqNum = random(1000, 30000);
int expectedSeq;

void setup() {
  // Initialize serial communication
  Serial.begin(9600);

  // Connect to WiFi
  connectToWiFi();
  udp.begin(1234);
}

void loop() {
  switch (netState) {
    case 'i':
      Serial.println("i");
      netCodeSendInit();
      netState = 'w';
      timeSent = millis();
      break;
    case 'w':
      Serial.println("w");
      netCodeRNA();
      if(millis()-timeSent>1000) {
        Serial.println("Failed to get ACKIN");
        netState = 'i';
      }
      break;
    case 'r':
      Serial.println("r");
      netCodeRNA();
      break;
  }
}

void netCodeRNA() {
  int packetSize = udp.parsePacket();
  Serial.print("packetSize: ");
  Serial.println(packetSize);
  if (packetSize) {
    // receive incoming UDP packets
    int len = udp.read(packetBuffer, 1000);
    if (len > 0) {
      packetBuffer[len - 1] = 0;
    }
    Serial.printf("UDP packet contents: %s\n", packetBuffer);
    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, packetBuffer);
    const String msgType = doc["message"];

    if(expectedSeq < (int)doc["sequence_number"]) {
      expectedSeq = (int)doc["sequence_number"];
    }else if(expectedSeq > (int)doc["sequence_number"]) {
      return;
    }

    JsonDocument doc1;
    JsonObject encoder = doc1.add<JsonObject>();
    encoder["client_type"] = "BR";
    encoder["client_id"] = "BR17";
    encoder["sequence_number"] = seqNum;
    seqNum++;

    //if Packet is status request send back status else if exec cmd save new status
    std::string packet = "";

    Serial.println(msgType);
    if (msgType[0] == 'S') {
      encoder["message"] = "STAT";
      encoder["state"] = carriageState;
      serializeJson(encoder, packet);
      Serial.println("recieved STATREQ sent back STAT");

      udp.beginPacket(gateway, 3017);
      udp.write(stringToUni8Arr(packet), packet.length);
      udp.endPacket();
    } else if (msgType[0] == 'E') {
      string temp(doc["cmd"]);
      carriageState = temp;
      encoder["message"] = "AKEX";

      serializeJson(encoder, packet);
      Serial.print("recieved EXEC executing: ");
      Serial.println(temp);

      udp.beginPacket(gateway, 3017);
      udp.write(stringToUni8Arr(packet), packet.length());
      udp.endPacket();
    } else if (msgType[0] == 'A') {
      carriageState = "Idle";
      netState = 'r';
      expectedSeq = (int)doc["sequence_number"];
    } 
  }
}

void netCodeUpdate() {
  JsonDocument doc;
  JsonObject encoder = doc.add<JsonObject>();
  encoder["client_type"] = "BR";
  encoder["client_id"] = "BR17";
  encoder["sequence_number"] = seqNum;
  seqNum++;
  encoder["message"] = "STAT";
  encoder["state"] = carriageState;

  std::string packet = "";
  serializeJson(encoder, packet);

  udp.beginPacket(gateway, 3017);
  udp.write(stringToUni8Arr(packet), packet.length());
  udp.endPacket();
}

void netCodeSendInit() {
  JsonDocument doc;
  JsonObject encoder = doc.add<JsonObject>();
  encoder["client_type"] = "BR";
  encoder["client_id"] = "BR17";
  encoder["sequence_number"] = seqNum;
  seqNum++;
  encoder["message"] = "BRIN";

  std::string packet = "";
  serializeJson(encoder, packet);

  udp.beginPacket(gateway, 3017);
  udp.write(stringToUni8Arr(packet), packet.length());
  udp.endPacket();
}

const uint8_t* stringToUni8Arr(const std::string& str) {
  const char* cstr = str.c_str();
  size_t len = strlen(cstr);

  uint8_t* temp = new uint8_t[len + 1];
  memcpy(temp, cstr, len + 1);

  return temp;
}

// Connect to WiFi
void connectToWiFi() {
  if (WiFi.config(staticIP, gateway, subnet) == false) {
    Serial.println("Configuration failed.");
  }

  Serial.println("Connecting to WiFi...");
  WiFi.begin(ssid);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting...");
    Serial.println(WiFi.status());
  }

  Serial.println("Connected to WiFi");
}