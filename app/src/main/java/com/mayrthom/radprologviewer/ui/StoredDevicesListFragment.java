package com.mayrthom.radprologviewer.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.mayrthom.radprologviewer.database.DataList;
import com.mayrthom.radprologviewer.ui.adapters.DeviceListAdapter;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.viewModel.SharedViewModel;
import com.mayrthom.radprologviewer.viewModel.SharedViewModelFactory;

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
        LinearLayout loadingContainer = view.findViewById(R.id.loadingContainer);

        SharedViewModelFactory factory = new SharedViewModelFactory(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        /* Show text if there are no datalogs stored */
        emptyText =  view.findViewById(R.id.emptyText);
        emptyText.setTextSize(18);

        adapter = new DeviceListAdapter();
        adapter.setOnItemClickListener(device -> {
            LoadingDialog dialog = new LoadingDialog(requireContext(),"Loading Data");
            sharedViewModel.loadDataPointsForDevice(device.deviceId).observe(getViewLifecycleOwner(), dataPoints -> {
                DataList dataList = new DataList(dataPoints, device);
                switchFragment(dataList);
                dialog.dismiss();
            });
        });

        adapter.setOnDeleteClickListener(device -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                getActivity().runOnUiThread(() -> showDeleteConfirmationDialog(device));
            });
        });
        adapter.setOnExportClickListener(device -> showExportConfirmationDialog(device));

        //update List
        loadingContainer.setVisibility(View.VISIBLE);
        sharedViewModel.getAllDevices().observe(getViewLifecycleOwner(), devices -> {
            loadingContainer.setVisibility(View.GONE);
            if (devices == null || devices.isEmpty())
                emptyText.setVisibility(View.VISIBLE);
            else
                emptyText.setVisibility(View.INVISIBLE);
            adapter.update(devices);
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
                .setPositiveButton("Delete", (dialog, which) -> {
                    LoadingDialog loadingDialog = new LoadingDialog(requireContext(),"Deleting Data");
                    Executors.newSingleThreadExecutor().execute(() -> {
                        sharedViewModel.deleteDeviceWithDatalogs(device);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Deleted!", Toast.LENGTH_SHORT).show();
                            loadingDialog.dismiss();
                        });
                    });
                }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /* Show confirmation dialog if the datalog should be really exported as csv */
    private void showExportConfirmationDialog(Device device) {
        String fileName = device.deviceType + "_0x" + Long.toHexString(device.deviceId);
        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Confirm Export")
                .setMessage("Export to:\n\"Downloads/\u200B" + fileName + ".csv\"?")
                .setPositiveButton("Export", (dialog, which) -> {
                    LoadingDialog loadingDialog = new LoadingDialog(requireContext(),"Exporting Data");
                    sharedViewModel.loadDataPointsForDevice(device.deviceId).observe(getViewLifecycleOwner(), dataPoints -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            boolean success = new DataList(dataPoints, device).exportCsv(requireContext(), fileName);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if(success)
                                    Toast.makeText(getContext(), "Export successful!", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(getContext(), "Export failed!", Toast.LENGTH_SHORT).show();
                                loadingDialog.dismiss();
                            });
                        });
                    });
                }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void switchFragment(DataList dataList) {
        sharedViewModel.setDataList(dataList);
        androidx.fragment.app.Fragment fragment = new PlotFragment();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null);
        transaction.commit();
    }
}
