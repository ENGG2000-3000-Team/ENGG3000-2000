TaskHandle_t core0, core1;
SemaphoreHandle_t baton;

// the setup function runs once when you press reset or power the board
void setup() {
  Serial.begin(115200);

  baton = xSemaphoreCreateMutex();

  xTaskCreatePinnedToCore(codeForCore0,"Core0Task",1000,NULL,1,&core0,0);
  delay(500);  // needed to start-up task1
  xTaskCreatePinnedToCore(codeForCore1,"Core1Task",1000,NULL,1,&core1,1);

}

//Implement Hardware Code Here
void codeForCore0( void * parameter ){
  for (;;) {
    
    xSemaphoreTake( baton, portMAX_DELAY );
    //TODO
    //Put Code that touches shared data here
    xSemaphoreGive( baton );
  }
}

//Implement Net Code Here
void codeForCore1( void * parameter ) {
  for (;;) {

    xSemaphoreTake( baton, portMAX_DELAY );
    //TODO
    //Put Code that touches shared data here
    xSemaphoreGive( baton );  
  }
}


void loop() {
  delay(10);
}