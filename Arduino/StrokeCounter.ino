/*
10/2015 Mikko L

Sketch for a paddle stroke counter using a MPU-6050 motion sensor.
Using only the Y-axis of the sensor, readings are taken using a timer.
If the sensor value exceeds a set limit, a 0 or 1 is written into a buffer
depending if it's positive or negative.  When the buffer is all zeros or all
ones, consider the orientation as valid and increment the counter.

The serial port sends out the count and can receive commands to start, stop 
and reset the counter.

BlueSMiRF config: Disable inquiry:          SI,0000
                  Enable Sniff Mode (50ms): SW,0050
                  Switch off LED (GPIO2):   S%,0400
Sniff mode saves 15-20mA of current and at 50ms interval doesn't increase latency. At 100ms
(SW,0A00) latency is notable.  The green LED on GPIO2 has a 330Ohm resistor, so it draws
4.5mA if left on.
*/

// I2C comms library
#include<Wire.h>;
// Power saving for Arduino
#include<avr/sleep.h>
#include<avr/power.h>

// IO pins
// Wire.h sets up I2C on pins A4 (SDA) and A5 (SCL)
const int ledPin = 13;

// MPU-6050 registers
const int MPU_ADDR = 0x68;
const int ACCEL_YOUT_H = 0x3D;
const int POWER_MGMT_1 = 0x6B;
const int POWER_MGMT_2 = 0x6C;

// Acceleration reading and limit.  Default scale is +/- 2g, 16 bit;
// Therefore one g is equal to +/- 2^14.  10 degreees tilt equals sin(10) * 2^14
// = 2845
const float LIMIT_DEG = 10.0;
int accRaw;
int limitRaw;

// Buffer and control
unsigned int strokeCount = 0;
byte readBuffer;
bool state = 0;
bool prevState = 0;
bool isRunning = false;
byte serialCommand;

// Timer/Counter1 period, 25ms interval (=40Hz)
const int TIMER1_PRELOAD = 0x3CB0;

// Interrupt service routine for the sleep timer
ISR(TIMER1_OVF_vect)
{
    // Reload the timer
    TCNT1 = TIMER1_PRELOAD;
}

// Function to enter and exit IDLE mode. Uses ./avr/sleep.h and ./avr/power.h
void enterIdleMode(void){
    set_sleep_mode(SLEEP_MODE_IDLE);
    sleep_enable();
    // Disable Timer0 - this will destroy millis() but otherwise Arduino wakes up every 1 ms.
    power_timer0_disable();
    // Disable also I2C
    power_twi_disable();
    // Go to Idle mode here
    sleep_mode();
    // ...and wake up. Program resumes here after the ISR
    sleep_disable();
    power_timer0_enable();
    power_twi_enable();
}

void setup() {
    // Power saving by shutting down unused bits
    pinMode(ledPin, OUTPUT);
    digitalWrite(ledPin, LOW);
    power_adc_disable();
    power_spi_disable();
    power_timer2_disable();
    
    // Set up Timer/Counter1
    TCCR1A = 0x00;
    TCCR1B = 0x02;  // Prescaler = 8
    TCNT1 = TIMER1_PRELOAD;  // Preload the counter
    TIMSK1 = 0x01;  // Enable the timer overflow interrupt
    
    // Convert the degrees limit to an integer
    float pi = 3.14159;
    float rad = pi / 180;
    limitRaw = int(sin(LIMIT_DEG * rad) * 0x3FFF);
  
    // Start up and configure the MPU-6050
    Wire.begin();
    Wire.beginTransmission(MPU_ADDR);
    Wire.write(POWER_MGMT_1);  
    Wire.write(0x28);  // Set CYCLE and TMP_DIS bits
    Wire.endTransmission(false);
    Wire.beginTransmission(MPU_ADDR);
    Wire.write(POWER_MGMT_2);
    Wire.write(0xC7);  // Set CYCLE frequency to 40Hz and disable gyro on all axes
    Wire.endTransmission(true);
  
    // Start Serial communication
    Serial.begin(115200);
}

void loop() {
    // If the counter is running,
    if(isRunning){
        // Read the acceleration
        Wire.beginTransmission(MPU_ADDR);
        Wire.write(ACCEL_YOUT_H);  // Start address of register
        Wire.endTransmission(false);
        Wire.requestFrom(MPU_ADDR, 2, true);  // Request 2 registers (high and low bits)
        accRaw = Wire.read() << 8 | Wire.read();  // Read high bits, left shift one byte and read low bits
  
        // If the reading exceeds the set limit
        if(abs(accRaw) > limitRaw){
            // bit shift the buffer to left
            readBuffer = readBuffer << 1;
            // if positive, add "1" to the buffer
            if(accRaw > 0){
                readBuffer += 1;
            }
        }
  
        // Check if the buffer is all ones or all zeros, change the state accordingly
        // Tried to use bitwise syntax to mask the buffer bits, got weird results with ~ (NOT) operator
        if(readBuffer == 0xFF){
            state = 1;  
        }
        if(readBuffer == 0x00){
            state = 0;
        }
  
        // If the state changed, increment the count
        if(state != prevState){
            strokeCount++;
          /* Enable toggling LED on or off.  The LED has a 330Ohm series resistor so it draws
             9.6mA when on (assume 1.8V voltage drop) - huge! */
            // digitalWrite(ledPin, !digitalRead(ledPin)); 
            prevState = state; // Store the state
            Serial.println(strokeCount);  // Send the count to Serial port
        }        
    }

    // Read from Serial port, if there is any incoming data.
    if(Serial.available()){
        serialCommand = Serial.read();
        // Reset count
        if(serialCommand == 0x72){   // r for "reset"
            strokeCount = 0;
            Serial.println("RESET");
        }
        // Start counting
        if(serialCommand == 0x53){   // S for "start"
            isRunning = true;
            Serial.println("START");
        }
        // Stop counting
        if(serialCommand == 0x73){   // s for "stop"
            isRunning = false;
            Serial.println("STOP");
        }
    }
    // Main loop up to here takes 544us to execute when isRunning == true.
    // Everything done, go to sleep and wake up at the next timer interrupt
    enterIdleMode();
}
