#include <WiFi.h>
#include <Udp.h>

const char *ssid = "ENGG2K3K";

const int localPort = 3017;
IPAddress local_IP(10, 20, 30, 117);
IPAddress remote_IP(10, 20, 30, 1);
WiFiUDP udp;

TaskHandle_t core0, core1;
SemaphoreHandle_t baton;

String carriageState;

// Motor control variables
int motorSpeed = 0;

// LED control variables
int ledRed = 0;
int ledGreen = 0;
int ledBlue = 0;

// Define motor pins
const int motorPin1 = 18;  // Motor control pin 1
const int motorPin2 = 19;  // Motor control pin 2
const int motorPWM = 21;   // PWM pin for motor speed

// Define LED pins
const int ledRedPin = 16;
const int ledGreenPin = 5;
const int ledBluePin = 17;

const int ledRedPin2 = 15;
const int ledGreenPin2 = 4;
const int ledBluePin2 = 2;

// the setup function runs once when you press reset or power the board
void setup() {
  Serial.begin(115200);

  // Set motor pins as output
  pinMode(motorPin1, OUTPUT);
  pinMode(motorPin2, OUTPUT);
  pinMode(motorPWM, OUTPUT);

  // Set LED pins as output
  pinMode(ledRedPin, OUTPUT);
  pinMode(ledGreenPin, OUTPUT);
  pinMode(ledBluePin, OUTPUT);

  connectToWiFi();

  udp.begin(localPort);

  baton = xSemaphoreCreateMutex();

  xTaskCreatePinnedToCore(codeForCore0, "Core0Task", 1000, NULL, 1, &core0, 0);
  delay(500);  // needed to start-up task1
  xTaskCreatePinnedToCore(codeForCore1, "Core1Task", 1000, NULL, 1, &core1, 1);
}

//Implement Hardware Code Here
void codeForCore0(void *parameter) {
  for (;;) {
    // Here you would receive and parse JSON data via TCP/UDP from the CCP
    // For demonstration, we'll use hardcoded states
    //String carriageState = "Idle";

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
    xSemaphoreTake(baton, portMAX_DELAY);
    //TODO
    //Put Code that touches shared data here
    xSemaphoreGive(baton);
    delay(500);
  }
}

//TODO
//Implement Net Code Here
void codeForCore1(void *parameter) {
  for (;;) {
    int packetSize = udp.parsePacket();
    if (packetSize) {
      char incomingData[packetSize];
      udp.read(incomingData, packetSize);

      Serial.print("Data: ");
      Serial.println(incomingData);
    }

    xSemaphoreTake(baton, portMAX_DELAY);
    //TODO
    //Put Code that touches shared data here
    if (packetSize) {
      comprehend(incomingData);
    }
    xSemaphoreGive(baton);
  }
}

//TODO
void comprehend(char incomingData[]) {
  //carriageState = incomingData[0];
  for(int i=0; i<incomingData.length; i++) {
    Serial.println(incomingData[i]);
  }
}

void moveMotorForward() {
  digitalWrite(motorPin1, HIGH);
  digitalWrite(motorPin2, LOW);
  //analogWrite(motorPWM, 100);  // Full speed
  // for(int i=100; i<=255; i=i+5){
  //   analogWrite(motorPWM, i);  // Full speed
  //   delay(500);
  //   Serial.println(i);
  // }

  while (true) {
    while (Serial.available() > 0) {
      int speed;
      speed = Serial.parseInt();         // parseInt() reads in the first integer value from the Serial Monitor.
      speed = constrain(speed, 0, 255);  // constrains the speed between 0 and 255 because analogWrite() only works in this range.

      Serial.print("Setting speed to ");  // feedback and prints out the speed that you entered.
      Serial.println(speed);

      analogWrite(motorPWM, speed);  // sets the speed of the motor.
    }
  }
}

void connectToWiFi() {
  Serial.println("Connecting to WiFi...");
  WiFi.begin(ssid);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting...");
    Serial.println(WiFi.status());
  }

  Serial.println("Connected to WiFi");
}

void stopMotor() {
  digitalWrite(motorPin1, LOW);
  digitalWrite(motorPin2, LOW);
  //analogWrite(motorPWM, 0);  // Stop motor
  for (int i = 255; i >= 0; i = i - 5) {
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

void loop() {
  delay(10);
}