package com.mayrthom.radprologviewer.dataloglist;

import android.app.AlertDialog;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mayrthom.radprologviewer.database.AppDatabase;
import com.mayrthom.radprologviewer.Datalog;
import com.mayrthom.radprologviewer.Plot;
import com.mayrthom.radprologviewer.R;
import com.mayrthom.radprologviewer.SharedViewModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DatalogListFragment extends Fragment {
    private DatalogListAdapter adapter;
    private AppDatabase db;
    private TextView emptyText;
    private SharedViewModel sharedViewModel;

    public DatalogListFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_datalog, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        /* Show text if there are no datalogs stored */
        emptyText =  view.findViewById(R.id.emptyText);
        emptyText.setTextSize(18);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        db = AppDatabase.getInstance(getContext());
        adapter = new DatalogListAdapter();
        adapter.setOnItemClickListener(datalog -> switchFragment(datalog));
        adapter.setOnDeleteClickListener(datalog -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                getActivity().runOnUiThread(() -> showDeleteConfirmationDialog(datalog));
            });
        });
        adapter.setOnExportClickListener(datalog -> showExportConfirmationDialog(datalog));
        recyclerView.setAdapter(adapter);
        loadData();
        return view;
    }

    /* Navigating back to the last fragment */
    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle("Datalogs List");
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getParentFragmentManager().popBackStack();
            }
        });
    }
    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Datalog> logs = db.datalogDao().getAllLogs();
            getActivity().runOnUiThread(() -> adapter.setData(logs));
            new Handler(Looper.getMainLooper()).post(() -> {
                if(logs.isEmpty())
                    emptyText.setVisibility(View.VISIBLE);
                else
                    emptyText.setVisibility(View.INVISIBLE);
            });

        });
    }

    /* Show confirmation dialog if the datalog should be really deleted */
    private void showDeleteConfirmationDialog(Datalog datalog) {
        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Confirm Delete")
                .setMessage("Really want to delete datalog?")
                .setPositiveButton("Delete", (dialog, which) ->
                {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.datalogDao().delete(datalog);
                        new Handler(Looper.getMainLooper()).post(() -> loadData());
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /* Show confirmation dialog if the datalog should be really exported as csv */
    private void showExportConfirmationDialog(Datalog datalog) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        String fileName = "datalog_" + simpleDateFormat.format(datalog.getDownloadDate());
        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle("Confirm Export")
                .setMessage("Export to:\n\"Downloads/\u200B" + fileName + ".csv\"?")
                .setPositiveButton("Export", (dialog, which) ->
                {
                    if(datalog.exportCsv(this.requireContext(), fileName))
                    {
                        Toast.makeText(getActivity(), "stored successful", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        Toast.makeText(getActivity(), "Error!", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void switchFragment(Datalog d) {
        sharedViewModel.setDatalog(d);
        Fragment fragment = new Plot();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
