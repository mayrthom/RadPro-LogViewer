package com.mayrthom.radprologviewer.dataloglist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mayrthom.radprologviewer.Datalog;
import com.mayrthom.radprologviewer.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatalogListAdapter extends RecyclerView.Adapter<DatalogListAdapter.DatalogViewHolder> {
    private List<Datalog> datalogList;
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;
    private OnExportClickListener exportListener;
    public DatalogListAdapter() {
        this.datalogList = new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(Datalog datalog);
    }
    public interface OnDeleteClickListener {
        void onDeleteClick(Datalog datalog);
    }
    public interface OnExportClickListener {
        void onExportClick(Datalog datalog);
    }

    @NonNull
    @Override
    public DatalogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.datalog_list_item, parent, false);
        return new DatalogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DatalogViewHolder holder, int position) {
        Datalog datalog = datalogList.get(position);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm", Locale.US);

        holder.textViewStartValue.setText("Date Range: " + dateFormat.format(new Date(datalog.getStartPoint()*1000)) + " - " + dateFormat.format(new Date( datalog.getEndPoint() * 1000 )));
        holder.textViewDownloadDate.setText("Download Date: " + dateFormat.format(datalog.getDownloadDate()));
        holder.textViewEntryCount.setText(datalog.getDeviceInfo());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(datalog);
            }
        });
        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(datalog);
            }
        });
        holder.buttonExport.setOnClickListener(v -> {
            if (exportListener != null) {
                exportListener.onExportClick(datalog);
            }
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
    
    public void setData(List<Datalog> newList) {
        this.datalogList = newList;
        notifyDataSetChanged();
    }

    public static class DatalogViewHolder extends RecyclerView.ViewHolder {
        public View buttonDelete, buttonExport;
        TextView textViewStartValue, textViewDownloadDate, textViewEntryCount;

        public DatalogViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewStartValue = itemView.findViewById(R.id.textViewStartValue);
            textViewDownloadDate = itemView.findViewById(R.id.textViewDownloadDate);
            textViewEntryCount = itemView.findViewById(R.id.textViewEntryCount);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonExport = itemView.findViewById(R.id.buttonExport);
        }
    }
}
