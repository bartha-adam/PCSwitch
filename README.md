Features:
 - Turn On/Off PC in LAN instantly or delayed using Android app
 - Allow multiple users to control the same PC: user A sets shutdown with delay of 30mins, user B can cancel or modify the scheduled shutdown

This app was developed by my own necesity for turning On/Off a PC used as media server, with a smartphone.

Components:
 - PCSwitch - Android app for controling PC on/off operations
 - PCSwitch_Common - Common code used by server and Android app
 - PCSwitch_Server - Server component, handles and performs commands from client

Build instructions:
 - Build PCSwitch_Common
  - make sure PCSwitch_Common/PCSwitch_Common.jar is created and copied to PCSwitch/app/libs
 - Build PCSwitch_Server 
  - server needs to be launched with port 12002 (hardcoded in PCSwitch for now), specified as first argument
 - Build PCSwitch Android app
  - Android Studio 1.1 or newer needed