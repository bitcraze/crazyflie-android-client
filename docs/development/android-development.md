---
title: Android client development
page_id: android-development
---

This guide will help you getting started with setting up a development environment for the Crazyflie Android client, running the app from source code and contributing code.

## Installation

### Requirements
  * An Android device running version 4.0 or higher (the code might be changed to run on lower versions, but this has not been tested yet)
  * USB On-the-go (OTG) cable (aka. USB Host cable) to connect the Android device to the Crazyradio dongle

### Installing the Android Development Tools (ADT)
  - Download the most recent Android Development Tools from http://developer.android.com/sdk/index.html
    * get the latest ADT Bundle (eg. adt-bundle-windows-x86-20130917.zip for Win32)
  - Extract the zip file and start Eclipse (adt-bundle-windows-x86-20130917/eclipse/eclipse.exe)
  - Start the Android SDK Manager (Window -> Android SDK Manager)
    * select Android 4.0 (API 14) and install all of its components
    * using an actual Android device for development is recommended
    * If you still want to use the emulator for development on a Windows machine, then you should use the "Intel x86 Atom System Image".  (Make sure that you use the Intel x86 Emulator Accelerator (HAXM) when your machine support the VT-x extension. This speeds up the emulator considerably.)

If you have problems installing the ADT environment or setting up the emulator you might find some help here:
  * http://developer.android.com/sdk/installing/bundle.html
  * http://developer.android.com/tools/help/sdk-manager.html
  * http://developer.android.com/tools/devices/index.html
  * http://developer.android.com/tools/device.html

### Getting the Crazyflie Android client source code
  - Install a Git client for your operating system
    * Windows client from http://msysgit.github.io
    * Linux client, e.g. on Debian based systems with "sudo apt-get install git"
  - The Eclipse Git plug-in (EGit) is already included in your ADT environment, but you should update it to the latest version
    * Use the Update manager (Help -> Install new software...)
    * Update site: http://download.eclipse.org/egit/updates
  - With the EGit plug-in you can checkout the Crazyflie Android client source code from https://github.com/bitcraze/crazyflie-android-client.git
    * or on the commandline: ```git clone https://github.com/bitcraze/crazyflie-android-client.git```

### Running the Android client from source code
  - On Windows: install USB drivers for your Android device (http://developer.android.com/tools/extras/oem-usb.html)
  - Enable USB debugging on your Android device (Settings > Developer options)
    * You might need to enable the developer options first!
  - Connect your PC to the Android device via USB or Wifi
  - "Run as Android application" (this deploys and installs the APK to your Android device)
  - Use a USB On-the-go (OTG) cable (aka. USB Host cable) to connect your Android device to the Crazyradio dongle
  - Run the Crazyflie app (see also [Crazyflie Android client user guide](/docs/userguides/user-instructions.md))

More info about USB drivers, developer options and debugging setup can be found here:
  * http://developer.android.com/tools/device.html#setting-up

## Reporting bugs or requesting features
Bugs and feature requests should be added as GitHub issues: https://github.com/bitcraze/crazyflie-android-client/issues

## Contributing your code
If you consider contributing code, then you should create a [github](https://github.com) account, fork the [crazyflie-android-client](https://github.com/bitcraze/crazyflie-android-client) and send a [pull request](https://github.com/bitcraze/crazyflie-android-client/pulls)

### Coding standard
Due to a lack of time and laziness the code is still messy, but in general the coding style should follow the [Android Code Style Guidelines for Contributors](http://source.android.com/source/code-style.html).

  * [Use spaces for indentation](http://source.android.com/source/code-style.html#use-spaces-for-indentation)
  * [Follow field naming conventions](http://source.android.com/source/code-style.html#follow-field-naming-conventions)
