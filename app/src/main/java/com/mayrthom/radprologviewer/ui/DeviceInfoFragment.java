package com.mayrthom.radprologviewer.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentTransaction;

import com.mayrthom.radprologviewer.BuildConfig;
import com.mayrthom.radprologviewer.R;
import com.mayrthom.radprologviewer.viewModel.SharedViewModel;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mayrthom.radprologviewer.DataList;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.viewModel.SharedViewModelFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DeviceInfoFragment extends androidx.fragment.app.Fragment {

    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 1000;
    private static final int BAUD_RATE = 115200;
    private int deviceId, portNum;
    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;
    private TextView deviceIdText, statusText;
    private View viewBtn, loadBtn;
    private Device device;
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;
    private SharedViewModel sharedViewModel;
    private AlertDialog alertDialog;

    public DeviceInfoFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
        mainLooper = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deviceId = getArguments().getInt("device");
        portNum = getArguments().getInt("port");
    }

    @Override
    public void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(requireActivity(), broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        requireActivity().unregisterReceiver(broadcastReceiver);
        super.onStop();
    }

    /* Navigating back to the last fragment */
    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle("Device Info");
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getParentFragmentManager().popBackStack();}});
        if(!connected && (usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted)) {
            resetViews();
            mainLooper.post(this::connect);
        }
    }

    @Override
    public void onPause() {
        if(connected) {
            disconnect();
        }
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_device, container, false);
        SharedViewModelFactory factory = new SharedViewModelFactory(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);
        deviceIdText = view.findViewById(R.id.device_text);
        statusText = view.findViewById(R.id.status_text);

        viewBtn = view.findViewById(R.id.view_data_btn);
        loadBtn = view.findViewById(R.id.load_data_btn);

        enableButtons(false);

        viewBtn.setOnClickListener(v ->{
            enableButtons(false);
            readAndViewData(false);
        });

        loadBtn.setOnClickListener(v ->{
            enableButtons(false);
            readAndViewData(true);
        });
        return view;
    }

    private void connect() {
        try {
            UsbDevice device = null;
            UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
            for(UsbDevice v : usbManager.getDeviceList().values())
                if(v.getDeviceId() == deviceId)
                    device = v;
            if(device == null) {
                enableButtons(false);
                statusText.setText("connection failed: device not found");
                return;
            }
            UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
            if(driver == null) {
                statusText.setText( "connection failed: no driver for device");
                return;
            }
            if(driver.getPorts().size() < portNum) {
                statusText.setText("connection failed: not enough ports at device");
                return;
            }
            usbSerialPort = driver.getPorts().get(portNum);
            UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
            if (usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
                usbPermission = UsbPermission.Requested;
                int flags = PendingIntent.FLAG_MUTABLE;
                Intent intent = new Intent(INTENT_ACTION_GRANT_USB);
                intent.setPackage(getActivity().getPackageName());
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, flags);
                usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
                return;
            }
            if (usbConnection == null) {
                enableButtons(false);
                if (!usbManager.hasPermission(driver.getDevice()))
                    statusText.setText("connection failed: permission denied");
                else
                    statusText.setText("connection failed: open failed");
                return;
            }

            usbSerialPort.open(usbConnection);
            try{
                usbSerialPort.setParameters(BAUD_RATE, 8, 1, UsbSerialPort.PARITY_NONE);
            }
            catch (UnsupportedOperationException e){
                statusText.setText(e.getMessage());
            }
            connected = true;
            statusText.setText("connected!");
            clearInputBuffer(1000);
            printDeviceId();
            checkTimeDiff(61); //Check if Time on Geiger counter is the same as on the android device

        } catch (Exception e) {
            statusText.setText( "Error: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }

    private void send(String str) {
        if(!connected) {
            return;
        }
        try {
            byte[] data = (str + '\n').getBytes();
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
        }
    }
    private void clearInputBuffer(int timeout) throws IOException {
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
    private String readData(int timeout) throws IOException {
        try {
            String result;
            do {
                int len = 1;
                int bufferSize = 10* usbSerialPort.getReadEndpoint().getMaxPacketSize();
                byte[] buffer = new byte[bufferSize+1];
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while (len != 0) {
                    len = usbSerialPort.read(buffer,bufferSize, timeout);
                    out.write(buffer, 0, len);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    result = out.toString(StandardCharsets.UTF_8); //only supported in SDK 33
                else
                    result = new String(out.toByteArray(), StandardCharsets.UTF_8); // for older android versions
            }
            while (result.isBlank()); // load again if the result of the first attempt is empty
            return result.split("\n")[0]; //only interested in the first line

        } catch (IOException e) {
            disconnect();
            throw new IOException(e);
        }
    }

    private void printDeviceId() throws Exception{

            device = getDeviceInfo();
            deviceIdText.setText(device.toString());
    }

    private Device getDeviceInfo() throws Exception {
        send("GET tubeSensitivity"); //new command
        String s = readData(1000);
        if (s.contains("ERROR")) {
            send("GET tubeConversionFactor"); //old command
            s = readData(1000);
        }

        s = s.replaceAll("OK ", "");
        s= s.replaceAll("[^0-9.\\-]", "");
        float conversionFactor = Float.parseFloat(s);
        send("GET deviceId");
        s = readData(1000);
        return new Device(s, conversionFactor);
    }

    private void checkTimeDiff(long maxDiff) throws Exception {
        send("GET deviceTime");
        String s = readData(1000);
        s = s.replaceAll("OK ", "");
        s= s.replaceAll("[^0-9.\\-]", "");
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        long deviceTime = Long.parseLong(s);
        long androidTime = Instant.now().getEpochSecond();
        long diff = deviceTime-androidTime; //Difference between time of android device and Geiger counter
        if (diff > maxDiff || diff < -1 * maxDiff) {
            String androidTimeString = Instant.ofEpochSecond(androidTime).atZone(zoneId).format(formatter);
            String deviceTimeString = Instant.ofEpochSecond(deviceTime).atZone(zoneId).format(formatter);
            getActivity().runOnUiThread(() -> showSyncConfirmationDialog(androidTimeString,deviceTimeString));
        }
        else
            enableButtons(true);
    }

    private void showSyncConfirmationDialog(String androidTime, String deviceTime) {
        alertDialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Time Mismatch Detected!")
                .setMessage("Clock on Geiger counter is not synchronized with the Android device.\n\n" +
                        "Android time: " + androidTime +
                        "\nDevice time: " + deviceTime +
                        "\n\nWould you like to update the time on the Geiger counter?")
                .setPositiveButton("Sync", (dialog, which) -> syncTime())
                .setNegativeButton("Dismiss ", (dialog, which) -> enableButtons(true))
                .setOnCancelListener(d -> enableButtons(true))
                .show();
    }

    //Set time of the Geiger counter to the time of the android device
    private void syncTime() {
        statusText.setText("Syncronizing time");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                send("SET deviceTime " + Instant.now().getEpochSecond());
                String response = readData(1000);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if(response.contains("OK"))
                        showSyncResultDialog();
                    else
                        Toast.makeText(requireContext(), "time synchronized failed!", Toast.LENGTH_SHORT).show();

                    enableButtons(true);
                    statusText.setText("connected!");
                });
            }
            catch (Exception e) {
                statusText.setText("Error: " + e.getMessage());
            }
        });
        executor.shutdown();
    }

    private void showSyncResultDialog() {
        ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
        alertDialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Time Sync Successful!")
                .setMessage("If the hour on the Geiger counter is incorrect, please check time zone.\n\n" +
                        "The time zone must be set manually on the Geiger counter.\n\n" +
                        "Correct time zone: UTC" + offset)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    //Load Data from Device
    private void readAndViewData(boolean save) {
        statusText.setText("Loading!\n(May take a while)");
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                send("GET datalog");
                String receivedData = readData(2000);
                DataList dataList = new DataList(receivedData,device);
                if (save && !dataList.isEmpty())
                    sharedViewModel.addDatalogWithEntries(dataList);
                new Handler(Looper.getMainLooper()).post(() -> switchFragment(dataList));
            }
            catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> { statusText.setText("Error: " + e.getMessage());});
            }
        });
        executor.shutdown();
    }


    private void switchFragment(DataList d) {
        sharedViewModel.setDataList(d);
        androidx.fragment.app.Fragment fragment = new PlotFragment();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null);
        transaction.commit();
    }

    private void enableButtons(boolean b) {
        viewBtn.setEnabled(b);
        loadBtn.setEnabled(b);
    }

    void resetViews() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss(); //quit dialog
        }
        statusText.setText("Not Connected\nConnecting...");
        deviceIdText.setText("???\n");
        enableButtons(false);
    }

}