package com.mayrthom.radprologviewer.viewModel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mayrthom.radprologviewer.DataList;
import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.database.Repository;
import com.mayrthom.radprologviewer.database.datalog.Datalog;

import java.util.ArrayList;
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
    public void deleteDeviceWithDatalogs(Device device) {
        repository.deleteDeviceWithDatalogs(device);
    }
    public void addDatalogWithEntries(DataList dataPoints) {
        repository.addDatalogWithEntries(dataPoints);
    }

    public LiveData<List<DataList>> getDataListForDevice() {
        MediatorLiveData<List<DataList>> result = new MediatorLiveData<>();
        List<DataList> dataLists = new ArrayList<>();
        List<LiveData<?>> activeSources = new ArrayList<>();

        result.addSource(repository.getAllDevices(), devices -> {
            for (LiveData<?> source : activeSources) result.removeSource(source);
            activeSources.clear();
            dataLists.clear();

            if (devices == null || devices.isEmpty()) {
                result.setValue(new ArrayList<>());
                return;
            }
            for (Device device : devices) {
                LiveData<List<DataPoint>> datapointList = repository.getDataPointsForDevice(device.deviceId);
                activeSources.add(datapointList);
                result.addSource(datapointList, dataPoints -> {
                    if (dataPoints == null || dataPoints.isEmpty()) return;
                    dataLists.add(new DataList(dataPoints, device));
                    if (dataLists.size() == devices.size()) {
                        result.setValue(new ArrayList<>(dataLists));
                    }
                });
            }
        });
        return result;
    }

    public LiveData<List<DataList>> getAllDatalogs() {
        MediatorLiveData<List<DataList>> result = new MediatorLiveData<>();
        List<DataList> dataLists = new ArrayList<>();
        List<LiveData<?>> activeSources = new ArrayList<>();

        result.addSource(repository.getAllDatalogs(), datalogs -> {
            for (LiveData<?> source : activeSources)
                result.removeSource(source);

            activeSources.clear();
            dataLists.clear();

            if (datalogs == null || datalogs.isEmpty()) {
                result.setValue(new ArrayList<>());
                return;
            }

            for (Datalog datalog : datalogs) {
                LiveData<List<DataPoint>> dataPointsLiveData = repository.getDataPointsForDatalog(datalog.datalogId);
                activeSources.add(dataPointsLiveData);
                result.addSource(dataPointsLiveData, dataPoints -> {
                    if (dataPoints == null || dataPoints.isEmpty()) return;
                    LiveData<Device> deviceLiveData = repository.getDeviceById(datalog.deviceId);
                    activeSources.add(deviceLiveData);
                    result.addSource(deviceLiveData, device -> {
                        if (device == null) return;
                        dataLists.add(new DataList(dataPoints, device, datalog));
                        if (dataLists.size() == datalogs.size()) {
                            result.setValue(new ArrayList<>(dataLists));
                        }
                    });
                });
            }
        });

        return result;
    }

}
