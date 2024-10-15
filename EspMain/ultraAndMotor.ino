#include <WiFi.h>

// Define motor pins
const int motorPin1 = 18; // Motor control pin 1
const int motorPin2 = 19; // Motor control pin 2
const int motorPWM = 21;  // PWM pin for motor speed

// Define LED pins
const int ledRedPin = 16;
const int ledGreenPin = 5;
const int ledBluePin = 17;

const int ledRedPin2 = 15;
const int ledGreenPin2 = 4;
const int ledBluePin2 = 2;

// Define ultrasonic sensor pins
const int trigPin = 13;
const int echoPin = 12;

// WiFi credentials
const char* ssid = "ENGG2K3K";

// Motor control variables
int motorSpeed = 0;
int Testspeed = 100;

// LED control variables
int ledRed = 0;
int ledGreen = 0;
int ledBlue = 0;

String carriageState = "";  // To store the user-provided carriage state

void setup() {
  // Set motor pins as output
  pinMode(motorPin1, OUTPUT);
  pinMode(motorPin2, OUTPUT);
  pinMode(motorPWM, OUTPUT);

  // Set LED pins as output
  pinMode(ledRedPin, OUTPUT);
  pinMode(ledGreenPin, OUTPUT);
  pinMode(ledBluePin, OUTPUT);

  // Set ultrasonic sensor pins as output/input
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  // Initialize serial communication
  Serial.begin(9600);

  // Connect to WiFi
  connectToWiFi();
}

void loop() {
  float distance = getDistance();
  
  // Stop motor only if distance is less than 6 cm
  if (distance < 6) {
    Serial.println("Object detected within 6 cm. Stopping motor.");
    stopMotor();
  } else {
    moveMotorForward();  // Keep motor running when no obstacle is detected
  }
  
  // Check if there's input from the Serial Monitor
  if (Serial.available() > 0) {
    carriageState = Serial.readStringUntil('\n');  // Read carriage state from serial input
    carriageState.trim();  // Remove any leading/trailing spaces or newlines
    
    Serial.print("Carriage State Received: ");
    Serial.println(carriageState);

    // Read distance from the ultrasonic sensor
    Serial.print("Distance: ");
    Serial.print(distance);
    Serial.println(" cm");

    // Control motor and LEDs based on the received state
    if (carriageState == "Idle") {
      moveMotorForward();
      setLEDColor(0, 255, 0);  // Green
    } else if (carriageState == "In Transit") {
      moveMotorForward();
      setLEDColor(0, 0, 255);  // Flashing Blue
    } else if (carriageState == "Stop Arrival") {
      stopMotor();
      setLEDColor(255, 255, 0);  // Yellow
    } else if (carriageState == "Boarding/Alighting") {
      stopMotor();
      setLEDColor(255, 255, 0);  // Flashing Yellow
    } else if (carriageState == "Occupied") {
      moveMotorForward();
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
    } else {
      Serial.println("Invalid Carriage State");
    }
  }
  
  // Add delay for LED flashing effect
  delay(500);
}

// Function to get the distance using the ultrasonic sensor
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
  
  // Convert the time into distance (Speed of sound is 343 meters per second)
  distance = duration * 0.034 / 2;  // Divide by 2 because the sound travels to the object and back

  return distance;  // Distance in cm
}

// Connect to WiFi
void connectToWiFi() {
  Serial.println("Connecting to WiFi...");
  WiFi.begin(ssid);

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
  analogWrite(motorPWM, Testspeed);  // Full speed
  for(int i = 60; i <= 255; i += 5) {
     analogWrite(motorPWM, i);  // Gradually increase speed
     delay(500);
     Serial.println(i);
   }
}

void stopMotor() {
  digitalWrite(motorPin1, LOW);
  digitalWrite(motorPin2, LOW);
  analogWrite(motorPWM, 0);  // Stop motor
  for(int i = 255; i >= 0; i -= 5) {
    analogWrite(motorPWM, i);  // Gradually decrease speed
    delay(500);
    Serial.println(i);
  }
}

// Control LED functions
void setLEDColor(int red, int green, int blue) {
  analogWrite(ledRedPin, red);
  analogWrite(ledGreenPin, green);
  analogWrite(ledBluePin, blue);
}
