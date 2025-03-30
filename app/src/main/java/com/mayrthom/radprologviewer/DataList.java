package com.mayrthom.radprologviewer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.github.mikephil.charting.data.Entry;
import com.google.gson.Gson;
import com.mayrthom.radprologviewer.database.dataPoint.DataPoint;
import com.mayrthom.radprologviewer.database.datalog.Datalog;
import com.mayrthom.radprologviewer.database.device.Device;

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

    //private float conversionFactor = 0;
    private Device device;
    private Datalog datalog;

    public DataList (String csv, Device device)
    {
        super();
        super.addAll(createDataPointList(csv));
        this.device = device;
    }

    public DataList (List<DataPoint>l, Device device, Datalog datalog)
    {
        super(l);
        this.device = device;
        this.datalog = datalog;
    }


    public DataList (List<DataPoint>l , Device device) {
        super(l);
        this.device = device;
    }

    public DataList () {
    }

     //creating the datalist from the csv input string
    private List<DataPoint> createDataPointList(String csv)
    {
        csv = csv.replaceAll("(\\r|\\n)", "");
        String[] parts = csv.split(";");
        List<long[]> values = Arrays.stream(parts, 1, parts.length)
                .map(s -> s.split(","))
                .map(arr -> new long[]{Long.parseLong(arr[0]), Long.parseLong(arr[1])})
                .collect(Collectors.toList());
        if(values.isEmpty())
            return new ArrayList<>();

        return IntStream.range(0, values.size() - 1)
                .mapToObj(i -> {
                    long t1 = values.get(i)[0];
                    long t2 = values.get(i + 1)[0];
                    if(t1 == t2)
                        return null;

                    long v1 = values.get(i)[1];
                    long v2 = values.get(i + 1)[1];
                    long time =  ((t2-t1))/2 + t1;
                    float radiation = ((float)(v2-v1) / (float)(t2-t1) ); //value in CPS
                    return new DataPoint(0,radiation,time);
                })
                .filter(x -> x!= null)
                .collect(Collectors.toList());
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

    public long getEndPoint() {
        DataPoint startpoint = this.stream()
                .max(Comparator.comparingLong(v -> v.timestamp))
                .orElse(null);
        return startpoint == null ? 0 : startpoint.timestamp;
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

    public Datalog getDatalog() {
        return datalog;
    }

    public void setDatalogId(long datalogId)
    {
        for (DataPoint entry : this) {
            entry.datalogId = datalogId;
        }
    }
}
