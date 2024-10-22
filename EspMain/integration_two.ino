#include <WiFi.h>

// Define motor pins
const int motorPin1 = 18; // Motor control pin 1
const int motorPin2 = 19; // Motor control pin 2
const int motorPWM = 21;  // PWM pin for motor speed

// Define LED pins
const int ledRedPin = 16;
const int ledGreenPin = 5;
const int ledBluePin = 17;

// Define ultrasonic sensor pins
const int trigPin = 13;
const int echoPin = 12;

// WiFi credentials
const char* ssid = "ENGG2K3K";

// Motor control variables
int motorSpeed = 0;
int maxSpeed = 255;  // Maximum motor speed
int minSpeed = 0;
int accelerationStep = 5; // Step value for acceleration and deceleration
int distanceThreshold = 6; // Distance threshold in centimeters

enum State {
    INITIALIZE,
    RUNNING,
    STOPPED,
    E_STOP
};

class Carriage {
public:
    State state;
    int motorSpeed;

    Carriage() : state(INITIALIZE) {}

    void updateState(int distance) {
        switch (state) {
            case INITIALIZE:
                printf("State: INITIALIZE\n");
                state = RUNNING;
                accelerateMotor();
                break;

            case STOPPED:
                printf("State: STOPPED\n");
                if (distance > distanceThreshold) {
                    state = RUNNING;
                }
                break;

            case RUNNING:
                printf("State: RUNNING\n");
                if (distance <= distanceThreshold) {
                    state = STOPPED;
                } else {
                    accelerateMotor();
                }
                break;

            case E_STOP:
                printf("State: E_STOP\n");
                stopMotor();
                break;

            default:
                printf("Unknown state.\n");
                break;
        }
    }

    void accelerateMotor() {
        if (motorSpeed < maxSpeed) {
            motorSpeed += accelerationStep;
            analogWrite(motorPWM, motorSpeed);
            digitalWrite(motorPin1, HIGH);
            digitalWrite(motorPin2, LOW);
            printf("Motor accelerating, speed: %d\n", motorSpeed);
        }
    }

    void decelerateMotor() {
        if (motorSpeed > minSpeed) {
            motorSpeed -= accelerationStep;
            analogWrite(motorPWM, motorSpeed);
            printf("Motor decelerating, speed: %d\n", motorSpeed);
        } else {
            stopMotor();
        }
    }

    void stopMotor() {
        motorSpeed = 0;
        analogWrite(motorPWM, motorSpeed);
        digitalWrite(motorPin1, LOW);
        digitalWrite(motorPin2, LOW);
        printf("Motor stopped.\n");
    }

    int measureDistance() {
        // Trigger ultrasonic sensor
        digitalWrite(trigPin, LOW);
        delayMicroseconds(2);
        digitalWrite(trigPin, HIGH);
        delayMicroseconds(10);
        digitalWrite(trigPin, LOW);

        // Measure the time it takes for the echo to return
        long duration = pulseIn(echoPin, HIGH);
        // Calculate distance in centimeters
        int distance = duration * 0.034 / 2;
        return distance;
    }
};

void setup() {
    // Setup motor, ultrasonic sensor, and LED pins
    pinMode(motorPin1, OUTPUT);
    pinMode(motorPin2, OUTPUT);
    pinMode(motorPWM, OUTPUT);
    pinMode(trigPin, OUTPUT);
    pinMode(echoPin, INPUT);
    pinMode(ledRedPin, OUTPUT);
    pinMode(ledGreenPin, OUTPUT);
    pinMode(ledBluePin, OUTPUT);

    // Initialize serial communication
    Serial.begin(9600);

    // Initialize WiFi connection
    WiFi.begin(ssid);
}

void loop() {
    static Carriage carriage;
    int distance = carriage.measureDistance();
    Serial.print("Measured distance: ");
    Serial.println(distance);

    // Update carriage state based on measured distance
    carriage.updateState(distance);

    // If in running state and motor speed is decreasing, decelerate gradually
    if (carriage.state == STOPPED && carriage.motorSpeed > minSpeed) {
        carriage.decelerateMotor();
    }

    delay(100); // Small delay for sensor reading frequency
}

