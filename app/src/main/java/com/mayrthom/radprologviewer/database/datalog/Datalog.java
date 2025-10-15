package com.mayrthom.radprologviewer.database.datalog;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.mayrthom.radprologviewer.database.device.Device;

@Entity(foreignKeys = @ForeignKey(
        entity = Device.class,
        parentColumns = "deviceId",
        childColumns = "datalog_device_id",
        onDelete = ForeignKey.CASCADE
))
public class Datalog {
    @PrimaryKey(autoGenerate = true)
    public long datalogId;

    @ColumnInfo(name = "datalog_device_id", index = true)
    public final String deviceId;
    public final long downloadDate;

    public Datalog(long downloadDate, String deviceId) {
        this.downloadDate = downloadDate;
        this.deviceId = deviceId;
    }
}
