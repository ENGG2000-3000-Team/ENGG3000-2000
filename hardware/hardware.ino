#include <ESP8266WiFi.h>

// Define motor pins
const int motorPin1 = D1; // Motor control pin 1
const int motorPin2 = D2; // Motor control pin 2
const int motorPWM = D3;  // PWM pin for motor speed

// Define LED pins
const int ledRedPin = D5;
const int ledGreenPin = D6;
const int ledBluePin = D7;

// Define IR sensor pin
const int irSensorPin = D4;

// WiFi credentials
const char* ssid = "ENGG2K3K";
const char* password = "your_wifi_password";  //no pwd so remove this

// Motor control variables
int motorSpeed = 0;

// LED control variables
int ledRed = 0;
int ledGreen = 0;
int ledBlue = 0;

void setup() {
  // Set motor pins as output
  pinMode(motorPin1, OUTPUT);
  pinMode(motorPin2, OUTPUT);
  pinMode(motorPWM, OUTPUT);

  // Set LED pins as output
  pinMode(ledRedPin, OUTPUT);
  pinMode(ledGreenPin, OUTPUT);
  pinMode(ledBluePin, OUTPUT);

  // Set IR sensor pin as input
  pinMode(irSensorPin, INPUT);

  // Initialize serial communication
  Serial.begin(9600);

  // Connect to WiFi
  connectToWiFi();
}

void loop() {
  // Here you would receive and parse JSON data via TCP/UDP from the CCP
  // For demonstration, we'll use hardcoded states

  String carriageState = "In Transit";  // Replace with actual received state

  // Read the IR sensor value
  int irSensorValue = digitalRead(irSensorPin);

  // Control motor and LEDs based on the state and IR sensor value
  if (carriageState == "Idle") {
    stopMotor();
    setLEDColor(0, 255, 0);  // Green
  } else if (carriageState == "In Transit") {
    moveMotorForward();
    setLEDColor(0, 0, 255);  // Flashing Blue
  } else if (carriageState == "Stop Arrival" || irSensorValue == LOW) {
    // Assume IR sensor detects the stop when LOW
    stopMotor();
    setLEDColor(255, 255, 0);  // Yellow
  } else if (carriageState == "Boarding/Alighting") {
    stopMotor();
    setLEDColor(255, 255, 0);  // Flashing Yellow
  } else if (carriageState == "Occupied") {
    stopMotor();
    setLEDColor(0, 0, 255);  // Blue
  } else if (carriageState == "Collision Avoidance") {
    stopMotor();
    setLEDColor(255, 0, 0);  // Flashing Red
  } else if (carriageState == "Emergency") {
    stopMotor();
    setLEDColor(255, 0, 0);  // Red
  } else if (carriageState == "Maintenance Mode") {
    stopMotor();
    setLEDColor(255, 165, 0);  // Flashing Orange
  } else if (carriageState == "Connection Lost") {
    stopMotor();
    setLEDColor(128, 0, 128);  // Purple
  }

  // Add delay for LED flashing effect
  delay(500);
}

// Connect to WiFi
void connectToWiFi() {
  Serial.println("Connecting to WiFi...");
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting...");
  }

  Serial.println("Connected to WiFi");
}

// Control motor functions
void moveMotorForward() {
  digitalWrite(motorPin1, HIGH);
  digitalWrite(motorPin2, LOW);
  analogWrite(motorPWM, 255);  // Full speed
}

void stopMotor() {
  digitalWrite(motorPin1, LOW);
  digitalWrite(motorPin2, LOW);
  analogWrite(motorPWM, 0);  // Stop motor
}

// Control LED functions
void setLEDColor(int red, int green, int blue) {
  analogWrite(ledRedPin, red);
  analogWrite(ledGreenPin, green);
  analogWrite(ledBluePin, blue);
}
