#include <mutex>

TaskHandle_t core0;
TaskHandle_t core1;
std::mutex locker;

void setup() {
  Serial.begin(9600);
  xTaskCreatePinnedToCore(Core0code,"Core0",10000, NULL, 0, &core0, 0);
  xTaskCreatePinnedToCore(Core1code,"Core1", 10000, NULL, 1, &core1, 1);
  
}

//DO NOT USE VOID LOOP
void loop() {}

//TODO
/*Put impement net code here*/
void Core0code(void * parameter) { //Structure according to UML
  for(;;) {
    
    //Waits until the other task is finsished with i Handle race conditions
    locker.lock();
    //TODO
    /*Put code that invloves data here*/
    locker.unlock();

  }
};

//TODO
/*Put impement hardware code here*/
void Core1code(void * parameter) {//Structure according to UML
  for(;;) {
    
    //Handle race conditions
    locker.lock();
    //TODO
    /*Put code that invloves data here*/
    locker.unlock();

  }
};