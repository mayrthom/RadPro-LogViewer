package com.mayrthom.radprologviewer.database.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mayrthom.radprologviewer.R;
import com.mayrthom.radprologviewer.DataList;
import com.mayrthom.radprologviewer.database.device.Device;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {
    private final List<DataList> deviceList;
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;
    private OnExportClickListener exportListener;

    public DeviceListAdapter() {
        this.deviceList = new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(DataList dataList);
    }
    public interface OnDeleteClickListener {
        void onDeleteClick(Device device);
    }
    public interface OnExportClickListener {
        void onExportClick(DataList dataList);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stored_devices_list_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            DataList dataList = deviceList.get(position);
            Device device = dataList.getDevice();

            //format the time according to the timezone on the android device
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            final ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime zonedStartTime = Instant.ofEpochSecond(dataList.getStartPoint()).atZone(zoneId);
            ZonedDateTime zonedEndTime = Instant.ofEpochSecond(dataList.getEndPoint()).atZone(zoneId);

            //set text for item
            holder.textViewDateRange.setText("Date Range: " + zonedStartTime.format(formatter) + " - " + zonedEndTime.format(formatter));
            holder.textViewDeviceValue.setText(device.toString());

            //setup listeners
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(dataList);
                });
            holder.buttonDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDeleteClick(device);
                });
            holder.buttonExport.setOnClickListener(v -> {
                if (exportListener != null) exportListener.onExportClick(dataList);
              });

    }

    @Override
    public int getItemCount() {
        return deviceList.size();
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
    public void update(List<DataList> newList) {
        this.deviceList.clear();
        this.deviceList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        public final View buttonDelete, buttonExport;
        final TextView textViewDateRange, textViewDeviceValue;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDateRange = itemView.findViewById(R.id.textViewDateRange);
            textViewDeviceValue = itemView.findViewById(R.id.textViewDeviceValue);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonExport = itemView.findViewById(R.id.buttonExport);
        }
    }
}
