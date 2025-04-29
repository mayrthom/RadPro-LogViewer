package com.mayrthom.radprologviewer.database.dataPoint;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.mayrthom.radprologviewer.database.datalog.Datalog;

@Entity(foreignKeys = @ForeignKey(entity = Datalog.class,
        parentColumns = "datalogId",
        childColumns = "datalogId",
        onDelete = ForeignKey.CASCADE
))
public class DataPoint {
    @PrimaryKey(autoGenerate = true)
    public long dataPointId;

    @ColumnInfo(index = true)
    public long datalogId;
    public final float radiationLevel;
    @ColumnInfo(index = true)
    public final long timestamp;

    public DataPoint(long datalogId, float radiationLevel, long timestamp) {
        this.datalogId = datalogId;
        this.radiationLevel = radiationLevel;
        this.timestamp = timestamp;
    }
}