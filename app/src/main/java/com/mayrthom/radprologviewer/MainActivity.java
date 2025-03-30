package com.mayrthom.radprologviewer;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.ui.StoredDatalogsListFragment;
import com.mayrthom.radprologviewer.ui.USBDevicesFragment;
import com.google.android.material.navigation.NavigationView;
import com.mayrthom.radprologviewer.ui.StoredDevicesListFragment;
import com.mayrthom.radprologviewer.viewModel.SharedViewModel;
import com.mayrthom.radprologviewer.viewModel.SharedViewModelFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private SharedViewModel sharedViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedViewModel = new ViewModelProvider(this, new SharedViewModelFactory(this)).get(SharedViewModel.class);

        // Register file picker
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        importCsv(uri); //Now reading from the csv and add the list to the database
                    }
                }
        );

        drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        FragmentManager fragmentManager = getSupportFragmentManager();
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_devices) {
                fragmentManager.beginTransaction().replace(R.id.fragment_container, new USBDevicesFragment()).addToBackStack(null).commit();
                drawerLayout.closeDrawers();
            }

            else if (item.getItemId() == R.id.nav_dat_list) {
                fragmentManager.beginTransaction().replace(R.id.fragment_container, new StoredDatalogsListFragment()).addToBackStack(null).commit();
                drawerLayout.closeDrawers();
            }

            else if (item.getItemId() == R.id.nav_dev_list) {
                fragmentManager.beginTransaction().replace(R.id.fragment_container, new StoredDevicesListFragment()).addToBackStack(null).commit();
                drawerLayout.closeDrawers();
            }

            else if (item.getItemId() == R.id.nav_import) { //Selecting csv file for import
                String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/vnd.ms-excel"};
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes).addCategory(Intent.CATEGORY_OPENABLE);
                filePickerLauncher.launch(Intent.createChooser(intent, "Choose CSV"));
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            else if (item.getItemId() == R.id.nav_info) { //show info window
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                View dialogView = inflater.inflate(R.layout.info_layout, null);
                new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogStyle)
                        .setView(dialogView)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
            }
            drawerLayout.closeDrawers();

            return true;
        });

        navigationView.getMenu().performIdentifierAction(R.id.nav_devices,0);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
    }
    void importCsv(Uri uri)
    {
        if (uri != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
                    Device device = new Gson().fromJson(reader.readLine(), new TypeToken<Device>() {}.getType()); //first line is device object stored as json
                    if (device == null || device.deviceId == 0 || device.deviceType == null || device.conversionValue == 0)
                        throw new IOException("wrong Input format!");
                    if (!reader.readLine().contains("time,radiation[CPS]"))//second line is csv title which ust be the following: time,radiation[CPS
                        throw new IOException("wrong Input format!");
                    List<DataPoint> l = reader.lines().map(s -> {
                        String[] strings = s.split(",");
                        return new DataPoint(0, Float.parseFloat(strings[1]), Long.parseLong(strings[0]));
                    }).collect(Collectors.toList());
                    reader.close();
                    sharedViewModel.addDatalogWithEntries(new DataList(l, device));
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, "File imported successful!", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, "File import failed!", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }
}
