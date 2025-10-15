package com.mayrthom.radprologviewer.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.mayrthom.radprologviewer.BuildConfig;
import com.mayrthom.radprologviewer.database.DataList;
import com.mayrthom.radprologviewer.R;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.serialPort.SerialPortManager;
import com.mayrthom.radprologviewer.viewModel.SharedViewModel;
import com.mayrthom.radprologviewer.viewModel.SharedViewModelFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceInfoFragment extends Fragment {

    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";

    private int deviceId, portNum;
    private UsbPermission usbPermission = UsbPermission.Unknown;

    private SharedViewModel sharedViewModel;
    private SerialPortManager serialPortManager;

    private TextView deviceIdText, statusText;
    private View viewBtn, viewResetBtn;
    private ProgressBar progressBar;
    private Device deviceInfo;

    private AlertDialog alertDialog;
    private final Handler mainLooper;
    private final BroadcastReceiver usbPermissionReceiver;

    public DeviceInfoFragment() {
        usbPermissionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
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
        ContextCompat.registerReceiver(requireActivity(), usbPermissionReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        requireContext().unregisterReceiver(usbPermissionReceiver);
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
        if(serialPortManager == null || !serialPortManager.connected && (usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted)) {
            resetViews();
            mainLooper.post(this::connect);
        }
    }

    @Override
    public void onPause() {
        disconnect();
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
        viewResetBtn = view.findViewById(R.id.load_data_btn);
        progressBar = view.findViewById(R.id.progressBar);

        enableButtons(false);

        viewBtn.setOnClickListener(v -> {
            readAndViewData(false);
        });

        viewResetBtn.setOnClickListener(v -> {
            resetAndRead();
        });
        return view;
    }

    private void connect() {
        try {
            resetViews();
            serialPortManager = new SerialPortManager();

            boolean success = serialPortManager.connect(requireContext(), deviceId, portNum);

            if (!success) return;

            serialPortManager.clearInputBuffer(1000);
            deviceInfo = getDevice();
            deviceIdText.setText(deviceInfo.toString());
            statusText.setText("Connected!");
            progressBar.setVisibility(View.GONE);
            checkTimeDiff(61);

        } catch (Exception e) {
            statusText.setText("Connection failed: " + e.getMessage());
        }
    }

    private void disconnect() {
        if (serialPortManager != null) serialPortManager.disconnect();
        serialPortManager = null;
    }

    private Device getDevice() throws Exception {
        String s = serialPortManager.sendAndRead("GET tubeSensitivity",1000);
        float factor = Float.parseFloat(s.replace("OK ", "").trim());
        String id = serialPortManager.sendAndRead("GET deviceId",1000).replace("OK ", "").trim();
        return new Device(id, factor);
    }

    private void checkTimeDiff(long maxDiff) throws Exception {
        ZoneId deviceZoneId, androidZoneId = ZoneId.systemDefault();
        ZoneOffset deviceOffset, androidOffset = androidZoneId.getRules().getOffset(Instant.now());

        String s = serialPortManager.sendAndRead("GET deviceTime",1000);
        long deviceTime = Long.parseLong(s.replaceAll("OK |\\r|\\n|\\s", ""));
        long androidTime = Instant.now().getEpochSecond();

        s = serialPortManager.sendAndRead("GET deviceTimeZone",1000);
        if(! s.contains("ERROR")) {
            int offset = (int) Float.parseFloat(s.replaceAll("OK |\\r|\\n|\\s", ""));
            deviceOffset = ZoneOffset.ofHours(offset);
            deviceZoneId = ZoneId.ofOffset("UTC",deviceOffset);
        }
        else {
            deviceOffset = androidOffset;
            deviceZoneId = androidZoneId;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        long diff = deviceTime-androidTime; //Difference between time of android device and Geiger counter
        if (diff > maxDiff || diff < -1 * maxDiff || deviceOffset != androidOffset) {
            String androidTimeString = Instant.ofEpochSecond(androidTime).atZone(androidZoneId).format(formatter);
            String deviceTimeString = Instant.ofEpochSecond(deviceTime).atZone(deviceZoneId).format(formatter);
            getActivity().runOnUiThread(() -> showTimeMismatchInfo(androidTimeString,deviceTimeString));
        }
        else
            enableButtons(true);
    }

    private void showTimeMismatchInfo(String androidTime, String deviceTime) {
        alertDialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Time Mismatch")
                .setMessage("Android: " + androidTime + "\nDevice: " + deviceTime + "\n\nSync time?")
                .setPositiveButton("Sync", (dialog, which) -> syncTime())
                .setNegativeButton("Cancel", (dialog, which) -> enableButtons(true))
                .show();
    }

    //Set time of the Geiger counter to the time of the android device
    private void syncTime() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Syncronizing time");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String response = serialPortManager.sendAndRead("SET deviceTime " + Instant.now().getEpochSecond(),1000);
                if(response.contains("OK"))
                {
                    int offset = ZoneId.systemDefault().getRules().getOffset(Instant.now()).getTotalSeconds()/3600;
                    response = serialPortManager.sendAndRead("SET deviceTimeZone " + offset,1000);
                    boolean success = response.contains("OK");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        showTimeSyncResultInfo(success);
                        progressBar.setVisibility(View.GONE);
                        statusText.setText("connected!");
                        enableButtons(true);
                    });
                }
                else
                    new Handler(Looper.getMainLooper()).post(() -> statusText.setText("time synchronized failed!"));
            }
            catch (Exception e) {
                statusText.setText("Error: " + e.getMessage());
            }
        });
        executor.shutdown();
    }

    private void showTimeSyncResultInfo(boolean zoneSynced) {
        String message1 = "The time has been synchronized successful.";
        String message2 = "If the hour on the Geiger counter is incorrect, please check time zone.\n\n" +
                "The time zone must be set manually on the Geiger counter.\n\n" +
                "Correct time zone: UTC" + ZoneId.systemDefault().getRules().getOffset(Instant.now());

            alertDialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                    .setTitle("Time Sync Successful!")
                    .setMessage(zoneSynced?message1:message2)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();

    }

    private void resetAndRead() {
        alertDialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Reset Datalog?")
                .setMessage("Really want to reset the datalog after loading?")
                .setPositiveButton("OK", (dialog, which) -> readAndViewData(true))
                .setNegativeButton("Cancle", (dialog, which) -> dialog.dismiss())
                .show();
    }


        private void readAndViewData(boolean reset) {
        enableButtons(false);
        statusText.setText("Loading...\n(May take a while)");
        progressBar.setVisibility(View.VISIBLE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //keep window from locking

            executor.execute(() -> {
            try {
                serialPortManager.send("GET datalog");
                String data = serialPortManager.read(2500);
                DataList dataList = new DataList(data, deviceInfo);
                if(reset)
                    serialPortManager.send("RESET datalog");
                mainLooper.post(() -> switchFragment(dataList));
            } catch (Exception e) {
                progressBar.setVisibility(View.GONE);
                mainLooper.post(() ->{
                    statusText.setText("Read error: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
        executor.shutdown();
    }

    private void switchFragment(DataList d) {
        PlotFragment fragment = new PlotFragment();
        Bundle args = new Bundle();
        args.putBoolean("store", true);
        fragment.setArguments(args);

        sharedViewModel.setDataList(d);
        FragmentTransaction tx = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null);
        tx.commit();
    }

    private void enableButtons(boolean b) {
        viewBtn.setEnabled(b);
        viewResetBtn.setEnabled(b);
    }

    private void resetViews() {
        if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Not connected\nConnecting...");
        deviceIdText.setText("???\n");
        enableButtons(false);
    }
}
