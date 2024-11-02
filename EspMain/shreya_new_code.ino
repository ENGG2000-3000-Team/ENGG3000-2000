#include <WiFi.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>
#include <iostream>
#include <string.h>
using namespace std;

// Pin Definitions
const int motorPin1 = 18;
const int motorPin2 = 19;
const int motorPWM = 21;
const int ledRedPin = 16;
const int ledGreenPin = 5;
const int ledBluePin = 17;
// const int IRpin = 26;

// Define ultrasonic sensor pins
const int trigPin = 13;
const int echoPin = 12;

// WiFi credentials/Net
const char* ssid = "ENGG2K3K";
IPAddress staticIP(10, 20, 30, 117);
IPAddress gateway(10, 20, 30, 149);
IPAddress subnet(255, 255, 255, 0);
WiFiUDP udp;
char packetBuffer[1000];
unsigned int remotePort = 3017;
char netState = 'i';
unsigned long timeSent;
unsigned long seqNum = 15000;
int expectedSeq;

//ir
// int temp;

long duration;
float distance;

// States Enumeration
enum CarriageState {
  INITIALIZATION,
  STOPC,
  FSLOWC,
  FFASTC,
  RSLOWC,
  STOPO,
  OFLN,
  ESTOP,
  ERROR,
  DEAD
};

// Variables for state management
CarriageState carriageState = INITIALIZATION;
unsigned long lastHeartbeatTime = 0;
const unsigned long heartbeatInterval = 3000;  // 2 seconds for heartbeat check
bool ackReceived = false;
bool Estop = false;

// Setup function
void setup() {
  // Initialize motor and LED pins
  pinMode(motorPin1, OUTPUT);
  pinMode(motorPin2, OUTPUT);
  pinMode(motorPWM, OUTPUT);
  pinMode(ledRedPin, OUTPUT);
  pinMode(ledGreenPin, OUTPUT);
  pinMode(ledBluePin, OUTPUT);
  // pinMode(IRpin, INPUT_PULLUP);

  //IR
  // attachInterrupt(IRpin,stopCarriage,FALLING);



  // Start serial communication
  Serial.begin(9600);

  // Connect to WiFi
  connectToWiFi();
  udp.begin(1234);

  // Set initial state and display it
  carriageState = INITIALIZATION;
  displayState();
}

void loop() {
  // Listen for CCP commands
  netChangeState();

  // temp = digitalRead(26);
  // Serial.println(temp);
  // float distance = getDistance();
  // Send a 10us pulse to trigger the sensor
  // digitalWrite(trigPin, LOW);
  // delayMicroseconds(2);
  // digitalWrite(trigPin, HIGH);
  // delayMicroseconds(10);
  // digitalWrite(trigPin, LOW);

  // Read the echoPin and calculate the distance based on the duration
  // duration = pulseIn(echoPin, HIGH);

  // Convert the time into distance (Speed of sound is 343 meters per second).
  // Divide by 2 because the sound travels to the object and back.
  // distance = duration * 0.034 / 2;

  // if (distance <= 6) carriageState = STOPC;
  // else if (distance <= 20) carriageState = FSLOWC;

  if (Estop) {
    carriageState = ESTOP;
    Estop = false;
  }
  // Execute state logic
  switch (carriageState) {
    case INITIALIZATION:
      initializeCarriage();
      break;
    case STOPC:
      stopCarriage();
      break;
    case FSLOWC:
      moveCarriageSlow();
      break;
    case FFASTC:
      moveCarriageFast();
      break;
    case RSLOWC:
      moveBackwardSlow();
      break;
    case STOPO:
      openDoors();
      break;
    case OFLN:
      setOffline();
      break;
    case ESTOP:
      emergencyStop();
      Estop = false;
      break;
    case ERROR:
      handleError();
      break;
    case DEAD:
      handleDeadState();
      break;
  }
}

float getDistance() {
  long duration;
  float distance;

  // Send a 10us pulse to trigger the sensor
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  // Read the echoPin and calculate the distance based on the duration
  duration = pulseIn(echoPin, HIGH);

  // Convert the time into distance (Speed of sound is 343 meters per second).
  // Divide by 2 because the sound travels to the object and back.
  distance = duration * 0.034 / 2;

  return distance;  // Distance in cm
}

// Functions for Different States
void initializeCarriage() {
  Serial.println("[INITIALIZATION]: Starting system setup...");
  setLEDColor(0, 255, 0);  // Green for initialization
  delay(3000);             // Simulate initialization time
  Serial.println("[INITIALIZATION]: Setup complete, transitioning to STOPC...");
  carriageState = STOPC;  // Move to STOPC after initialization
  displayState();
}

