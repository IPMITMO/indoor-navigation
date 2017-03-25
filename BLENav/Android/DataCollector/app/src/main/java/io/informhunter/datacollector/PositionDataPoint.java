package io.informhunter.datacollector;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by informhunter on 16.03.2017.
 */

public class PositionDataPoint extends DataPoint {
    private float PosX;
    private float PosY;


    protected PositionDataPoint(float x, float y) {
        Type = DataPointType.Position;
        PosX = x;
        PosY = y;
    }

    public static void WriteHeaderToFile(FileWriter fileWriter) throws IOException {
        fileWriter.write("PosX,PosY,Timestamp\n");
    }

    @Override
    public void WriteToFile(FileWriter fileWriter) throws IOException {
        fileWriter.write(String.format(Locale.US, "%f,%f,%d\n",
                PosX,
                PosY,
                Timestamp));
    }
}
