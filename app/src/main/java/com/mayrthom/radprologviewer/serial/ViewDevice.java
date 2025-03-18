package com.mayrthom.radprologviewer.serial;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentTransaction;

import com.mayrthom.radprologviewer.BuildConfig;
import com.mayrthom.radprologviewer.database.AppDatabase;
import com.mayrthom.radprologviewer.Datalog;
import com.mayrthom.radprologviewer.DeviceInfo;
import com.mayrthom.radprologviewer.Plot;
import com.mayrthom.radprologviewer.R;
import com.mayrthom.radprologviewer.SharedViewModel;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewDevice extends Fragment {

    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    private static final int BAUD_RATE = 115200;
    private int deviceId, portNum;
    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;
    private TextView deviceIdText, statusText;
    private View viewBtn, loadBtn;
    private DeviceInfo deviceInfo;
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;
    private AppDatabase db;

    public ViewDevice() {
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
        //setRetainInstance(true);
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
        if(!connected && (usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted))
            mainLooper.post(this::connect);
    }

    @Override
    public void onPause() {
        if(connected) {
            disconnect();
        }
        super.onPause();
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_device, container, false);
        db = AppDatabase.getInstance(requireContext());
        deviceIdText = view.findViewById(R.id.device_text);
        statusText = view.findViewById(R.id.status_text);

        viewBtn = view.findViewById(R.id.view_data_btn);
        loadBtn = view.findViewById(R.id.load_data_btn);

        enableButtons(false);

        viewBtn.setOnClickListener(v ->{
                enableButtons(false);
                viewData(false);
        });

        loadBtn.setOnClickListener(v ->{
            enableButtons(false);
            viewData(true);
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
            }catch (UnsupportedOperationException e){
                statusText.setText(e.getMessage());
            }
            statusText.setText("connected");
            enableButtons(true);

            connected = true;
            printDeviceId();
            //controlLines.start();
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
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = (str + '\n').getBytes();
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
        }
    }

    private String readData() throws IOException {
        try {
            int len=1;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (len != 0){
                byte[] buffer = new byte[8192];
                len = usbSerialPort.read(buffer, READ_WAIT_MILLIS);
                out.write(buffer, 0, len);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return out.toString(StandardCharsets.UTF_8); //only supported in SDK 33
            }
            else
            {
                return new String(out.toByteArray(),StandardCharsets.UTF_8); // for older android versions
            }
        } catch (IOException e) {
            // when using read with timeout, USB bulkTransfer returns -1 on timeout _and_ errors
            // like connection loss, so there is typically no exception thrown here on error
            disconnect();
            throw new IOException(e);
        }
    }

    DeviceInfo getDeviceInfo() throws Exception {
        send("GET deviceId");
        String s = readData();
        return new DeviceInfo(s);
    }

    void printDeviceId()
    {
        try {
            deviceInfo = getDeviceInfo();
            statusText.setText("Connected");
            deviceIdText.setText(deviceInfo.toString());
        }
        catch (Exception e)
        {
            enableButtons(false);
            statusText.setText("Error: " + e.getMessage());
        }
    }
    void viewData(boolean save)
    {
        statusText.setText("Loading!\n(May take a while)");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                    send("GET tubeConversionFactor");
                    String s = readData();
                    s = s.replace("OK ", "");
                    float conversionFactor = Float.parseFloat(s);

                    send("GET datalog");
                    s = readData();

                    Datalog d = new Datalog(System.currentTimeMillis(), s, deviceInfo.toString(), conversionFactor);

                    if (save && !d.getEntrySet().isEmpty())
                        db.datalogDao().insert(d);
                    new Handler(Looper.getMainLooper()).post(() -> switchFragment(d));
                }
                catch (Exception e)
                    {
                        statusText.setText("Error: " + e.getMessage());
                    }
            });
        executor.shutdown();
    }

    private void switchFragment(Datalog d) {
        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.setDatalog(d);
        Fragment fragment = new Plot();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void enableButtons(boolean b)
    {
        viewBtn.setEnabled(b);
        loadBtn.setEnabled(b);
    }
}