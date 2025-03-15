package com.mayrthom.radprologviewer;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.mayrthom.radprologviewer.dataloglist.DatalogListFragment;
import com.mayrthom.radprologviewer.serial.DevicesFragment;
import com.google.android.material.navigation.NavigationView;


public class MainActivity extends AppCompatActivity {
    // Declare the DrawerLayout, NavigationView, and Toolbar
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                fragmentManager.beginTransaction().replace(R.id.fragment_container, new DevicesFragment()).addToBackStack(null).commit();
                drawerLayout.closeDrawers();
            }

            else if (item.getItemId() == R.id.nav_dat_list) {
                fragmentManager.beginTransaction().replace(R.id.fragment_container, new DatalogListFragment()).addToBackStack(null).commit();
                drawerLayout.closeDrawers();
            }
            else if (item.getItemId() == R.id.nav_info) {
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

}
