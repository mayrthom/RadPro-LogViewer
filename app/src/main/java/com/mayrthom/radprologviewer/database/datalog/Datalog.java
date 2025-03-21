package com.mayrthom.radprologviewer.database.datalog;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.mayrthom.radprologviewer.database.device.Device;

@Entity(foreignKeys = @ForeignKey(
        entity = Device.class,
        parentColumns = "deviceId",
        childColumns = "deviceId",
        onDelete = ForeignKey.CASCADE
))
public class Datalog {
    @PrimaryKey(autoGenerate = true)
    public long datalogId;
    @ColumnInfo(name = "deviceId", index = true)
    public final long deviceId;
    public final long downloadDate;


    public Datalog(long downloadDate, long deviceId) {
        this.downloadDate = downloadDate;
        this.deviceId = deviceId;
    }
}
