package com.mayrthom.radprologviewer.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mayrthom.radprologviewer.R;
import com.mayrthom.radprologviewer.DataList;
import com.mayrthom.radprologviewer.database.adapters.DeviceListAdapter;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.viewModel.SharedViewModel;
import com.mayrthom.radprologviewer.viewModel.SharedViewModelFactory;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executors;

public class StoredDevicesListFragment extends androidx.fragment.app.Fragment {
    private DeviceListAdapter adapter;
    private TextView emptyText;
    private SharedViewModel sharedViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SharedViewModelFactory factory = new SharedViewModelFactory(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        /* Show text if there are no datalogs stored */
        emptyText =  view.findViewById(R.id.emptyText);
        emptyText.setTextSize(18);

        adapter = new DeviceListAdapter(sharedViewModel, getViewLifecycleOwner());
        adapter.setOnItemClickListener(dataList -> switchFragment(dataList));
        adapter.setOnDeleteClickListener(device -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                getActivity().runOnUiThread(() -> showDeleteConfirmationDialog(device));
            });
        });
        adapter.setOnExportClickListener((dataList, device) -> showExportConfirmationDialog(dataList,device));

        //update Devicelist
        sharedViewModel.getAllDevices().observe(getViewLifecycleOwner(), deviceList -> {
            if (deviceList != null) {
                adapter.updateDevices(deviceList);
                if(deviceList.isEmpty())
                    emptyText.setVisibility(View.VISIBLE);
                else
                    emptyText.setVisibility(View.INVISIBLE);
            }
        });
        recyclerView.setAdapter(adapter);
        return view;
    }

    /* Navigating back to the last fragment */
    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle("Stored Devices");
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    /* Show confirmation dialog if the datalog should be really deleted */
    private void showDeleteConfirmationDialog(Device device) {
        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Confirm Delete")
                .setMessage("Really want to delete whole device?")
                .setPositiveButton("Delete", (dialog, which) ->
                {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        sharedViewModel.deleteDeviceWithDatalogs(device);
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /* Show confirmation dialog if the datalog should be really exported as csv */
    private void showExportConfirmationDialog(DataList dataList, Device device) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        String fileName = device.serialNumber + "_" + simpleDateFormat.format(System.currentTimeMillis());
        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Confirm Export")
                .setMessage("Export to:\n\"Downloads/\u200B" + fileName + ".csv\"?")
                .setPositiveButton("Export", (dialog, which) ->
                {
                    if(dataList.exportCsv(this.requireContext(), fileName))
                        Toast.makeText(getActivity(), "stored successful", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getActivity(), "Error!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void switchFragment(DataList d) {
        sharedViewModel.setDataList(d);
        androidx.fragment.app.Fragment fragment = new PlotFragment();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
