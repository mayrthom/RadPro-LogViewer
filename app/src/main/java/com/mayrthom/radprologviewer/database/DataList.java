package com.mayrthom.radprologviewer.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.github.mikephil.charting.data.Entry;
import com.google.gson.Gson;
import com.mayrthom.radprologviewer.database.device.Device;
import com.mayrthom.radprologviewer.utils.MovingAverageFilter;
import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
 This class is used as wrapper which contains the data which is displayed in one plot.
 */
public class DataList extends ArrayList<DataPoint> {

    private Device device;

    public DataList (String csv, Device device)
    {
        super();
        super.addAll(createDataPointList(csv));
        this.device = device;
    }

    public DataList (List<DataPoint>l , Device device) {
        super(l);
        this.device = device;
    }

    public DataList () {
    }

     //creating the datalist from the csv input string
     private List<DataPoint> createDataPointList(String csv) {
         if (csv == null || csv.trim().isEmpty()) {
             return new ArrayList<>();
         }

         String normalized = csv.replace('\r', ';').replace('\n', ';');
         String[] parts = normalized.split(";", -1);

         List<DataPoint> result = new ArrayList<>();
         long[] previous = null;

         for (String part : parts) {
             String s = part.trim();

             // empty entry by ";;"
             if (s.isEmpty()) {
                 previous = null;
                 continue;
             }

             // ignore header e.g. "OK time,tubePulseCount"
             if (s.startsWith("OK") || s.toLowerCase().contains("time")) {
                 continue;
             }

             String[] arr = s.split(",");
             if (arr.length != 2) {
                 continue;
             }

             try {
                 long t = Long.parseLong(arr[0].trim());
                 long v = Long.parseLong(arr[1].trim());

                 long[] current = new long[]{t, v};

                 if (previous != null) {
                     long t1 = previous[0];
                     long v1 = previous[1];

                     if (t != t1) {
                         long time = ((t - t1) / 2) + t1;
                         float radiation = (float) (v - v1) / (float) (t - t1);

                         if (radiation >= 0) {
                             result.add(new DataPoint(0, radiation, time));
                         }
                     }
                 }

                 previous = current;

             } catch (NumberFormatException ignored) {
             }
         }

         return result;
     }


    // Saving a csv of the datalist file into the Download Directory with a given file Name
    public boolean exportCsv(Context context, String fileName) {

        ContentResolver resolver = context.getContentResolver();
        Uri csvUri;
        Gson gson = new Gson();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName + ".csv");
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            csvUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (csvUri == null)
                return false;
        } else {
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(directory, fileName + ".csv");
            csvUri = Uri.fromFile(file);
        }

        try (OutputStream outputStream = resolver.openOutputStream(csvUri)) {
            if (outputStream != null) {
                outputStream.write((gson.toJson(device) + "\n").getBytes());
                outputStream.write(("time,radiation[CPS]\n").getBytes());
                for (DataPoint point : this) {
                    outputStream.write((point.timestamp + "," + point.radiationLevel + "\n").getBytes());
                }
                outputStream.flush();
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    public long getStartPoint()
    {
        DataPoint startpoint = this.stream()
                .min(Comparator.comparingLong(v -> v.timestamp))
                .orElse(null);
        return startpoint == null ? 0: startpoint.timestamp;
    }

    // Get the List in a useful form for the MPAndroidChart Library
    public List<Entry> getEntrySet()
    {
        long startPoint = getStartPoint();
        return this.stream()
        .map(dataPoint ->
        {
            float y = dataPoint.radiationLevel;
            float x = dataPoint.timestamp - startPoint;
            return new Entry(x,y);
        }).collect(Collectors.toList());
    }

    // Get the List in a useful form for the MPAndroidChart Library and applying a filter
    public List<Entry> getFilteredEntrySet(int window)
    {
        if (this.size()< window) {
            return getEntrySet();
        }
        else {
            MovingAverageFilter filter = new MovingAverageFilter(window);
            return filter.applyFilter(getEntrySet());
        }
    }
    public float getConversionFactor() {
        return device.conversionValue;
    }

    public Device getDevice() {
        return device;
    }


    public void setDatalogId(long datalogId)
    {
        for (DataPoint entry : this) {
            entry.datalogId = datalogId;
        }
    }
}
