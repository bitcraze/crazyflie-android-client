---
title: User Instructions
page_id: user-instructions
---

## Run the Crazyflie Android app

  - Connect the Crazyradio dongle to the Android device with the USB OTG cable.
  - A pop-up should ask you which app you'd like to use with the USB device.
  - Select the Crazyflie app ("Crazyflie Client").
  - When you run the app for the first time, you can scan for the [Crazyflie (must be switched on). (Preferences button -> Connection Settings -> Radio Scan)
    * Or you can set channel and data rate in the preferences manually
  - Once the channel and data rate is set, you can connect to it. (Connect button)
    * The green LEDs on the Crazyflie and the Crazyradio should blink rapidly.
  - Use the on-screen controls to fly the Crazyflie
    * **WARNING: Be careful with the on-screen controls, they are harder to use than a gamepad.**

## Connecting to the Crazyflie 2.0 via Bluetooth LE 

  * Your Android device must support Bluetooth LE (Low Energy) 4.0 and run Android 4.4+ (eg. Nexus 4, Nexus 5, Samsung Galaxy S4)
  * Make sure that you have **not** paired your phone with the Crazyflie 2.0 via your phone's general Bluetooth settings, otherwise the app will not be able to connect properly (connection will claim to be successful, but controls won't work)
  * If no Crazyradio USB dongle is connected, a connection over Bluetooth LE will be attempted
    * **Please note:** The (old) Crazyflie 1.0 does NOT support Bluetooth LE, only Crazyflie 2.x has support for it

## Connecting an external controller

Instead of using the on-screen controls, you can also connect an external controller like a PS3 Controller (or compatible):

### Connect a PS3 Controller (over USB) 

In addition to the USB OTG cable, you'll also need:
  * USB Y-Cable
  * USB Hub
  * PS3 Controller (or compatible joystick/gamepad)

Setup:
  - Connect a USB Host (OTG) adapter to your Android device
  - Connect a USB Y-cable with the USB Host adapter
  - Connect a wall-plug with the (red) power plug of the Y-cable (you need external power)
  - Connect the upstream port of a USB hub with the socket of the Y-cable
  - Plug the PS3 controller and the crazyradio into the USB hub
  - The Crazyflie Android client should say "Using external PS3 controller" and the crazyradio's LEDs should light up for a second
  - Press the "PS" button and move the analog sticks (the pitch/roll/thrust/yaw on-screen values should change)

Alternative setup without a USB Y-cable:

![cf android app with controller](/docs/images/cf_android_app_with_controller.jpg)

### Connect a PS3 Controller (over Bluetooth) 
  * Some Android devices made by Sony, like the Xperia Z1, support the PS3 controller natively ([forum post](http://forum.bitcraze.se/viewtopic.php?f=11&t=920)
  * Otherwise your Android device must be rooted
  * You also need to install the Sixaxis Controller app (paid)
    * [Sixaxis Controller App](https://play.google.com/store/apps/details?id=com.dancingpixelstudios.sixaxiscontroller&hl=en)
  * Use the Sixaxis Controller app to pair and connect the PS3 Controller
  * You might have to press the "PS" button to activate the PS3 Controller
  * **Please note:** You cannot connect to the crazyflie 2.x while you are connected to the PS3 controller via Bluetooth. (This actually works both with BLE and Crazyradio on certain Sony Xperia phones (e.g. Z2,Z3). Only with Crazyradio on Nexus 5, more testing needs to be done though)

## Android device compatibility 

| Manufacturer | Name | Android version | Crazyradio over USB OTG | Bluetooth LE |
| --- | --- | --- | --- | --- |
| Asus | Nexus 7 (2012) | 4.3 | supported | ? |
| HP | Touchpad | 4.0.4 (Cyanogenmod 9) | [Crazyradio not recognized](http://forum.bitcraze.se/viewtopic.php?f=6&t=362) | not supported |
| HTC | Desire | 2.1 | Unsupported Android version | not supported |
| ::: | One M7 | 4.x? | supported? | supported |
| ::: | One M8 | 4.4.2? | supported? | supported |
| LG  | Nexus 4 | 4.4 | ? | supported? |
| ::: | Nexus 5 | 4.4 | supported (Crazyradio firmware version 0.51+ is required!) | supported? |
| ::: | :::     | 5.0 | supported (Crazyradio firmware version 0.51+ is required!) | supported |
| ::: | :::     | 5.1 | supported (Crazyradio firmware version 0.51+ is required!) | supported |
| Motorola | Defy | 4.4.4 (Cyanogenmod 11) | supported | not supported |
| :::      | Moto G | 4.3 | supported | supported? |
| Samsung | Galaxy Nexus | 4.2.2/4.3 | supported | ? |
| :::     | Galaxy S2 | ? | supported | ? |
| :::     | Galaxy S3 | ? | supported | ? |
| :::     | Galaxy S4 | 4.3   | supported? | not supported |
| :::     | :::       | 4.4.2 | supported | supported |
| :::     | Nexus 10 | 4.4 (Cyanogenmod) | supported | supported (a bit laggy) |
| Sony | Tablet S | ? | [crazyradio not recognized](http://forum.bitcraze.se/viewtopic.php?f=6&t=362) | ? |
| :::  | Z3 | 4.4.4 | supported | supported |
| :::  | Z2 Tablet | 5.0.2 | supported | supported |
| Verizon | Ellipsis 7 | 4.2 | ? | not supported |

## Known issues 
  * Flight data from the Crazyflie is not displayed
  * Larger screen size of tablets is not used efficiently
  * No reliable connection quality indicator
  * Repeatedly connecting and disconnecting the Crazyradio can cause problems

More can be found here: https://github.com/bitcraze/crazyflie-android-client/issues

## Reporting bugs 

Bugs and feature requests should be added as GitHub issues: https://github.com/bitcraze/crazyflie-android-client/issues
