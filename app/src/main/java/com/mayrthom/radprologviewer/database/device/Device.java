package com.mayrthom.radprologviewer.database.device;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Device {

    @PrimaryKey()
    public long deviceId; //deviceId is the Unique ID of the Microcontroller of the Geiger counter
    public String deviceType;
    public float conversionValue;

    public Device(String deviceType, long deviceId, Float conversionValue) {
        this.deviceType = deviceType;
        this.deviceId = deviceId;
        this.conversionValue = conversionValue;
    }

    //Generate Device from serial string
    public Device(String deviceString, float conversionValue) throws Exception {
        try {
            deviceString = deviceString.split("OK ")[1];
            deviceString = deviceString.replaceAll("\\r|\\n", "");
            String[] s = deviceString.split(";");
            String serialNumberString = s[2];
            this.deviceType = s[0];
            serialNumberString = serialNumberString.replaceAll("[^0-9a-fA-F]", "");
            this.deviceId = Long.parseLong(serialNumberString,16);
            this.conversionValue = conversionValue;
        }
        catch(Exception e) {
            throw new Exception("Wrong Input");
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Model Name: " + deviceType +  "\nSerial Number: 0x" + Long.toHexString(deviceId);
    }

}
