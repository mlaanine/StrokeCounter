/*
10/2015 Mikko L

Sketch for reading 2 position switches and filter the output.
Reading interval can be set using a timer - no delay() is used.

Switches are mounted opposite to each other, and both are read into 8 bit (=byte) 
FIFO buffers. When one buffer is 0xFF and the other is 0x00, consider the state as 
valid and increment the count.

The current count is sent to Serial port.  The serial port can receive commands to 
start, stop and reset the counter.
*/

// Set IO pins
const int ledPin = 13;
const int sw0Pin = 2;
const int sw1Pin = 3;

// Set the timer, unsigned long can last up to 50 days before rolling over
unsigned long timer;
const unsigned long INTERVAL = 10; // milliseconds

// Establish buffers and state variables
byte sw0Reading = 0;
byte sw0Buffer = 0;
byte sw1Reading = 0;
byte sw1Buffer = 0;
bool isRunning = true;
unsigned int count = 0;
bool state = 0;
bool prevState = 0;
byte serialCommand;

void setup() {
  // Configure IO pins
  pinMode(ledPin, OUTPUT);
  // INPUT_PULLUP enables the internal ~20k pull-up resistor.
  // Connect the other end of the switches to GND.
  pinMode(sw0Pin, INPUT_PULLUP);  
  pinMode(sw1Pin, INPUT_PULLUP);  
  // Start Serial port to send the count
  Serial.begin(115200);
  // Initialize the timer
  timer = millis();
}

void loop() {
  // If the counter is running,
  if(isRunning){
    // If the INTERVAL has elapsed,
    if(millis()-timer > INTERVAL){
      // Reset the timer to wait for the next INTERVAL 
      timer += INTERVAL;
      // Read the switch states
      sw0Reading = digitalRead(sw0Pin);
      sw1Reading = digitalRead(sw1Pin);
      // Bit shift the buffers to the left and add the latest reading
      sw0Buffer = sw0Buffer << 1;
      sw0Buffer += sw0Reading;
      sw1Buffer = sw1Buffer << 1;
      sw1Buffer += sw1Reading;
      
      // Check if the buffers are all ones or all zeros, change the state accordingly
      if((sw0Buffer == 0xFF) && (sw1Buffer == 0x00)){
        state = 0;
      }     
      if((sw0Buffer == 0x00) && (sw1Buffer == 0xFF)){
        state = 1;
      }     
      
      // If the state changed, increment the count
      if(state != prevState){
        count++;
        digitalWrite(ledPin, !digitalRead(ledPin)); // Toggle LED on or off
        prevState = state; // Store the state
        Serial.println(count);  // Send the count to Serial port
      }  
    }
  }

  // Functions outside of the timer:

  // Read from Serial port, if there is any incoming data.
  if(Serial.available()){
    serialCommand = Serial.read();
    // Reset count
    if(serialCommand == 0x72){   // r for "reset"
      count = 0;
      Serial.println("RESET");
    }
    // Start and stop counting
    if(serialCommand == 0x73){   // s for "start/stop"
      isRunning = !isRunning;
      if(isRunning){
        Serial.println("START");
      }
      else{
        Serial.println("STOP");
      }
    }
  }
}
