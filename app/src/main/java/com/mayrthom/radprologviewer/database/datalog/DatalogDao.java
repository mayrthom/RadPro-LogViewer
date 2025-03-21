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

    @Query("SELECT * FROM Datalog")
    LiveData<List<Datalog>> getAllDatalogs();

    @Query("DELETE FROM Datalog WHERE datalogId = :datalogId")
    void deleteDatalogById(long datalogId);

    @Query("SELECT COUNT(*) FROM Datalog WHERE deviceId = :deviceId")
    int getDatalogCountForDevice(long deviceId);
}