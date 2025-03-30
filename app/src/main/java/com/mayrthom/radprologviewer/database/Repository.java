package com.mayrthom.radprologviewer.database;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.mayrthom.radprologviewer.DataList;
import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;
import com.mayrthom.radprologviewer.database.dataPoint.DataPointDao;
import com.mayrthom.radprologviewer.database.datalog.Datalog;
import com.mayrthom.radprologviewer.database.datalog.DatalogDao;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.database.device.DeviceDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Repository {
    private final DeviceDao deviceDao;
    private final DatalogDao datalogDao;
    private final DataPointDao dataPointDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public Repository(Context context) {
        Database database = Database.getDatabase(context);

        this.deviceDao = database.deviceDao();
        this.datalogDao = database.datalogDao();
        this.dataPointDao = database.dataPointDao();
    }

    /* Devices */
    public LiveData<List<Device>> getAllDevices() {
        return deviceDao.getAllDevices();
    }
    public LiveData<Device> getDeviceById(long id) {
        return deviceDao.getDeviceById(id);
    }
    public void deleteDeviceWithDatalogs(Device device)
    {
        executorService.execute(() ->{
        deviceDao.deleteDevice(device.deviceId);
        });
    }

    /* Datalogs */
    public LiveData<List<Datalog>> getAllDatalogs() {
        return datalogDao.getAllDatalogs();
    }
    public void deleteDatalogWithPoints(Datalog datalog)
    {
        executorService.execute(() -> {
            datalogDao.deleteDatalogById(datalog.datalogId);
            if (datalogDao.getDatalogCountForDevice(datalog.deviceId) == 0)
                deviceDao.deleteDevice(datalog.deviceId);
        });
    }
    public void addDatalogWithEntries(DataList datalist) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Device device = datalist.getDevice();
            long deviceId = device.deviceId;
            if (deviceDao.exists(deviceId))
                deviceDao.updateDevice(device); //update device in case the conversionvalue has changed.
            else
                deviceDao.insertDevice(device);

            Datalog datalog = new Datalog(System.currentTimeMillis(), deviceId);
            long datalogId = datalogDao.insertDatalog(datalog);

            // Now insert datapoints with tha according datalogId
            datalist.setDatalogId(datalogId);
            dataPointDao.insertAll(datalist);
        });
    }

    /* Datapoints */
    public LiveData<List<DataPoint>> getDataPointsForDatalog(long datalogId) {
        return dataPointDao.getDataPointsForDatalog(datalogId);
    }

    public LiveData<List<DataPoint>> getDataPointsForDevice(long deviceId) {
        return dataPointDao.getDataPointsForDevice(deviceId);
    }

}
