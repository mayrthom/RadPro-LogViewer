package com.mayrthom.radprologviewer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Datalog> datalogLiveData = new MutableLiveData<>();
    public void setDatalog(Datalog d) {
        datalogLiveData.setValue(d);
    }
    public LiveData<Datalog> getDatalog() {
        return datalogLiveData;
    }
}
