package com.mayrthom.radprologviewer.database;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;
import com.mayrthom.radprologviewer.database.dataPoint.DataPointDao;
import com.mayrthom.radprologviewer.database.datalog.Datalog;
import com.mayrthom.radprologviewer.database.datalog.DatalogDao;
import com.mayrthom.radprologviewer.database.datalog.DatalogWithTimestampsAndDevice;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.database.device.DeviceDao;

import java.util.List;

public class Repository {
    private final DeviceDao deviceDao;
    private final DatalogDao datalogDao;
    private final DataPointDao dataPointDao;

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

    public void deleteDeviceWithDatalogs(Device device)
    {
        deviceDao.deleteDevice(device.deviceId);
    }

    /* Datalogs */
    public void deleteDatalogWithPoints(Datalog datalog)
    {
            datalogDao.deleteDatalogById(datalog.datalogId);
            if (datalogDao.getDatalogCountForDevice(datalog.deviceId) == 0)
                deviceDao.deleteDevice(datalog.deviceId);
    }
    public void addDatalogWithEntries(DataList datalist) {
            Device device = datalist.getDevice();
            long deviceId = device.deviceId;
            if (deviceDao.exists(deviceId))
                deviceDao.updateDevice(device);
            else
                deviceDao.insertDevice(device);

            Datalog datalog = new Datalog(System.currentTimeMillis(), deviceId);
            long datalogId = datalogDao.insertDatalog(datalog);

            // Now insert datapoints with tha according datalogId
            datalist.setDatalogId(datalogId);
            dataPointDao.insertAll(datalist);
    }
    public LiveData<List<DatalogWithTimestampsAndDevice>> getDatalogWithTimestampsAndDevice()
    {
        return datalogDao.getDatalogWithTimestampsAndDevice();
    }

    /* Datapoints */
    public LiveData<List<DataPoint>> getDataPointsForDatalog(long datalogId) {
        return dataPointDao.getDataPointsForDatalog(datalogId);
    }

    public LiveData<List<DataPoint>> getDataPointsForDevice(long deviceId) {
        return dataPointDao.getDataPointsForDevice(deviceId);
    }
}
