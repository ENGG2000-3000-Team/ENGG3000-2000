#include <WiFi.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>
#include <iostream>
#include <string.h>
using namespace std;

// Motor pins
const int motorPin1 = 18;
const int motorPin2 = 19;
const int motorPWM = 21;

// LED pins
const int ledRedPin = 16;
const int ledGreenPin = 5;
const int ledBluePin = 17;

const int ledRedPin2 = 15;
const int ledGreenPin2 = 4;
const int ledBluePin2 = 2;

// Ultrasonic sensor pins
const int trigPin = 13;
const int echoPin = 12;

// WiFi credentials/Net
const char* ssid = "ENGG2K3K";
IPAddress staticIP(10, 20, 30, 117);
IPAddress gateway(10, 20, 30, 145);
IPAddress subnet(255, 255, 255, 0);
WiFiUDP udp;
char packetBuffer[1000];
unsigned int localPort = 9999;  //Todo change needed
char netState = 'i';
unsigned long timeSent;
unsigned long seqNum = random(1000, 30000);
int expectedSeq;

// State variables
enum State {
  INITIALIZE,
  STOPPED,
  BACKWARDS_SLOW,
  ACCELERATING,
  CRUISE,
  SLOW_DOWN,
  STOP_AT_STATION,
  ERROR_HAZARD,
  E_STOP
};

State carriageState = INITIALIZE;  // Initial state
int errorRetryCount = 0;
bool ccpConnectionLost = false;
bool packetDelay = false;
bool doorsOpen = false;

// Motor control variables
int motorSpeed = 0;
int desiredSpeed = 0;
int Testspeed = 100;

// Function prototypes
void connectToWiFi();
void updateState();
void moveMotorForward();
void stopMotor();
void setLEDColor(int red, int green, int blue);
float getDistance();
void handleCarriageState();
void handleCommunicationErrors();
void openDoors();
void closeDoors();

void setup() {
  // Motor pin setup
  pinMode(motorPin1, OUTPUT);
  pinMode(motorPin2, OUTPUT);
  pinMode(motorPWM, OUTPUT);

  // LED pin setup
  pinMode(ledRedPin, OUTPUT);
  pinMode(ledGreenPin, OUTPUT);
  pinMode(ledBluePin, OUTPUT);

  // Ultrasonic sensor setup
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  // Initialize serial communication
  Serial.begin(9600);

  // Connect to WiFi
  connectToWiFi();
  udp.begin(1234);
}

void loop() {
  //Handle State of network and connection status
  netChangeState();
  // Handle state machine and hardware actions based on the current state
  handleCarriageState();

  // Get distance from ultrasonic sensor
  float distance = getDistance();

  // If an object is within 6 cm, stop the motor
  if (distance < 6) {
    Serial.println("Object detected within 6 cm. Stopping motor.");
    stopMotor();
  } else {
    // Otherwise, move the motor forward based on the state
    if (carriageState != STOPPED && carriageState != STOP_AT_STATION) {
      moveMotorForward();
    }
  }

  // Delay for LED flashing effect
  delay(500);
}

void netChangeState() {
  switch (netState) {
    case 'i':
      netSendInit();
      netState = 'w';
      timeSent = millis();
      break;
    case 'w':
      netReceiveAck();
      if (millis() - timeSent > 1000) {
        Serial.println("Failed to get ACKIN");
        netState = 'i';
      }
      break;
    case 'r':
      netReceiveAck();
      break;
  }
}

void netReceiveAck() {
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

    if (expectedSeq < (int)doc["sequence_number"]) {
      expectedSeq = (int)doc["sequence_number"];
    } else if (expectedSeq > (int)doc["sequence_number"]) {
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
      encoder["state"] = sendCarriageState(carriageState);
      serializeJson(encoder, packet);
      Serial.println("recieved STATREQ sent back STAT");

      udp.beginPacket(gateway, 3017);
      udp.write(stringToUni8Arr(packet), packet.length());
      udp.endPacket();
    } else if (msgType[0] == 'E') {
      string temp(doc["cmd"]);
      changeState(temp);
      encoder["message"] = "AKEX";

      serializeJson(encoder, packet);
      Serial.print("recieved EXEC executing: ");

      udp.beginPacket(gateway, 3017);
      udp.write(stringToUni8Arr(packet), packet.length());
      udp.endPacket();
    } else if (msgType[0] == 'A') {
      carriageState = INITIALIZE;
      netState = 'r';
      expectedSeq = (int)doc["sequence_number"];
    }
  }
}

void netSendUpdate() {
  JsonDocument doc;
  JsonObject encoder = doc.add<JsonObject>();
  encoder["client_type"] = "BR";
  encoder["client_id"] = "BR17";
  encoder["sequence_number"] = seqNum;
  seqNum++;
  encoder["message"] = "STAT";
  encoder["state"] = sendCarriageState(carriageState);

  std::string packet = "";
  serializeJson(encoder, packet);

  udp.beginPacket(gateway, 3017);
  udp.write(stringToUni8Arr(packet), packet.length());
  udp.endPacket();
}

