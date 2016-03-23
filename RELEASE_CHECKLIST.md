Crazyflie Android Client - Release Checklist
=====================================

Connect
-------

* **BLE connect**
  * Try connecting while no Crazyradio is attached and **no** nearby CF2 is switched on
     * => expected error: BLE connection timeout (Toast)
  * Try connecting while **no** Crazyradio is attached and Bluetooth is turned off
     * => expected error: An app wants to turn on Bluetooth (Pop-up)
  * Try connecting while **no** Crazyradio is attached and nearby CF2 is switched on
     * connect button should turn BLUE, CF connection LED should blink rapidly
     * play gently with thrust to see if the control commands are sent

* **Crazyradio connect**
  * Try connecting while Crazyradio is attached and **no** nearby CF is switched on
     * => expected error: "Connecting", "Too many packets lost" and "Disconnected" toasts
  * Try connecting while Crazyradio is attached and nearby CF2 is switched on with wrong channel
     * => expected error: "Connecting", "Too many packets lost" and "Disconnected" toasts
  * Try connecting while Crazyradio is attached and nearby CF is switched on
     * connect button should turn GREEN. CF connection LED should blink rapidly
     * play gently with thrust to see if the control commands are sent


Preferences
-----------

* Preferences - Crazyradio stats
  * Open the preferences menu while Crazyradio is attached
  * Open the connection settings
  * Radio stats should show firmware version number and serial number.
* Preferences - Crazyradio scan
  * Open the preferences menu while Crazyradio is attached and nearby CF is switched on
  * Open the connection settings
  * Start radio scan (how long does it take?)
  * => at least one CF should be found
* Preferences - Crazyradio scan
  * Open the preferences menu while **no** Crazyradio is attached
  * Open the connection settings
  * => all Crazyradio settings should be greyed out

Bootloader
-----------

* Remove JSON file manually, disable network connection and restart the activity
  * expected error: "No local file found. No network connection available. Please check your connectivity."
* Remove JSON file manually and see if it's downloaded on the next start of the Bootloader activity
* With an existing JSON file, disable network connection and restart activity
  * => list should be filled with last available JSON
* Firmware list should be filled
* Check the order of the firmwares (newest should be on the top)
* Try info button to see release notes
* Remove all downloaded firmwares, disable network connection and start flashing
* Try flashing while no Crazyradio is attached
  * => expected error: "Please make sure that a Crazyradio (PA) is connected."
* Try flashing while Crazyradio is attached
  * Try flashing while no nearby CF is switched on (in bootloader mode)
      * => should show search dialog
      * => expected error: "No Crazyflie found in bootloader mode" (after 10 seconds)
  * Try flashing CF1 firmware onto CF2
      * => expected error "Incompatible firmware version"
  * Try flashing CF2 firmware onto CF1
      * => expected error "Incompatible firmware version"
  * Try flashing CF2 firmware onto CF2
      * => progress bar should move
      * => console should be filled with messages
      * => after flashing, CF should be restarted automatically into firmware mode
  * Try interrupting a flashing process
      * double back button
          * => connection should be stopped (GREEN LED on CF should stop blinking)
      * detach Crazyradio
          * => connection should be stopped


Attaching/Detaching Crazyradio
------------------------------

* Attach Crazyradio
  * "Crazyradio attached" Toast should appear and sound played
  * "Choose an app for the USB device" should appear
  * Select Crazyflie Client
* Detach Crazyradio
  * "Crazyradio detached" Toast should appear and sound played
* Detach Crazyradio while control commands are sent
  * expected error: "Too many packets lost" (should actually be a different error message)