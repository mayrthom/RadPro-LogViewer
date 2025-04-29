package com.mayrthom.radprologviewer.serialPort;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.mayrthom.radprologviewer.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SerialPortManager {
    public UsbSerialPort usbSerialPort;
    private UsbManager usbManager;
    private UsbDeviceConnection usbConnection;
    public boolean connected = false;

    public static final int BAUD_RATE = 115200;
    public static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";


    public boolean connect(Context context, int deviceId, int portNum) throws IOException {
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        UsbDevice device = findDeviceById(deviceId);
        if (device == null) throw new IOException("USB Device not found");

        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) throw new IOException("No USB serial driver found");

        if (driver.getPorts().size() <= portNum)
            throw new IOException("Invalid port number");

        usbSerialPort = driver.getPorts().get(portNum);
        usbConnection = usbManager.openDevice(driver.getDevice());

        if (usbConnection == null && !usbManager.hasPermission(driver.getDevice())) {
            //usbPermission = DeviceInfoFragment.UsbPermission.Requested;
            int flags = PendingIntent.FLAG_MUTABLE;
            Intent intent = new Intent(INTENT_ACTION_GRANT_USB);
            intent.setPackage(context.getPackageName());
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return false;
        }

        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                throw new IOException("connection failed: permission denied");
            else
                throw new IOException("connection failed: open failed");
        }
        usbSerialPort.open(usbConnection);
        usbSerialPort.setParameters(BAUD_RATE, 8, 1, UsbSerialPort.PARITY_NONE);

        connected = true;
        return true;
    }

    private UsbDevice findDeviceById(int deviceId) {
        for (UsbDevice d : usbManager.getDeviceList().values()) {
            if (d.getDeviceId() == deviceId)
                return d;
        }
        return null;
    }

    public void disconnect() {
        if (usbSerialPort != null) {
            try {
                usbSerialPort.close();
            } catch (IOException ignored) {}
        }
        usbSerialPort = null;
        usbConnection = null;
        connected = false;
    }

    public void send(String command) throws IOException {
        if (!connected) throw new IOException("Not connected");
        byte[] data = (command + "\n").getBytes(StandardCharsets.UTF_8);
        usbSerialPort.write(data, 2000);
    }

    public void clearInputBuffer(int timeout) throws IOException {
        try {
            int len = 1;
            byte[] buffer = new byte[usbSerialPort.getReadEndpoint().getMaxPacketSize()];
            while (len != 0) {
                len = usbSerialPort.read(buffer, timeout);
            }
        } catch (IOException e) {
            disconnect();
            throw new IOException(e);
        }
    }

    public String read(int timeout) throws IOException {
        if (!connected) throw new IOException("Not connected");
        String result;
        do {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int size = 1000 * usbSerialPort.getReadEndpoint().getMaxPacketSize();
            byte[] buffer = new byte[size+1];
            int len = 1;
            while (len != 0) {
                len = usbSerialPort.read(buffer,size, timeout);
                out.write(buffer, 0, len);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                result = out.toString(StandardCharsets.UTF_8); //only supported in SDK 33
            else
                result = new String(out.toByteArray(), StandardCharsets.UTF_8); // for older android versions
        }
        while (result.isBlank()); // load again if the result of the first attempt is empty

        return result.split("\n")[0]; //only interested in the first line
    }

    public String sendAndRead(String command, int timeout) throws IOException {
        send(command);
        return read(timeout);
    }
}
