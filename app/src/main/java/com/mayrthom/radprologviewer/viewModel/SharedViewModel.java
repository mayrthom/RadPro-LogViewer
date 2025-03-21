package com.mayrthom.radprologviewer.viewModel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mayrthom.radprologviewer.DataList;
import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.database.Repository;
import com.mayrthom.radprologviewer.database.datalog.Datalog;

import java.util.List;


//public class SharedViewModel extends ViewModel {
public class SharedViewModel extends ViewModel {

    private final MutableLiveData<DataList> dataListLiveData = new MutableLiveData<>();
    private final Repository repository;

    public SharedViewModel(Context context) {
        this.repository = new Repository(context);
    }

    //Datalist for the Plot
    public LiveData<DataList> getDataList() {
        return dataListLiveData;
    }
    public void setDataList(DataList d) {
        dataListLiveData.setValue(d);
    }

    //Functions to access room database
    public void deleteDatalogWithPoints(Datalog datalog) {
        repository.deleteDatalogWithPoints(datalog);
    }
    public LiveData<List<DataPoint>> getDataPointsForDatalog(long datalogId) {
        return repository.getDataPointsForDatalog(datalogId);
    }
    public void deleteDeviceWithDatalogs(Device device) {
        repository.deleteDeviceWithDatalogs(device);
    }

    public LiveData<List<Datalog>> getAllDatalogs() {
        return repository.getAllDatalogs();
    }

    public LiveData<Device> getDeviceById(long id) {
        return repository.getDeviceById(id);
    }

    public LiveData<List<Device>> getAllDevices() {
        return repository.getAllDevices();
    }

    public void addDatalogWithEntries(Device device, DataList dataPoints) {
        repository.addDatalogWithEntries(device,dataPoints);
    }
    public LiveData<List<DataPoint>> getDataPointsForDevice(long deviceId) {
        return repository.getDataPointsForDevice(deviceId);
    }

}
