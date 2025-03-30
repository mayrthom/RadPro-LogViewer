package com.mayrthom.radprologviewer.database.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.DataList;
import com.mayrthom.radprologviewer.database.datalog.Datalog;
import com.mayrthom.radprologviewer.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatalogListAdapter extends RecyclerView.Adapter<DatalogListAdapter.DatalogViewHolder> {
    private final List<DataList> datalogList;
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;
    private OnExportClickListener exportListener;

    public DatalogListAdapter() {
        datalogList = new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(DataList dataList);
    }
    public interface OnDeleteClickListener {
        void onDeleteClick(DataList dataList);
    }
    public interface OnExportClickListener {
        void onExportClick(DataList dataList);
    }

    @NonNull
    @Override
    public DatalogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.datalog_list_item, parent, false);
        return new DatalogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DatalogViewHolder holder, int position) {
        DataList dataList = datalogList.get(position); //get datalog
        if (dataList == null) return;
        Datalog datalog = dataList.getDatalog();
        Device device = dataList.getDevice();
        //set text for item
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm", Locale.US);
        holder.textViewDownloadDate.setText("Download Date: " + dateFormat.format(datalog.downloadDate));
        holder.textViewDateRange.setText("Date Range: " + dateFormat.format(new Date(dataList.getStartPoint() * 1000)) + " - " + dateFormat.format(new Date(dataList.getEndPoint() * 1000)));
        holder.textViewModelName.setText(device.toString());

        //setup listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(dataList);
            });
        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClick(dataList);
            });
        holder.buttonExport.setOnClickListener(v -> {
            if (exportListener != null) exportListener.onExportClick(dataList);
          });
    }

    @Override
    public int getItemCount() {
        return datalogList.size();
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }
    public void setOnExportClickListener(OnExportClickListener listener) {
        this.exportListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void update(List<DataList> dataLists) {
        this.datalogList.clear();
        this.datalogList.addAll(dataLists);
        notifyDataSetChanged();
    }

    public static class DatalogViewHolder extends RecyclerView.ViewHolder {
        public final View buttonDelete, buttonExport;
        final TextView textViewDateRange, textViewDownloadDate, textViewModelName;

        public DatalogViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDateRange = itemView.findViewById(R.id.textViewDateRange);
            textViewDownloadDate = itemView.findViewById(R.id.textViewDownloadDate);
            textViewModelName = itemView.findViewById(R.id.textViewModelName);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonExport = itemView.findViewById(R.id.buttonExport);
        }
    }
}
