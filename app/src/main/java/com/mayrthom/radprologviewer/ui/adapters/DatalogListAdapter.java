package com.mayrthom.radprologviewer.ui.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mayrthom.radprologviewer.database.datalog.DatalogWithTimestampsAndDevice;
import com.mayrthom.radprologviewer.database.datalog.Datalog;
import com.mayrthom.radprologviewer.R;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DatalogListAdapter extends RecyclerView.Adapter<DatalogListAdapter.DatalogViewHolder> {
    private final List<DatalogWithTimestampsAndDevice> datalogList;
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;
    private OnExportClickListener exportListener;

    public DatalogListAdapter() {
        datalogList = new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(DatalogWithTimestampsAndDevice richDatalog);
    }
    public interface OnDeleteClickListener {
        void onDeleteClick(Datalog datalog);
    }
    public interface OnExportClickListener {
        void onExportClick(DatalogWithTimestampsAndDevice datalogWithTimestampsAndDevice);
    }

    @NonNull
    @Override
    public DatalogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.datalog_list_item, parent, false);
        return new DatalogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DatalogViewHolder holder, int position) {
        DatalogWithTimestampsAndDevice richDatalog = datalogList.get(position); //get datalog

        if (richDatalog == null) return;


        //format the time according to the timezone on the android device
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        final ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zonedStartTime = Instant.ofEpochSecond(richDatalog.minTimestamp).atZone(zoneId);
        ZonedDateTime zonedEndTime = Instant.ofEpochSecond(richDatalog.maxTimestamp).atZone(zoneId);
        ZonedDateTime zonedDownloadDate =Instant.ofEpochMilli(richDatalog.datalog.downloadDate).atZone(zoneId);

        //set text for item
        holder.textViewDownloadDate.setText("Download Date: " + zonedDownloadDate.format(formatter));
        holder.textViewDateRange.setText("Date Range: " + zonedStartTime.format(formatter) + " - " + zonedEndTime.format(formatter));
        holder.textViewModelName.setText(richDatalog.device.toString());

        //setup listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(richDatalog);
            });
        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClick(richDatalog.datalog);
            });
        holder.buttonExport.setOnClickListener(v -> {
            if (exportListener != null) exportListener.onExportClick(richDatalog);
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
    public void update(List<DatalogWithTimestampsAndDevice> richDatalog) {
        this.datalogList.clear();
        this.datalogList.addAll(richDatalog);
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
