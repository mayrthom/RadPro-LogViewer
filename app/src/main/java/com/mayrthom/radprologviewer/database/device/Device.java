package com.mayrthom.radprologviewer.database.device;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Device {

    @PrimaryKey()
    @NonNull
    public String deviceId; //deviceId is the Unique ID of the Microcontroller of the Geiger counter
    public String deviceType;
    public float conversionValue;

    public Device(String deviceType, String deviceId, Float conversionValue) {
        this.deviceType = deviceType;
        this.deviceId = deviceId;
        this.conversionValue = conversionValue;
    }

    //Generate Device from serial string
    public Device(String deviceString, float conversionValue) throws Exception {
        try {
            deviceString = deviceString.replaceAll("OK|\\r|\\n", "");
            String[] s = deviceString.split(";");
            String serialNumberString = s[2];
            this.deviceType = s[0];
            this.deviceId = serialNumberString;
            this.conversionValue = conversionValue;
        }
        catch(Exception e) {

            throw new Exception("Wrong Input");
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Model Name: " + deviceType +  "\nSerial Number: 0x" + deviceId;
    }

}