void netSendInit() {
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

void changeState(String s) {
  const char* state = s.c_str();
  if(strcmp(state, "SLOW_DOWN") == 0){
    carriageState = SLOW_DOWN;
  }else if(strcmp(state, "STOP_AT_STATION") == 0) {
    carriageState = STOP_AT_STATION;
  }else if(strcmp(state, "FSLOW") == 0) {
    carriageState = STOP_AT_STATION; //TODO Change based on current spd
  }else if(strcmp(state, "FFAST") == 0) {
    carriageState = STOP_AT_STATION; //TODO Change based on current spd
  }else if(strcmp(state, "BACKWARDS_SLOW") == 0) {
    carriageState = BACKWARDS_SLOW;
  }else if(strcmp(state, "E_STOP") == 0) {
    carriageState = E_STOP;
  }
}

// Function to handle carriage states and update hardware accordingly
void handleCarriageState() {
  switch (carriageState) {
    case INITIALIZE:
      Serial.println("State: INITIALIZE");
      carriageState = STOPPED;
      netSendUpdate();
      break;
    case STOPPED:
      Serial.println("State: STOPPED");
      stopMotor();
      setLEDColor(255, 255, 0);  // Yellow
      if (!atStation()) {
        carriageState = ACCELERATING;
        netSendUpdate();
        closeDoors();
      }
      break;
    case BACKWARDS_SLOW:
      Serial.println("State: BACKWARDS_SLOW");
      carriageState = STOPPED;
      netSendUpdate();
      break;
    case ACCELERATING:  //TODO Change to acheive speed sent by CCP then send back an update
      Serial.println("State: ACCELERATING");
      setLEDColor(0, 0, 255);  // Blue
      if (!ccpConnectionLost && !packetDelay) {
        carriageState = CRUISE;
        netSendUpdate();
      } else {
        carriageState = ERROR_HAZARD;
        netSendUpdate();
      }
      break;
    case CRUISE:  //TODO Change to acheive speed sent by CCP then send back an update
      Serial.println("State: CRUISE");
      setLEDColor(0, 0, 255);  // Blue
      if (ccpConnectionLost || packetDelay) {
        carriageState = ERROR_HAZARD;
        netSendUpdate();
      } else {
        carriageState = SLOW_DOWN;
        netSendUpdate();
      }
      break;
    case SLOW_DOWN:  //TODO Change to acheive speed sent by CCP then send back an update
      Serial.println("State: SLOW_DOWN");
      stopMotor();
      setLEDColor(255, 255, 0);  // Yellow
      carriageState = STOPPED;
      netSendUpdate();
      break;
    case STOP_AT_STATION:
      Serial.println("State: STOP_AT_STATION");
      stopMotor();
      openDoors();
      setLEDColor(255, 255, 0);  // Yellow
      carriageState = STOPPED;
      netSendUpdate();
      break;
    case ERROR_HAZARD:
      Serial.println("State: ERROR_HAZARD");
      handleCommunicationErrors();
      if (errorRetryCount >= 3) {
        carriageState = E_STOP;
        netSendUpdate();
      }
      break;
    case E_STOP:
      Serial.println("State: E_STOP");
      stopMotor();
      openDoors();
      setLEDColor(255, 0, 0);  // Red
      break;
  }
  Serial.println(sendCarriageState(carriageState));  // Send current state over Serial
}

// Function to get distance using ultrasonic sensor
float getDistance() {
  long duration;
  float distance;

  // Trigger sensor
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  // Calculate distance based on pulse duration
  duration = pulseIn(echoPin, HIGH);
  distance = duration * 0.034 / 2;  // Convert to cm
  return distance;
}

// Motor control
void moveMotorForward() {
  digitalWrite(motorPin1, HIGH);
  digitalWrite(motorPin2, LOW);
  analogWrite(motorPWM, Testspeed);  // Start motor at specified speed
}

void stopMotor() {
  digitalWrite(motorPin1, LOW);
  digitalWrite(motorPin2, LOW);
  analogWrite(motorPWM, 0);  // Stop motor
}

// LED control
void setLEDColor(int red, int green, int blue) {
  analogWrite(ledRedPin, red);
  analogWrite(ledGreenPin, green);
  analogWrite(ledBluePin, blue);
}

// WiFi connection
void connectToWiFi() {
  if (WiFi.config(staticIP, gateway, subnet) == false) {
    Serial.println("Configuration failed.");
  }

  Serial.println("Connecting to WiFi...");
  WiFi.begin(ssid);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting...");
  }
  Serial.println("Connected to WiFi");
}

// Functions for handling carriage operations
void openDoors() {
  doorsOpen = true;
  Serial.println("Doors opening...");
}

void closeDoors() {
  doorsOpen = false;
  Serial.println("Doors closing...");
}

void handleCommunicationErrors() {
  if (ccpConnectionLost) {
    Serial.println("Error: Lost connection with CCP");
  }
  if (packetDelay) {
    Serial.println("Error: Packet delays detected");
  }
  errorRetryCount++;
}

// Function to send current carriage state via Serial
String sendCarriageState(State state) {
  String stateStr;
  switch (state) {
    case INITIALIZE: stateStr = "INITIALIZE"; break;
    case STOPPED: stateStr = "STOPPED"; break;
    case BACKWARDS_SLOW: stateStr = "BACKWARDS_SLOW"; break;
    case ACCELERATING: stateStr = "ACCELERATING"; break;
    case CRUISE:
      stateStr = "CRUISE";  //TODO Change to be able to send Cruise at what speed slow or fast??
      break;
    case SLOW_DOWN: stateStr = "SLOW_DOWN"; break;
    case STOP_AT_STATION: stateStr = "STOP_AT_STATION"; break;
    case ERROR_HAZARD: stateStr = "ERROR_HAZARD"; break;
    case E_STOP: stateStr = "E_STOP"; break;
  }
  return stateStr;  // Send state over Serial
}

// Simulate atStation condition
bool atStation() {
  return true;  // Simulate that the carriage is at a station
}

const uint8_t* stringToUni8Arr(const std::string& str) {
  const char* cstr = str.c_str();
  size_t len = strlen(cstr);

  uint8_t* temp = new uint8_t[len + 1];
  memcpy(temp, cstr, len + 1);

  return temp;
}
