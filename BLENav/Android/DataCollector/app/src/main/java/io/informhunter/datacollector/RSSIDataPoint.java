package io.informhunter.datacollector;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by informhunter on 04.03.2017.
 */

public class RSSIDataPoint extends DataPoint {
    private int RSSI;
    private String Name;

    RSSIDataPoint(String name, int RSSI) {
        Type = DataPointType.RSSI;
        this.Name = name;
        this.RSSI = RSSI;
    }

    public static void WriteHeaderToFile(FileWriter fileWriter) throws IOException {
        fileWriter.write("RSSI,Name,Timestamp\n");
    }

    @Override
    public void WriteToFile(FileWriter fileWriter) throws IOException {
        fileWriter.write(String.format(Locale.US, "%d,%s,%d\n",
                RSSI,
                Name,
                Timestamp));
    }
}
