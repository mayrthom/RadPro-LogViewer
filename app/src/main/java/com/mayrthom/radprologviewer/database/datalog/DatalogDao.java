package com.mayrthom.radprologviewer.database.datalog;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;
@Dao
public interface DatalogDao {
    @Insert
    long insertDatalog(Datalog datalog);

    @Query("DELETE FROM Datalog WHERE datalogId = :datalogId")
    void deleteDatalogById(long datalogId);

    @Query("SELECT COUNT(*) FROM Datalog WHERE datalog_device_id = :deviceId")
    int getDatalogCountForDevice(long deviceId);

    @Query("SELECT dl.datalogId, " +
            "dl.datalog_device_id, "+
            "dl.downloadDate, " +
            "d.deviceId, " +
            "d.deviceType, " +
            "d.conversionValue, " +
            "MIN(dp.timestamp) AS minTimestamp, " +
            "MAX(dp.timestamp) AS maxTimestamp " +
            "FROM Datalog dl  JOIN Device d ON dl.datalog_device_id = d.deviceId LEFT JOIN DataPoint dp ON dp.datalogId = dl.datalogId " +
            "GROUP BY dl.datalogId, d.deviceId")
    LiveData<List<DatalogWithTimestampsAndDevice>> getDatalogWithTimestampsAndDevice();
}