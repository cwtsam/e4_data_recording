# E4 Wristband EmpaLink Recording for Android

## Introduction

The project is an Android app at enables streaming of data from the Empatica E4 Wristband to your Android mobile device and data recording to CSV files on your device.

This project was modified from the sample EmpaLink Project given by Empatica (https://github.com/empatica/empalink-sample-project-android).

Three main functions were added:

### 1) Real-Time Time-Domain Heart-Rate Variability (HRV) Calculations

The app calculates Heart-Rate Variability measures and records it on the phone.

Under MainActivity.java, inter-beat-intervals (IBI) stream is accessible through the device.

Time-Domain HRV components calculate from IBI: Heart Rate (HR), Standard Deviation of NN intervals or IBI (SDNN) and Root-mean-square of Successive Differences in IBI (RMSSD).

- HR = 60/IBI ---- Calculated for every IBI sample

- SDNN and RMSSD use the same equations as found here (https://www.kubios.com/about-hrv/). ---- Values are calculated when a new IBI sample is detected, calculations use the past 60 samples of IBI.

### 2) Recording Data and Writing to CSV Files

The data is recorded to CSV files on the phone.

- FileWriter.java method helps to write the data to separate files (for current code: eda.csv and ibi.csv). These files should be saved to the Documents folder of the Android device.

- If you would like to modify which streams are recorded (temperature, accelerometer etc.), you would have to duplicate appendIBI or appendEDA functions in FileWriter.java, and change it to the stream that you want to add.

- In MainActivity.Data, a FileWriter object is created, getInstance and append functions are called to record data for each stream. 

### 3) Begin and End Timestamp Event Logs to CSV File

The app allows users to indicate event timestamps remotely on the phone. In this way, you would not need to indicate event timestamps by pressing the button on the E4 wristband.

- Buttons (Begin and End) are created to help mark the timestamps for the beginning and end of the recording sessions. 

- FileWriter.java helps to write these timestamps of each stream to the timestamp.csv file.


## Information on the Original Sample Project

The originial sample project gives you the boilerplate code you need to connect to an Empatica E4 device and start streaming data.

The sample application implemented in the project has very simple functionalities:

- It initializes the EmpaLink library with your API key.
- If the previous step is successful, it starts scanning for Empatica devices, till it finds one that can be used with the API key you inserted in the code.
- When such a device has been found, the app connects to the devices and streams data for 10 seconds, then it disconnects.

## Setup

- Clone / download this repository.
- Open the sample project in Android Studio.
- Make sure you have a valid API key. You can request one for your Empatica Connect account from our [Developer Area][1].
- Edit `MainActivity.java` and assign your API key to the `EMPATICA_API_KEY` constant .
- Download the Android SDK from our [Developer Area][1].
- Unzip the archive you've downloaded and copy the `.aar` file you'll find inside into the `libs` folder contained in the sample project.
- Build and run the project.
- If a device is in range and its light is blinking green, but the app doesn't connect, please check that the discovered device can be used with your API key. If the `allowed` parameter is always false, the device is not linked to your API key. Please check your [Developer Area][1].

If you need any additional information about the Empatica API for Android, please check the [official documentation][2].

[1]: https://www.empatica.com/connect/developer.php
[2]: http://developer.empatica.com