void stopCarriage() {
  Serial.println("[STOPC]: Carriage stopped, doors closed.");
  stopMotor();
  setLEDColor(255, 0, 0);  // Red for STOPC
}

void moveCarriageSlow() {
  Serial.println("[FSLOWC]: Carriage moving forward slowly, doors closed.");
  moveMotorForward(128);     // Slow speed
  setLEDColor(255, 128, 0);  // Orange for slow movement
  // delay(2000);             // Simulate movement time
  // Serial.println("[FSLOWC]: Reached destination, transitioning to STOPC...");
  // carriageState = STOPC;  // Transition to STOPC after moving
  displayState();
}

void moveCarriageFast() {
  Serial.println("[FFASTC]: Carriage moving forward fast, doors closed.");
  moveMotorForward(255);   // Fast speed
  setLEDColor(0, 255, 0);  // Green for fast movement
  // delay(3000);             // Simulate fast movement time
  // Serial.println("[FFASTC]: Reached destination, transitioning to STOPC...");
  // carriageState = STOPC;  // Transition to STOPC after moving
  displayState();
}

void moveBackwardSlow() {
  Serial.println("[RSLOWC]: Carriage moving backward slowly, doors closed.");
  moveMotorBackward(128);    // Slow backward speed
  setLEDColor(255, 128, 0);  // Orange for backward slow
  // delay(2000);               // Simulate backward movement time
  // Serial.println("[RSLOWC]: Reached position, transitioning to STOPC...");
  // carriageState = STOPC;  // Transition to STOPC after moving
  displayState();
}

void openDoors() {
  Serial.println("[STOPO]: Carriage stopped, doors open.");
  stopMotor();
  setLEDColor(255, 255, 0);  // Yellow for open doors
  delay(3000);               // Simulate door open time
  Serial.println("[STOPO]: Doors closed, transitioning to STOPC...");
  carriageState = STOPC;  // Return to STOPC after doors close
  displayState();
}

void setOffline() {
  Serial.println("[OFLN]: System going offline.");
  stopMotor();
  setLEDColor(128, 128, 128);  // Grey for offline
  delay(1000);                 // Simulate offline preparation time
  Serial.println("[OFLN]: System offline.");
}

void emergencyStop() {
  Serial.println("[ESTOP]: Emergency stop activated!");
  stopMotor();
  setLEDColor(255, 0, 0);  // Red for emergency stop
  delay(2000);             // Simulate emergency stop time
  Serial.println("[ESTOP]: Transitioning to INITIALIZATION...");
  carriageState = INITIALIZATION;  // Reset to INITIALIZATION after EStop
  displayState();
}

void handleError() {
  Serial.println("[ERROR]: Error detected! Attempting to recover...");
  stopMotor();
  setLEDColor(255, 0, 255);  // Purple for error
  delay(2000);               // Simulate error handling time
  Serial.println("[ERROR]: Recovered, transitioning to STOPC...");
  carriageState = STOPC;  // Return to STOPC after handling error
  displayState();
}

void handleDeadState() {
  Serial.println("[DEAD]: System in unrecoverable error state.");
  stopMotor();
  setLEDColor(128, 0, 0);  // Dark red for DEAD state
  while (true)
    ;  // Halt indefinitely
}

// Handle CCP commands with acknowledgment
void stringToEnum(std::string s) {
  Serial.print("[COMMAND RECEIVED]: ");
  const char* state = s.c_str();
  if (strcmp(state, "STOPC") == 0) {
    carriageState = STOPC;
  } else if (strcmp(state, "STOPO") == 0) {
    carriageState = STOPO;
  } else if (strcmp(state, "FSLOWC") == 0) {
    carriageState = FSLOWC;
  } else if (strcmp(state, "FFASTC") == 0) {
    carriageState = FFASTC;
  } else if (strcmp(state, "RSLOWC") == 0) {
    carriageState = RSLOWC;
  } else if (strcmp(state, "ERR") == 0) {
    carriageState = ERROR;
  } else if (strcmp(state, "OFLN") == 0) {
    carriageState = OFLN;
  }
  displayState();  // Display the new state
}

