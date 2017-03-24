package io.informhunter.datacollector;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by informhunter on 16.03.2017.
 */

public abstract class DataPoint {
    protected DataPointType Type;
    protected long Timestamp;


    public DataPoint() {
        Timestamp = Calendar.getInstance().getTimeInMillis();
    }

    public abstract void WriteToFile(FileWriter fileWriter) throws IOException;

    public DataPointType GetPointType() {
        return Type;
    }

    public long GetTimestamp() {
        return Timestamp;
    }

}
