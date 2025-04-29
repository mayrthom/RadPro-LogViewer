package com.mayrthom.radprologviewer.utils;


import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mayrthom.radprologviewer.database.DataList;
import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.ui.LoadingDialog;
import com.mayrthom.radprologviewer.viewModel.SharedViewModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class FileImporter {
    public static void importCsv(Context context, Uri uri, SharedViewModel sharedViewModel) {
        if (uri != null) {
            LoadingDialog dialog = new LoadingDialog(context,"Importing CSV");
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(uri)));
                    Device device = new Gson().fromJson(reader.readLine(), new TypeToken<Device>() {}.getType()); // first line is device object stored as json
                    if (device == null || device.deviceId == 0 || device.deviceType == null || device.conversionValue == 0)
                        throw new IOException("Wrong input format!");
                    if (!reader.readLine().contains("time,radiation[CPS]")) // second line must be the title
                        throw new IOException("Wrong input format!");

                    List<DataPoint> dataPoints = reader.lines().map(s -> {
                        String[] strings = s.split(",");
                        return new DataPoint(0, Float.parseFloat(strings[1]), Long.parseLong(strings[0]));
                    }).collect(Collectors.toList());
                    reader.close();
                    sharedViewModel.addDatalogWithEntries(new DataList(dataPoints, device));

                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, "File imported successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, "File import failed!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            });
        }
    }


}