// Display current state
void displayState() {
  Serial.print("[CURRENT STATE]: ");
  switch (carriageState) {
    case INITIALIZATION: Serial.println("INITIALIZATION"); break;
    case STOPC: Serial.println("STOPC (Stopped, Doors Closed)"); break;
    case FSLOWC: Serial.println("FSLOWC (Forward Slow, Doors Closed)"); break;
    case FFASTC: Serial.println("FFASTC (Forward Fast, Doors Closed)"); break;
    case RSLOWC: Serial.println("RSLOWC (Reverse Slow, Doors Closed)"); break;
    case STOPO: Serial.println("STOPO (Stopped, Doors Open)"); break;
    case OFLN: Serial.println("OFLN (Offline)"); break;
    case ESTOP: Serial.println("ESTOP (Emergency Stop)"); break;
    case ERROR: Serial.println("ERROR"); break;
    case DEAD: Serial.println("DEAD"); break;
    default: Serial.println("Unknown State"); break;
  }
  netSendUpdate();
}

// WiFi Connection Function
void connectToWiFi() {
  if (WiFi.config(staticIP, gateway, subnet) == false) {
    Serial.println("Configuration failed.");
  }

  Serial.println("[WiFi]: Connecting...");
  WiFi.begin(ssid);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("[WiFi]: Still connecting...");
  }
  Serial.println("[WiFi]: Connected.");
}

// Motor Control Functions
void moveMotorForward(int speed) {
  digitalWrite(motorPin1, HIGH);
  digitalWrite(motorPin2, LOW);
  analogWrite(motorPWM, speed);
}

void moveMotorBackward(int speed) {
  digitalWrite(motorPin1, LOW);
  digitalWrite(motorPin2, HIGH);
  analogWrite(motorPWM, speed);
}

void stopMotor() {
  analogWrite(motorPWM, 0);
  digitalWrite(motorPin1, LOW);
  digitalWrite(motorPin2, LOW);
  carriageState = STOPC;
}

// LED Control Function
void setLEDColor(int red, int green, int blue) {
  analogWrite(ledRedPin, red);
  analogWrite(ledGreenPin, green);
  analogWrite(ledBluePin, blue);
}

void netChangeState() {
  switch (netState) {
    case 'i':
      netSendInit();
      netState = 'w';
      timeSent = millis();
      break;
    case 'w':
      if (millis() - timeSent > 1000) {
        Serial.println("Failed to get ACKIN");
        netState = 'i';
      }
      netReceiveNAck();
      break;
    case 'r':
      netReceiveNAck();
      // Heartbeat mechanism for communication integrity
      if (millis() - lastHeartbeatTime > 10000) {
        Estop = true;
        netState = 'i';
        lastHeartbeatTime = millis();
      }
      break;
  }
}

void netReceiveNAck() {
  int packetSize = udp.parsePacket();
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
      encoder["state"] = stateToString(carriageState);
      serializeJson(encoder, packet);
      Serial.println("recieved STATREQ sent back STAT");
      lastHeartbeatTime = millis();

      udp.beginPacket(gateway, remotePort);
      udp.write(stringToUni8Arr(packet), packet.length());
      udp.endPacket();
    } else if (msgType[0] == 'E') {
      string temp(doc["cmd"]);
      stringToEnum(temp);
      encoder["message"] = "AKEX";

      serializeJson(encoder, packet);
      Serial.printf("recieved EXEC executing: ");

      udp.beginPacket(gateway, remotePort);
      udp.write(stringToUni8Arr(packet), packet.length());
      udp.endPacket();
    } else if (msgType[0] == 'A') {
      Serial.println("AKIN");
      carriageState = INITIALIZATION;
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
  encoder["state"] = stateToString(carriageState);

  std::string packet = "";
  serializeJson(encoder, packet);

  udp.beginPacket(gateway, remotePort);
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

  udp.beginPacket(gateway, remotePort);
  udp.write(stringToUni8Arr(packet), packet.length());
  udp.endPacket();
}

String stateToString(CarriageState state) {
  String stateStr;
  switch (state) {
    case INITIALIZATION: stateStr = "INITIALIZATION"; break;
    case STOPC: stateStr = "STOPC"; break;
    case FSLOWC: stateStr = "FSLOWC"; break;
    case FFASTC: stateStr = "FFASTC"; break;
    case RSLOWC: stateStr = "RSLOWC"; break;
    case STOPO: stateStr = "STOPO"; break;
    case OFLN: stateStr = "OFLN"; break;
    case ESTOP: stateStr = "ESTOP"; break;
    case ERROR: stateStr = "ERROR"; break;
    case DEAD: stateStr = "DEAD"; break;
  }
  return stateStr;  // Send state over Serial
}

const uint8_t* stringToUni8Arr(const std::string& str) {
  const char* cstr = str.c_str();
  size_t len = strlen(cstr);

  uint8_t* temp = new uint8_t[len + 1];
  memcpy(temp, cstr, len + 1);

  return temp;
}
