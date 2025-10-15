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
import com.mayrthom.radprologviewer.database.datalog.Datalog;
import com.mayrthom.radprologviewer.database.datalog.DatalogWithTimestampsAndDevice;
import com.mayrthom.radprologviewer.ui.adapters.DatalogListAdapter;
import com.mayrthom.radprologviewer.viewModel.SharedViewModel;
import com.mayrthom.radprologviewer.database.DataList;
import com.mayrthom.radprologviewer.viewModel.SharedViewModelFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

public class StoredDatalogsListFragment extends androidx.fragment.app.Fragment {
    private DatalogListAdapter adapter;
    private TextView emptyText;
    private SharedViewModel sharedViewModel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        LinearLayout loadingContainer = view.findViewById(R.id.loadingContainer);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        SharedViewModelFactory factory = new SharedViewModelFactory(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        emptyText =  view.findViewById(R.id.emptyText);
        emptyText.setTextSize(18);

        // Set up Adapter
        adapter = new DatalogListAdapter();
        adapter.setOnDeleteClickListener(datalog -> showDeleteConfirmationDialog(datalog));
        adapter.setOnExportClickListener(richDatalog -> showExportConfirmationDialog(richDatalog));

        adapter.setOnItemClickListener(richDatalog -> {
            LoadingDialog dialog = new LoadingDialog(requireContext(),"Loading Data");
            sharedViewModel.getDataPointsForDatalog(richDatalog.datalog.datalogId).observe(getViewLifecycleOwner(), dataPoints -> {
                    DataList dataList = new DataList(dataPoints, richDatalog.device);
                    switchFragment(dataList);
                    dialog.dismiss();
                });
        });

        //Update List
        loadingContainer.setVisibility(View.VISIBLE);
        sharedViewModel.getDatalogWithTimestampsAndDevice().observe(getViewLifecycleOwner(), datalogWithTimestampsAndDevices -> {
            loadingContainer.setVisibility(View.GONE);
            if (datalogWithTimestampsAndDevices == null || datalogWithTimestampsAndDevices.isEmpty())
                emptyText.setVisibility(View.VISIBLE);
            else
                emptyText.setVisibility(View.INVISIBLE);
            adapter.update(datalogWithTimestampsAndDevices);
                });

        recyclerView.setAdapter(adapter);
        return view;
    }

    /* Navigating back to the last fragment */
    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle("Stored Datalogs");
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    /* Show confirmation dialog if the datalog should be really deleted */
    private void showDeleteConfirmationDialog(Datalog datalog) {
        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Confirm Delete")
                .setMessage("Do you really want to delete datalog?")
                .setPositiveButton("Delete", (dialog, which) ->
                {
                    LoadingDialog loadingDialog = new LoadingDialog(requireContext(), "Deleting Data");
                    Executors.newSingleThreadExecutor().execute(() -> {
                        sharedViewModel.deleteDatalogWithPoints(datalog);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Deleted!", Toast.LENGTH_SHORT).show();
                            loadingDialog.dismiss();
                        });
                    });

                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /* Show confirmation dialog if the datalog should be really exported as csv */
    private void showExportConfirmationDialog(DatalogWithTimestampsAndDevice richDatalog) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
        String fileName = "0x" + richDatalog.datalog.deviceId + "_" + Instant.ofEpochMilli(richDatalog.datalog.downloadDate).atZone(ZoneOffset.UTC).format(formatter);
        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Confirm Export")
                .setMessage("Export to:\n\"Downloads/\u200B" + fileName + ".csv\"?")
                .setPositiveButton("Export", (dialog, which) -> {
                    LoadingDialog loadingDialog = new LoadingDialog(requireContext(),"Exporting Data");
                    sharedViewModel.getDataPointsForDatalog(richDatalog.datalog.datalogId).observe(getViewLifecycleOwner(), dataPoints -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                boolean success = new DataList(dataPoints, richDatalog.device).exportCsv(requireContext(), fileName);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(getContext(), success?"Export successful!":"Export failed!", Toast.LENGTH_SHORT).show();
                                    loadingDialog.dismiss();
                                });
                        });
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void switchFragment(DataList dataList) {
        sharedViewModel.setDataList(dataList);
        androidx.fragment.app.Fragment fragment = new PlotFragment();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null);
        transaction.commit();
    }
}
