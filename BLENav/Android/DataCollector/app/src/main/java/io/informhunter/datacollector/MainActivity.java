package io.informhunter.datacollector;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<float[]> currentRoute = new ArrayList<>();
    private Iterator<float[]> routeIterator;
    private float[] currentPoint = new float[2];

    private float[] cursorPoint = new float[2];
    private Bitmap original;
    private List<DataPoint> data = new ArrayList<>();

    private SensorManager mSensorManager;
    private Sensor mLinearAccSensor;
    private Sensor mMagneticSensor;
    private Sensor mGravitySensor;

    private float[] gravityValues = null;
    private float[] magneticValues = null;

    private boolean isCapturing = false;
    private BluetoothAdapter mAdapter;
    final private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            data.add(new RSSIDataPoint(device.getName(), rssi));

            TextView textView = (TextView) findViewById(R.id.textLog);
            textView.append("Discovered " + device.getName() + " ");
            textView.append(String.valueOf(rssi) + "\n");
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLinearAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                magneticValues = event.values;
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        }, mMagneticSensor, SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                gravityValues = event.values;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }, mGravitySensor, SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(!isCapturing || gravityValues == null || magneticValues == null) {
                    return;
                }

                float[] deviceRelativeAcceleration = new float[4];
                deviceRelativeAcceleration[0] = event.values[0];
                deviceRelativeAcceleration[1] = event.values[1];
                deviceRelativeAcceleration[2] = event.values[2];
                deviceRelativeAcceleration[3] = 0;
                float[] R = new float[16], I = new float[16], earthAcc = new float[16];

                SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

                float[] inv = new float[16];
                android.opengl.Matrix.invertM(inv, 0, R, 0);
                android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);

                data.add(new VectorDataPoint(
                        earthAcc[0],
                        earthAcc[1],
                        earthAcc[2],
                        DataPointType.LinearAcceleration));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }, mLinearAccSensor, SensorManager.SENSOR_DELAY_NORMAL);


        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        original = BitmapFactory.decodeResource(getResources(), R.drawable.cropped_flat, options);
    }


    public void onClickButtonCapture(View v) {
        TextView textView = (TextView) findViewById(R.id.textLog);
        Button btnNext = (Button) findViewById(R.id.nextPointButton);

        ToggleButton btn = (ToggleButton) v;
        if (btn.isChecked()) {
            //Enable
            textView.setText("Start capture\n");
            routeIterator = currentRoute.iterator();
            if(!routeIterator.hasNext()) {
                btn.toggle();
                return;
            }
            float[] point = routeIterator.next();
            data.add(new PositionDataPoint(point[0], point[1]));
            btnNext.setEnabled(true);
            btnNext.callOnClick();
            isCapturing = true;
            mAdapter.startLeScan(leScanCallback);
        } else {
            //Disable
            textView.setText("Stop capture\n");
            btnNext.setEnabled(false);
            mAdapter.stopLeScan(leScanCallback);
            isCapturing = false;
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        TextView coordsText = (TextView) findViewById(R.id.coordsText);
        ImageView flatPlan = (ImageView) findViewById(R.id.flatPlan);

        if(isCapturing) {
            return false;
        }

        int loc[] = new int[2];
        flatPlan.getLocationOnScreen(loc);

        float x = event.getX();
        float y = event.getY();
        coordsText.setText(String.valueOf(x - loc[0]) + " " + String.valueOf(y - loc[1]) + "\n");
        //drawPoint(x - loc[0], y - loc[1], false);

        x = (x - loc[0]) / flatPlan.getWidth() * 13.90f;
        y = (y - loc[1]) / flatPlan.getHeight() * 7.35f;

        cursorPoint[0] = x;
        cursorPoint[1] = y;

        coordsText.append(String.valueOf(x) + " " + String.valueOf(y) + "\n");

        redrawPlan();

        return super.onTouchEvent(event);
    }

    private void drawPoint(float x, float y, boolean coordsAreReal) {
        ImageView flatPlan = (ImageView) findViewById(R.id.flatPlan);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);

        Bitmap mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(mutableBitmap);

        float scaleX, scaleY;
        if(coordsAreReal) {
            scaleX = (float)mutableBitmap.getWidth() / 13.90f;
            scaleY = (float)mutableBitmap.getHeight() / 7.35f;
        } else {
            scaleX = (float)mutableBitmap.getWidth() / (float)flatPlan.getWidth();
            scaleY = (float)mutableBitmap.getHeight() / (float)flatPlan.getHeight();
        }

        canvas.drawCircle(x * scaleX , y * scaleY, 25, paint);
        flatPlan.setImageBitmap(mutableBitmap);
    }

    private void redrawPlan() {
        ImageView flatPlan = (ImageView) findViewById(R.id.flatPlan);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);

        Bitmap mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        float scaleX, scaleY;
        scaleX = (float)mutableBitmap.getWidth() / 13.90f;
        scaleY = (float)mutableBitmap.getHeight() / 7.35f;

        for(float[] point : currentRoute) {
            canvas.drawCircle(point[0] * scaleX , point[1] * scaleY, 25, paint);
        }

        paint.setColor(Color.BLUE);
        canvas.drawCircle(currentPoint[0] * scaleX , currentPoint[1] * scaleY, 25, paint);

        paint.setColor(Color.GREEN);
        canvas.drawCircle(cursorPoint[0] * scaleX , cursorPoint[1] * scaleY, 25, paint);

        flatPlan.setImageBitmap(mutableBitmap);
    }

    public void onClickButtonClear(View v) {
        TextView textLog = (TextView) findViewById(R.id.textLog);
        textLog.setText("");
    }

    public void onClickButtonSave(View v) {
        TextView textLog = (TextView) findViewById(R.id.textLog);
        textLog.append("Total captures: " + String.valueOf(data.size()) + "\n");
        saveData(data);
    }


    public void onAddPointButtonClick(View v) {
        currentRoute.add(cursorPoint.clone());
        redrawPlan();
    }

    public void onResetRouteButtonClick(View v) {
        currentRoute.clear();
        redrawPlan();
    }

    public void onNextPointButtonClick(View v) {
        data.add(new PositionDataPoint(currentPoint[0], currentPoint[1]));
        if(routeIterator.hasNext()) {
            currentPoint = routeIterator.next();
            redrawPlan();
        } else {
            ToggleButton tb = (ToggleButton)findViewById(R.id.toggleCaptureButton);
            tb.toggle();
            tb.callOnClick();
        }
    }


    private void saveData(List<DataPoint> data) {

        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "datacollector");
        boolean created = folder.mkdirs();

        String positionDataFile = folder.toString() + "/" + "position_data.csv";
        String rssiDataFile = folder.toString() + "/" + "rssi_data.csv";
        String laDataFile = folder.toString() + "/" + "la_data.csv";
        String DataFile = folder.toString() + "/" + "la_data.csv";



        try {
            FileWriter posFW = new FileWriter(positionDataFile);
            PositionDataPoint.WriteHeaderToFile(posFW);

            FileWriter rssiFW = new FileWriter(rssiDataFile);
            RSSIDataPoint.WriteHeaderToFile(rssiFW);

            FileWriter laFW = new FileWriter(laDataFile);
            VectorDataPoint.WriteHeaderToFile(laFW);

            for(DataPoint dp : data) {
                switch (dp.GetPointType()) {
                    case Position:
                        dp.WriteToFile(posFW);
                        break;
                    case RSSI:
                        dp.WriteToFile(rssiFW);
                        break;
                    case LinearAcceleration:
                        dp.WriteToFile(laFW);
                        break;
                }
            }
            posFW.close();
            rssiFW.close();
            laFW.close();
        } catch (Exception e) {
            e.getMessage();
        }
    }


}