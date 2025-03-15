package com.mayrthom.radprologviewer.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.mayrthom.radprologviewer.Datalog;

import java.util.List;

@Dao
public interface DatalogDao {
    @Insert
    void insert(Datalog dataLog);

    @Delete
    void delete(Datalog datalog);

    @Query("SELECT * FROM Datalog")
    List<Datalog> getAllLogs();

}