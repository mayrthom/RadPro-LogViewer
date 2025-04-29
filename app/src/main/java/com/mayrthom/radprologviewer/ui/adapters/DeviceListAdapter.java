package com.mayrthom.radprologviewer.ui.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mayrthom.radprologviewer.R;
import com.mayrthom.radprologviewer.database.device.Device;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private final List<Device> deviceList;
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;
    private OnExportClickListener exportListener;

    public DeviceListAdapter() {
        this.deviceList = new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(Device device);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Device device);
    }

    public interface OnExportClickListener {
        void onExportClick(Device device);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stored_devices_list_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);

        holder.textViewDeviceValue.setText(device.toString());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(device);
        });
        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClick(device);
        });
        holder.buttonExport.setOnClickListener(v -> {
            if (exportListener != null) exportListener.onExportClick(device);
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
    public void update(List<Device> newList) {
        this.deviceList.clear();
        this.deviceList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        public final View buttonDelete, buttonExport;
        final TextView textViewDeviceValue;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDeviceValue = itemView.findViewById(R.id.textViewDeviceValue);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonExport = itemView.findViewById(R.id.buttonExport);
        }
    }
}
