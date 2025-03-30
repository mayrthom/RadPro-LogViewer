package com.mayrthom.radprologviewer.database.device;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DeviceDao {
    @Insert
    void insertDevice(Device device);

    @Query("DELETE FROM Device WHERE deviceId = :deviceId")
    void deleteDevice(long deviceId);

    @Query("SELECT * FROM Device")
    LiveData<List<Device>> getAllDevices();

    @Query("SELECT * FROM Device WHERE deviceId = :deviceId LIMIT 1")
    LiveData<Device> getDeviceById(long deviceId);

    @Update
    void updateDevice(Device device);

    @Query("SELECT EXISTS(SELECT 1 FROM Device WHERE deviceId = :deviceId)")
    boolean exists(long deviceId);
}
