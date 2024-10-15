#include <cstdio>
#include <string>

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

class Carriage {
public:
    State state;
    bool doorsOpen;
    bool ccpConnectionLost;
    bool packetDelay;
    int errorRetryCount;

    Carriage() : state(INITIALIZE), doorsOpen(false), ccpConnectionLost(false), packetDelay(false), errorRetryCount(0) {}

    void updateState() {
        switch(state) {
            case INITIALIZE:
                printf("State: INITIALIZE\n");
                state = STOPPED; // After initializing, move to STOPPED
                break;

            case STOPPED:
                printf("State: STOPPED\n");
                if (atStation()) {
                    stopAtStation();
                } else {
                    closeDoors();
                    // Example of transitioning to ACCELERATING
                    state = ACCELERATING;
                }
                break;

            case BACKWARDS_SLOW:
                printf("State: BACKWARDS_SLOW\n");
                state = STOPPED; // Reverse completed, go back to STOPPED
                break;

            case ACCELERATING:
                printf("State: ACCELERATING\n");
                if (ccpConnectionLost || packetDelay) {
                    state = ERROR_HAZARD; // Move to error state if communication is lost
                } else {
                    closeDoors(); // Ensure doors are closed before moving
                    state = CRUISE; // Once acceleration completes, go to cruise
                }
                break;

            case CRUISE:
                printf("State: CRUISE\n");
                if (ccpConnectionLost || packetDelay) {
                    state = ERROR_HAZARD; // Move to error state if communication is lost
                } else {
                    // Simulate slowing down to stop
                    state = SLOW_DOWN;
                }
                break;

            case SLOW_DOWN:
                printf("State: SLOW_DOWN\n");
                state = STOPPED; // After slowing down, move to STOPPED
                break;

            case STOP_AT_STATION:
                printf("State: STOP_AT_STATION\n");
                // Simulate door operations at the station
                openDoors();
                // Wait for CCP signal to close doors and continue
                state = STOPPED;
                break;

            case ERROR_HAZARD:
                printf("State: ERROR_HAZARD\n");
                handleCommunicationErrors();
                if (ccpConnectionLost || packetDelay) {
                    errorRetryCount++;
                    if (errorRetryCount >= 3) {
                        state = E_STOP; // Critical error, move to emergency stop
                    } else {
                        // Attempt to recover, stay in Error/Hazard state
                        printf("Retrying connection, attempt %d\n", errorRetryCount);
                    }
                } else {
                    errorRetryCount = 0; // Reset retries if resolved
                    state = SLOW_DOWN; // Mild error, slow down and recover
                }
                break;

            case E_STOP:
                printf("State: E_STOP\n");
                openDoors(); // Doors open for safety in emergency stop
                // Stay in E_STOP until manually reset
                break;

            default:
                printf("Unknown state.\n");
                break;
        }
    }

    // Add serial communication to send the carriage state to ultraAndMotor.ino
    void sendCarriageState(State state) {
      String stateStr;
      
      switch (state) {
        case INITIALIZE:
          stateStr = "Initialize";
          break;
        case STOPPED:
          stateStr = "Stopped";
          break;
        case BACKWARDS_SLOW:
          stateStr = "Backwards Slow";
          break;
        case ACCELERATING:
          stateStr = "Accelerating";
          break;
        case CRUISE:
          stateStr = "Cruise";
          break;
        case SLOW_DOWN:
          stateStr = "Slow Down";
          break;
        case STOP_AT_STATION:
          stateStr = "Stop At Station";
          break;
        case ERROR_HAZARD:
          stateStr = "Error Hazard";
          break;
        case E_STOP:
          stateStr = "Emergency Stop";
          break;
      }
      
      // Send the state string over Serial
      Serial.println(stateStr);
    }

    void updateState() {
      // Same state machine logic as before
      // Call sendCarriageState at the end of each case to send the current state to ultraAndMotor.ino
      sendCarriageState(state);
      
      // Rest of the code
    }


    void stopAtStation() {
        state = STOP_AT_STATION;
        printf("Stopping at station. Doors open.\n");
        doorsOpen = true;
    }

    void openDoors() {
        doorsOpen = true;
        printf("Doors opening...\n");
    }

    void closeDoors() {
        doorsOpen = false;
        printf("Doors closing...\n");
    }

    void handleCommunicationErrors() {
        if (ccpConnectionLost) {
            printf("Error: Lost connection with CCP\n");
        }
        if (packetDelay) {
            printf("Error: Packet delays detected\n");
        }
    }

    bool atStation() {
        // Simulate condition for being at a station
        return true; // Example: returning true to simulate a station stop
    }
};

int main() {
    Carriage carriage;

    // Simulate the state transitions
    while (carriage.state != E_STOP) {
        carriage.updateState();
        
        // Simulate conditions like packet delay or connection lost
        if (carriage.state == CRUISE) {
            carriage.packetDelay = true;  // Trigger an error
        }
    }

    return 0;
}