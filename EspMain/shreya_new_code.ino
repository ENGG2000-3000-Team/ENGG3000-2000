#include <WiFi.h>

// Pin Definitions
const int motorPin1 = 18;
const int motorPin2 = 19;
const int motorPWM = 21;
const int ledRedPin = 16;
const int ledGreenPin = 5;
const int ledBluePin = 17;
const char* ssid = "ENGG2K3K";  // WiFi SSID for connection

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
const unsigned long heartbeatInterval = 2000;  // 2 seconds for heartbeat check
bool ackReceived = false;

// Setup function
void setup() {
  // Initialize motor and LED pins
  pinMode(motorPin1, OUTPUT);
  pinMode(motorPin2, OUTPUT);
  pinMode(motorPWM, OUTPUT);
  pinMode(ledRedPin, OUTPUT);
  pinMode(ledGreenPin, OUTPUT);
  pinMode(ledBluePin, OUTPUT);

  // Start serial communication
  Serial.begin(9600);

  // Connect to WiFi
  connectToWiFi();

  // Set initial state and display it
  carriageState = INITIALIZATION;
  displayState();
}

void loop() {
  // Listen for CCP commands
  if (Serial.available() > 0) {
    int command = Serial.parseInt();
    handleCCPCommand(command);
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
      break;
    case ERROR:
      handleError();
      break;
    case DEAD:
      handleDeadState();
      break;
  }

  // Heartbeat mechanism for communication integrity
  if (millis() - lastHeartbeatTime > heartbeatInterval) {
    lastHeartbeatTime = millis();
    sendHeartbeat();  // Simulate heartbeat check
  }
}

// Functions for Different States
void initializeCarriage() {
  Serial.println("[INITIALIZATION]: Starting system setup...");
  setLEDColor(0, 0, 255);  // Blue for initialization
  delay(3000);  // Simulate initialization time
  Serial.println("[INITIALIZATION]: Setup complete, transitioning to STOPC...");
  carriageState = STOPC;  // Move to STOPC after initialization
  displayState();
}

void stopCarriage() {
  Serial.println("[STOPC]: Carriage stopped, doors closed.");
  stopMotor();
  setLEDColor(0, 255, 0);  // Green for STOPC
}

void moveCarriageSlow() {
  Serial.println("[FSLOWC]: Carriage moving forward slowly, doors closed.");
  moveMotorForward(128);  // Slow speed
  setLEDColor(0, 0, 255);  // Blue for slow movement
  delay(2000);  // Simulate movement time
  Serial.println("[FSLOWC]: Reached destination, transitioning to STOPC...");
  carriageState = STOPC;  // Transition to STOPC after moving
  displayState();
}

void moveCarriageFast() {
  Serial.println("[FFASTC]: Carriage moving forward fast, doors closed.");
  moveMotorForward(255);  // Fast speed
  setLEDColor(0, 0, 255);  // Blue for fast movement
  delay(3000);  // Simulate fast movement time
  Serial.println("[FFASTC]: Reached destination, transitioning to STOPC...");
  carriageState = STOPC;  // Transition to STOPC after moving
  displayState();
}

void moveBackwardSlow() {
  Serial.println("[RSLOWC]: Carriage moving backward slowly, doors closed.");
  moveMotorBackward(128);  // Slow backward speed
  setLEDColor(255, 128, 0);  // Orange for backward slow
  delay(2000);  // Simulate backward movement time
  Serial.println("[RSLOWC]: Reached position, transitioning to STOPC...");
  carriageState = STOPC;  // Transition to STOPC after moving
  displayState();
}

void openDoors() {
  Serial.println("[STOPO]: Carriage stopped, doors open.");
  stopMotor();
  setLEDColor(255, 255, 0);  // Yellow for open doors
  delay(3000);  // Simulate door open time
  Serial.println("[STOPO]: Doors closed, transitioning to STOPC...");
  carriageState = STOPC;  // Return to STOPC after doors close
  displayState();
}

void setOffline() {
  Serial.println("[OFLN]: System going offline.");
  stopMotor();
  setLEDColor(128, 128, 128);  // Grey for offline
  delay(1000);  // Simulate offline preparation time
  Serial.println("[OFLN]: System offline.");
}

void emergencyStop() {
  Serial.println("[ESTOP]: Emergency stop activated!");
  stopMotor();
  setLEDColor(255, 0, 0);  // Red for emergency stop
  delay(2000);  // Simulate emergency stop time
  Serial.println("[ESTOP]: Transitioning to INITIALIZATION...");
  carriageState = INITIALIZATION;  // Reset to INITIALIZATION after EStop
  displayState();
}

void handleError() {
  Serial.println("[ERROR]: Error detected! Attempting to recover...");
  stopMotor();
  setLEDColor(255, 0, 255);  // Purple for error
  delay(2000);  // Simulate error handling time
  Serial.println("[ERROR]: Recovered, transitioning to STOPC...");
  carriageState = STOPC;  // Return to STOPC after handling error
  displayState();
}

void handleDeadState() {
  Serial.println("[DEAD]: System in unrecoverable error state.");
  stopMotor();
  setLEDColor(128, 0, 0);  // Dark red for DEAD state
  while (true);  // Halt indefinitely
}

// Handle CCP commands with acknowledgment
void handleCCPCommand(int command) {
  Serial.print("[COMMAND RECEIVED]: ");
  switch (command) {
    case 0: Serial.println("INITIALIZATION"); carriageState = INITIALIZATION; break;
    case 1: Serial.println("STOPC"); carriageState = STOPC; break;
    case 2: Serial.println("FSLOWC"); carriageState = FSLOWC; break;
    case 3: Serial.println("FFASTC"); carriageState = FFASTC; break;
    case 4: Serial.println("RSLOWC"); carriageState = RSLOWC; break;
    case 5: Serial.println("STOPO"); carriageState = STOPO; break;
    case 6: Serial.println("OFLN"); carriageState = OFLN; break;
    case 7: Serial.println("ESTOP"); carriageState = ESTOP; break;
    case 8: Serial.println("ERROR"); carriageState = ERROR; break;
    case 9: Serial.println("DEAD"); carriageState = DEAD; break;
    default: Serial.println("Unknown Command"); return;
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
}

// WiFi Connection Function
void connectToWiFi() {
  Serial.println("[WiFi]: Connecting...");
  WiFi.begin(ssid);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("[WiFi]: Still connecting...");
  }
  Serial.println("[WiFi]: Connected.");
}

// Send heartbeat message
void sendHeartbeat() {
  Serial.println("[HEARTBEAT]: Checking connection...");
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
}

// LED Control Function
void setLEDColor(int red, int green, int blue) {
  analogWrite(ledRedPin, red);
  analogWrite(ledGreenPin, green);
  analogWrite(ledBluePin, blue);
}
