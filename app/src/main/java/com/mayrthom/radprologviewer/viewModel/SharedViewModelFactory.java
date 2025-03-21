package com.mayrthom.radprologviewer.viewModel;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class SharedViewModelFactory implements ViewModelProvider.Factory {
        private final Context context;


    public SharedViewModelFactory(Context context) {
            this.context = context;
        }


    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SharedViewModel(context);
    }
}
