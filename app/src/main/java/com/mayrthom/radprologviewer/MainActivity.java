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
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.mayrthom.radprologviewer.ui.StoredDatalogsListFragment;
import com.mayrthom.radprologviewer.ui.USBDevicesFragment;
import com.google.android.material.navigation.NavigationView;
import com.mayrthom.radprologviewer.ui.StoredDevicesListFragment;
import com.mayrthom.radprologviewer.utils.FileImporter;
import com.mayrthom.radprologviewer.viewModel.SharedViewModel;
import com.mayrthom.radprologviewer.viewModel.SharedViewModelFactory;


public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private SharedViewModel sharedViewModel;
    LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedViewModel = new ViewModelProvider(this, new SharedViewModelFactory(this)).get(SharedViewModel.class);
        inflater = LayoutInflater.from(MainActivity.this);


        // Register file picker
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),

                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        FileImporter.importCsv(MainActivity.this, uri, sharedViewModel);
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
                View dialogView = inflater.inflate(R.layout.info_notice, null);
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
}
