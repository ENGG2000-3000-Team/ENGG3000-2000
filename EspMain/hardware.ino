#include <WiFi.h>
#include <WiFiUdp.h>

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

// Define IR sensor pin
//const int irSensorPin = D4;

// WiFi credentials
const char* ssid = "ENGG2K3K";
IPAddress staticIP(10,20,30,117);
IPAddress gateway(10,20,30,1);   // Replace this with your gateway IP Addess
IPAddress subnet(255, 255, 255, 0);
WiFiUDP udp;
char packetBuffer[255];
unsigned int localPort = 9999;

// Motor control variables
int motorSpeed = 0;

// LED control variables
int ledRed = 0;
int ledGreen = 0;
int ledBlue = 0;
//https://randomnerdtutorials.com/esp32-useful-wi-fi-functions-arduino/
//https://www.baeldung.com/udp-in-java
//https://www.aranacorp.com/en/communication-between-two-esp32s-via-udp/
//Carriage State
String carriageState = "In Transit";

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
  //pinMode(irSensorPin, INPUT);

  // Initialize serial communication
  Serial.begin(9600);

  // Connect to WiFi
  connectToWiFi();
  udp.begin(localPort);
}

void loop() {
  // Here you would receive and parse JSON data via TCP/UDP from the CCP
  // For demonstration, we'll use hardcoded states
  netCode();

  // Read the IR sensor value
  //int irSensorValue = digitalRead(irSensorPin);
    
  // Control motor and LEDs based on the state and IR sensor value
  if (carriageState == "Idle") {
    stopMotor();
    setLEDColor(0, 255, 0);  // Green
  } else if (carriageState == "In Transit") {
    moveMotorForward();
    setLEDColor(0, 0, 255);  // Flashing Blue
  } else if (carriageState == "Stop Arrival") {
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

void netCode() {
  int packetSize = udp.parsePacket();
  if (packetSize) {
    // receive incoming UDP packets
    Serial.printf("Received %d bytes from %s, port %d\n", packetSize, udp.remoteIP().toString().c_str(), udp.remotePort());
    int len = udp.read(packetBuffer, 255);
    if (len > 0) {
      packetBuffer[len - 1] = 0;
    }
    Serial.printf("UDP packet contents: %s\n", packetBuffer);

    //if Packet is status request send back status else if exec cmd save new status
    if (packetBuffer[0] == 'S') {  //Arbitrary Val checks for stat request
      udp.beginPacket(gateway, 3017);
      udp.print(carriageState);
      udp.endPacket();
    } else if (packetBuffer[0] == 'E') {  //TODO Saves new status if execute cmd
      //TODO Message Handeling needs to be done here, with speed etc
      carriageState = "";
      for (int i = 1; i < 21; i++) {
        carriageState = carriageState + packetBuffer[i];
      }
    }
  }
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

// Control motor functions
void moveMotorForward() {
  digitalWrite(motorPin1, HIGH);
  digitalWrite(motorPin2, LOW);
  //analogWrite(motorPWM, 100);  // Full speed
  // for(int i=100; i<=255; i=i+5){
  //   analogWrite(motorPWM, i);  // Full speed
  //   delay(500);
  //   Serial.println(i);
  // }

while (true){
  while (Serial.available() > 0)
    {
      int speed;
      speed = Serial.parseInt();  // parseInt() reads in the first integer value from the Serial Monitor.
      speed = constrain(speed, 0, 255); // constrains the speed between 0 and 255 because analogWrite() only works in this range.

      Serial.print("Setting speed to ");  // feedback and prints out the speed that you entered.
      Serial.println(speed);

      analogWrite(motorPWM, speed);  // sets the speed of the motor.
    }
    }
}

void stopMotor() {
  digitalWrite(motorPin1, LOW);
  digitalWrite(motorPin2, LOW);
  //analogWrite(motorPWM, 0);  // Stop motor
  for(int i=255; i>=0; i=i-5){
    analogWrite(motorPWM, i);  // Full speed
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