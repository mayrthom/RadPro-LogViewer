package com.mayrthom.radprologviewer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.TypeConverters;
import androidx.room.PrimaryKey;

import com.mayrthom.radprologviewer.database.EntryConverter;
import com.github.mikephil.charting.data.Entry;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Entity
@TypeConverters({EntryConverter.class})
public class Datalog {
    @PrimaryKey(autoGenerate = true)

    private final long downloadDate;
    private long startPoint, endPoint;
    private final float conversionFactor;
    private final List<Entry> entrySet;
    private final String deviceInfo;

    public Datalog(long downloadDate, long startPoint, long endPoint, float conversionFactor, List<Entry> entrySet, String deviceInfo) {
        this.downloadDate = downloadDate;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.conversionFactor = conversionFactor;
        this.entrySet = entrySet;
        this.deviceInfo = deviceInfo;
    }
    @Ignore
    public Datalog(long downloadDate, String csv, String deviceInfo, float conversionFactor) {
        this.downloadDate = downloadDate;
        this.conversionFactor = conversionFactor;
        this.deviceInfo = deviceInfo;
        entrySet = processCSV(csv);
    }

    public List<Entry> processCSV(String csv) {
        csv = csv.replaceAll("(\\r|\\n)", "");
        String[] parts = csv.split(";");
        List<long[]> values = Arrays.stream(parts, 1, parts.length)
                .map(s -> s.split(","))
                .map(arr -> new long[]{Long.parseLong(arr[0]), Long.parseLong(arr[1])})
                .collect(Collectors.toList());
        if(values.isEmpty())
            return new ArrayList<>();

        long[] startpoints = values.stream()
                .min(Comparator.comparingLong(v -> v[0]))
                .orElse(null);
        startPoint = startpoints[0];

        long[] endpoints = values.stream()
                .max(Comparator.comparingLong(v -> v[0]))
                .orElse(null);
        endPoint = endpoints[0];


        return IntStream.range(0, values.size() - 1)
                .mapToObj(i -> {
                    long t1 = values.get(i)[0];
                    long t2 = values.get(i + 1)[0];
                    if(t1 == t2)
                        return null;

                    long v1 = values.get(i)[1];
                    long v2 = values.get(i + 1)[1];
                    /*
                    The difference quotient is calculated
                    and the start value is subtracted from each time value
                    (thus making time the relative value to the start value)
                    to prevent rounding errors due to the float type.
                     */
                    float x =  ((float)(t2-t1) )/2f + (t1 -startPoint);
                    float y = ((float)(v2-v1) / (float)(t2-t1) ); //value in CPS
                    return new Entry(x, y);
                })
                .filter(x -> x!= null)
                .collect(Collectors.toList());
    }

    public List<Entry> getEntrySet() {
        return entrySet;
    }

    public long getStartPoint() {
        return startPoint;
    }

    public long getEndPoint() {
        return endPoint;
    }

    public long getDownloadDate() {
        return downloadDate;
    }

    public float getConversionFactor() {
        return conversionFactor;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    /* Saving a csv of the datalog file into the Download Directory with a given file Name */
    public boolean exportCsv(Context context, String fileName) {

        ContentResolver resolver = context.getContentResolver();
        Uri csvUri;

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
                outputStream.write(("time,radiation[CPS]\n").getBytes());
                for (Entry entry : entrySet) {
                    outputStream.write(((((long) entry.getX() + startPoint)) + "," + entry.getY() + "\n").getBytes());
                }
                outputStream.flush();
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }
}
