package com.mayrthom.radprologviewer.database.device;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Device {
    @PrimaryKey(autoGenerate = true)
    public long deviceId;
    public String deviceType;
    public String serialNumber;
    public float conversionValue;


    public Device(String deviceType, String serialNumber, Float conversionValue) {
        this.deviceType = deviceType;
        this.serialNumber = serialNumber;
        this.conversionValue = conversionValue;

    }

    //Generate Device from serial string
    public Device(String deviceString, float conversionValue) throws Exception {
        try {
            deviceString = deviceString.replace("\n","");
            String[] s = deviceString.split(";");
            deviceType = s[0].replace("OK ", "");
            serialNumber = s[2];
            this.conversionValue = conversionValue;
        }
        catch (Exception e)
        {
            throw new Exception("Wrong Input");
        }
    }

    @NonNull
    @Override
    public String toString()
    {
        return "Model Name: " + deviceType +  "\nSerial Number: " + serialNumber;
    }
}
