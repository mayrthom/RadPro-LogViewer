package com.mayrthom.radprologviewer.database.datalog;

import androidx.room.Embedded;
import com.mayrthom.radprologviewer.database.device.Device;

public class DatalogWithTimestampsAndDevice {

    @Embedded
    public Datalog datalog; // Datalog data is embedded
    @Embedded
    public Device device;   // Device data is embedded
    public long minTimestamp;
    public long maxTimestamp;
}
