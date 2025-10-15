package com.mayrthom.radprologviewer.viewModel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.mayrthom.radprologviewer.database.DataList;
import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;
import com.mayrthom.radprologviewer.database.datalog.DatalogWithTimestampsAndDevice;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.database.Repository;
import com.mayrthom.radprologviewer.database.datalog.Datalog;

import java.util.List;


public class SharedViewModel extends ViewModel {

    private final MutableLiveData<DataList> dataListLiveData = new MutableLiveData<>();
    private final Repository repository;

    public SharedViewModel(Context context) {
        this.repository = new Repository(context);
    }

    public LiveData<DataList> getDataList() {
        return dataListLiveData;
    }

    public void setDataList(DataList d) {
        dataListLiveData.setValue(d);
    }

    public void deleteDatalogWithPoints(Datalog datalog) {
        repository.deleteDatalogWithPoints(datalog);
    }

    public void deleteDeviceWithDatalogs(Device device) {
        repository.deleteDeviceWithDatalogs(device);
    }

    public void addDatalogWithEntries(DataList dataPoints) {
        repository.addDatalogWithEntries(dataPoints);
    }

    public LiveData<List<Device>> getAllDevices() {
        return repository.getAllDevices();
    }

    public LiveData<List<DataPoint>> loadDataPointsForDevice(String deviceId) {
        return repository.getDataPointsForDevice(deviceId);
    }

    public LiveData<List<DatalogWithTimestampsAndDevice>> getDatalogWithTimestampsAndDevice() {
        return repository.getDatalogWithTimestampsAndDevice();
    }

    public LiveData<List<DataPoint>> getDataPointsForDatalog(long datalogId) {
        return repository.getDataPointsForDatalog(datalogId);
    }
}
