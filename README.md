# StrokeCounter
Android app and Arduino paddle attached gadget to count paddle strokes for kayakers.

This is something I didn't see on the market, so I made one myself.  I used an Arduino Pro Mini and 
an MPU-6050 motion sensor to count paddle strokes and a BlueSMiRF to connect to the phone.  The package 
is strapped to the paddle shaft using a 3D printed block and a waterproof bag.

As it is, the app shows stroke count, elapsed time and average cadence.

Next steps - Explore using the MPU-6050's FIFO buffer to read in bursts and put the Arduino to POWER_SAVE in the
meantime - this should save some more power.  Waterproof mechanics.

-Mikko L 10/2015
