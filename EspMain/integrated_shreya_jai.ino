#include <WiFi.h>

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

// WiFi credentials
const char* ssid = "ENGG2K3K";

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

State carriageState = INITIALIZE; // Initial state
int errorRetryCount = 0;
bool ccpConnectionLost = false;
bool packetDelay = false;
bool doorsOpen = false;

// Motor control variables
int motorSpeed = 0;
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
void sendCarriageState(State state);

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
}

void loop() {
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

// Function to handle carriage states and update hardware accordingly
void handleCarriageState() {
  switch (carriageState) {
    case INITIALIZE:
      Serial.println("State: INITIALIZE");
      carriageState = STOPPED;
      break;
    case STOPPED:
      Serial.println("State: STOPPED");
      stopMotor();
      setLEDColor(255, 255, 0);  // Yellow
      if (!atStation()) {
        carriageState = ACCELERATING;
        closeDoors();
      }
      break;
    case BACKWARDS_SLOW:
      Serial.println("State: BACKWARDS_SLOW");
      carriageState = STOPPED;
      break;
    case ACCELERATING:
      Serial.println("State: ACCELERATING");
      setLEDColor(0, 0, 255);  // Blue
      if (!ccpConnectionLost && !packetDelay) {
        carriageState = CRUISE;
      } else {
        carriageState = ERROR_HAZARD;
      }
      break;
    case CRUISE:
      Serial.println("State: CRUISE");
      setLEDColor(0, 0, 255);  // Blue
      if (ccpConnectionLost || packetDelay) {
        carriageState = ERROR_HAZARD;
      } else {
        carriageState = SLOW_DOWN;
      }
      break;
    case SLOW_DOWN:
      Serial.println("State: SLOW_DOWN");
      stopMotor();
      setLEDColor(255, 255, 0);  // Yellow
      carriageState = STOPPED;
      break;
    case STOP_AT_STATION:
      Serial.println("State: STOP_AT_STATION");
      stopMotor();
      openDoors();
      setLEDColor(255, 255, 0);  // Yellow
      carriageState = STOPPED;
      break;
    case ERROR_HAZARD:
      Serial.println("State: ERROR_HAZARD");
      handleCommunicationErrors();
      if (errorRetryCount >= 3) {
        carriageState = E_STOP;
      }
      break;
    case E_STOP:
      Serial.println("State: E_STOP");
      stopMotor();
      openDoors();
      setLEDColor(255, 0, 0);  // Red
      break;
  }
  sendCarriageState(carriageState);  // Send current state over Serial
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
void sendCarriageState(State state) {
  String stateStr;
  switch (state) {
    case INITIALIZE: stateStr = "Initialize"; break;
    case STOPPED: stateStr = "Stopped"; break;
    case BACKWARDS_SLOW: stateStr = "Backwards Slow"; break;
    case ACCELERATING: stateStr = "Accelerating"; break;
    case CRUISE: stateStr = "Cruise"; break;
    case SLOW_DOWN: stateStr = "Slow Down"; break;
    case STOP_AT_STATION: stateStr = "Stop At Station"; break;
    case ERROR_HAZARD: stateStr = "Error Hazard"; break;
    case E_STOP: stateStr = "Emergency Stop"; break;
  }
  Serial.println(stateStr);  // Send state over Serial
}

// Simulate atStation condition
bool atStation() {
  return true;  // Simulate that the carriage is at a station
}
