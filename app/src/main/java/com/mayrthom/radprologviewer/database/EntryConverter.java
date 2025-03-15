package com.mayrthom.radprologviewer.database;

import androidx.room.TypeConverter;

import com.github.mikephil.charting.data.Entry;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class EntryConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromEntryList(List<Entry> entries) {
        return gson.toJson(entries);
    }

    @TypeConverter
    public static List<Entry> toEntryList(String json) {
        Type listType = new TypeToken<List<Entry>>() {}.getType();
        return gson.fromJson(json, listType);
    }
}