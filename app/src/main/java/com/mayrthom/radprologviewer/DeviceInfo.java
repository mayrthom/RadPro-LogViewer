package com.mayrthom.radprologviewer;

import androidx.annotation.NonNull;

public class DeviceInfo {

    private final String deviceName;
    private final String deviceNumber;

    public DeviceInfo(String deviceString) throws Exception {
        try {
            deviceString = deviceString.replace("\n","");
            String[] s = deviceString.split(";");
            deviceName = s[0].replace("OK ", "");
            String firmwareVersion = s[1];
            deviceNumber = s[2];
        }
        catch (Exception e)
        {
            throw new Exception("Wrong Input");
        }
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceNumber() {
        return deviceNumber;
    }

    @NonNull
    @Override
    public String toString()
    {
        return "Model Name: " + deviceName +  "\nSerial Number: " + deviceNumber;
    }
}
