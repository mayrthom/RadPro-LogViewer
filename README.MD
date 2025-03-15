# RadPro Log-Viewer

This android app displays and stores the logged data of geigercounters with the [Radpro Firmware](https://github.com/Gissio/radpro).

## Features
- Load logged data from Geiger counter.
- Plot logged data.
- Store logged data locally on the android device.
- Export .csv files of logged data in the format "Timestamp[unix time],radiation[CPS]"
- The app also has a dark mode, which is set according to the current system mode.

Each datalog is stored on the Android device along with information about the Geiger counter it was loaded from. This allows to store data from multiple Geiger counters.

## Installation
Currently there's just the APK-file provided under the realese section, which can be installed on any android device above android version „Oreo“ 8.0 (API 26). In future it may be provided at the android play store, but for now it is a beta version. 

## To-dos in future
- Design a clear and structured ER model for the Room database to optimize data processing. Currently, each datalog is stored as a class, and the list of entries is serialized as JSON to be stored in the Room database.
- Enable the combination of data from multiple logs and display them in a shared chart.
- Add the option to view the chart on a logarithmic scale.
- Expand functionality with additional features, such as overlaying the radiation profile with GPS position or implementing an adjustable moving average filter, and much more.

## Credits
The app uses the following libraries:
- [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) - For the serial USB Connection
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - For plotting the data
- GSON - To serialize the List as JSON in order to store it in the Room Database
- Room - for storing downloaded datalogs persistently
## Screenshots
The peak in the diagram was produced with a check source, to check if it is working correctly.  
<img src="screenshots/screenshot2.jpeg" height="600"> <img src="screenshots/screenshot3.jpeg" height="600">

<img src="screenshots/screenshot1.jpeg" >  


## Personal Motivation
I thought it would be very useful to access the logged data anywhere without needing a computer. Since the only way to view the data was the GeigerLog software, I wanted a simpler solution.
Also I am not a professional App Developer.  
Although I learned Java and JavaFX for desktop applications at university, this is my first Android app. That's why the code is probably not written according to the best design principles.
