package com.mayrthom.radprologviewer.database;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;
import com.mayrthom.radprologviewer.database.dataPoint.DataPointDao;
import com.mayrthom.radprologviewer.database.datalog.Datalog;
import com.mayrthom.radprologviewer.database.datalog.DatalogDao;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.database.device.DeviceDao;

@androidx.room.Database(entities = {Device.class, Datalog.class, DataPoint.class}, version = 1, exportSchema = false)
public abstract class Database extends RoomDatabase {
    private static volatile Database INSTANCE;

    public abstract DeviceDao deviceDao();
    public abstract DatalogDao datalogDao();
    public abstract DataPointDao dataPointDao();

    public static Database getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), Database.class, "database").build();
                }
            }
        }
        return INSTANCE;
    }
}