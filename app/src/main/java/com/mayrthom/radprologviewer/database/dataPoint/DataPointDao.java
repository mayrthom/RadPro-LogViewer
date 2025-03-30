package com.mayrthom.radprologviewer.database.dataPoint;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DataPointDao {
    @Insert
    void insertAll(List<DataPoint> points);

    @Query("SELECT * FROM DataPoint WHERE datalogId = :datalogId")
    LiveData<List<DataPoint>> getDataPointsForDatalog(long datalogId);

    @Query("SELECT DISTINCT dp.* FROM DataPoint dp JOIN Datalog dl ON dp.datalogId = dl.datalogId WHERE dl.deviceId = :deviceId ORDER BY dp.timestamp ASC")
        LiveData<List<DataPoint>> getDataPointsForDevice(long deviceId);
}
