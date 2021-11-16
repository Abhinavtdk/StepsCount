package com.example.googlefitsensor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.googlefitsensor.databinding.ActivityMainBinding;
import com.example.googlefitsensor.permissionUtil.PermissionManager;
import com.example.googlefitsensor.permissionUtil.Permissions;
import com.example.googlefitsensor.utils.CommonUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements PermissionManager.PermissionListener, OnSuccessListener {

    int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;

    private boolean inLastWeekData = false;

    private final String TAG = "MainActivity";

    private FitnessOptions fitnessOptions;

    private FitnessDataResponseModel fitnessDataResponseModel;

    private ActivityMainBinding activityMainBinding;

    private float avg_bpm = 0;
    private float min_bpm = 0;

    private int sleepStage[] = new int [3];

    TextView as, bs, cs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        initialization();
        checkPermissions();
    }

    private void checkPermissions() {
        Log.d("MainActivity", "checkPermissions: ");
//        if (!PermissionManager.hasPermissions(this, Permissions.LOCATION_PERMISSION)) {
        if (false) {
            Log.d("MainActivity", " No permission");
            PermissionManager.requestPermissions(this, this, "", Permissions.LOCATION_PERMISSION);
        } else {
            Log.d("MainActivity", " Has permission");
            checkGoogleFitPermission();
        }
    }

    private void checkGoogleFitPermission() {
        fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .build();

        GoogleSignInAccount account = getGoogleAccount();
        Log.d(TAG, "checkGoogleFitPermission: Outside");

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            Log.d(TAG, "checkGoogleFitPermission: ");
            GoogleSignIn.requestPermissions(
                    MainActivity.this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    account,
                    fitnessOptions
            );
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startDataReading();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startDataReading() {
        getTodayData();
//        heartData();
//        trythis();
//        Toast.makeText(this,"Hey",Toast.LENGTH_SHORT).show();

        subscribeAndGetRealTimeData(DataType.TYPE_STEP_COUNT_DELTA);
//        subscribeAndGetRealTimeData(DataType.TYPE_HEART_RATE_BPM);
    }

    private void trythis() {
        setHeartLate();
        setSleepStage();
    }

    private void setSleepStage() {
        final ArrayList<String> heartRateList = new ArrayList<>();

        Calendar start = Calendar.getInstance();
        start.set(Calendar.YEAR,2021);
        start.set(Calendar.MONTH, Calendar.NOVEMBER );
        start.set(Calendar.DATE, 12);

        start.set(Calendar.HOUR_OF_DAY, 00);
        start.set(Calendar.MINUTE, 40);
        start.set(Calendar.SECOND, 00);

        final long startTime = start.getTimeInMillis();


        Calendar end = Calendar.getInstance();
        end.set(Calendar.YEAR, 2021);
        end.set(Calendar.MONTH, Calendar.NOVEMBER );
        end.set(Calendar.DATE, 14);

        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 50);
        end.set(Calendar.SECOND, 00);

        final long endTime = end.getTimeInMillis();

        Fitness.getHistoryClient(this,
                getGoogleAccount())
                .readData(new DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .setTimeRange(startTime,endTime,TimeUnit.MINUTES)
                .build())
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        float G = avg_bpm-min_bpm;
                        float S;
                        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
//                        as = findViewById(R.id.a);
                        as.setText(format.format(startTime) + " " + format.format(endTime));

//                        bs = findViewById(R.id.b);
                        bs.setText("G : " + G);

                        DataSet dataset = dataReadResponse.getDataSet(DataType.TYPE_HEART_RATE_BPM);
                        Log.d("HeyLol", "I'm "+dataset);


                        for (DataPoint dataPoint: dataset.getDataPoints()){

                            for(Field field: dataPoint.getDataType().getFields()){

                                S = Float.valueOf(String.valueOf(dataPoint.getValue(field))) - min_bpm;
                                Log.d("HeyLol", (S+min_bpm) +" ");

                                if(S>(0.5*G)){
                                    sleepStage[0] +=1;
                                }else{
                                    if(S < (0.2 * G)){
                                        sleepStage[2] += 1;
                                    }
                                    else{
                                        sleepStage[1] += 1;
                                    }
                                }

                                heartRateList.add(format.format(dataPoint.getEndTime(TimeUnit.MINUTES)) + "Value: "+dataPoint.getValue(field) + ", S : "+S);
                            }

                        }
//                        cs = findViewById(R.id.c);
                        cs.setText(": " + sleepStage[0] +",  : " + sleepStage[1] + ",  : " + sleepStage[2]);
                    }
                });

//        ListView printView = (ListView)findViewById(R.id.bpm);
//        final ArrayAdapter<String> timeAdabtor = new ArrayAdapter<String>(
//                this, android.R.layout.simple_list_item_1, heartRateList);
//        printView.setAdapter(timeAdabtor);
    }

    private void setHeartLate() {

        final ArrayList<String> heartLateList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        final long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        final long startTime = cal.getTimeInMillis();


        Fitness.getHistoryClient(this,
                getGoogleAccount())
                .readData(new DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .setTimeRange(startTime,endTime,TimeUnit.MINUTES)
                .build())
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {

                        float count = 0, total = 0;
                        DataSet dataSet = dataReadResponse.getDataSet(DataType.TYPE_HEART_RATE_BPM);
                        Log.d("HeyLol", dataSet.toString() +" ");

                        for (DataPoint dp : dataSet.getDataPoints()) {

                            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                            for (Field field : dp.getDataType().getFields()) {

                                if(count ==0){
                                    min_bpm = Float.valueOf(String.valueOf(dp.getValue(field)));
                                }
                                else{
                                    if(Float.valueOf(String.valueOf(dp.getValue(field))) < min_bpm){
                                        min_bpm = Float.valueOf(String.valueOf(dp.getValue(field)));
                                    }
                                }
                                count++;
                                total += Float.valueOf(String.valueOf(dp.getValue(field)));


                            }
                            avg_bpm = total / count;
                        }

                    }
                });

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void heartData() {
//        Toast.makeText(this,"In heart",Toast.LENGTH_SHORT).show();
        Fitness.getSensorsClient(this, getGoogleAccount())
                .findDataSources(
                        new DataSourcesRequest.Builder()
                                .setDataTypes(DataType.TYPE_HEART_RATE_BPM)
                                .setDataSourceTypes(DataSource.TYPE_RAW)
                                .build())
                .addOnSuccessListener(dataSources -> {
//                    Toast.makeText(this,"In Success"+dataSources,Toast.LENGTH_SHORT).show();
                    dataSources.forEach(dataSource -> {
//                        Toast.makeText(this,"In Success"+dataSources,Toast.LENGTH_SHORT).show();
                        Log.i("MainActivity", "heartData: "+dataSource.getStreamIdentifier());
                        Toast.makeText(this,dataSource.getStreamIdentifier(),Toast.LENGTH_SHORT).show();
                        Log.i("MainActivity", "heartData: "+dataSource.getDataType().getName());
                        Toast.makeText(this,dataSource.getDataType().getName(),Toast.LENGTH_SHORT).show();

                        if(dataSource.getDataType() == DataType.TYPE_HEART_RATE_BPM){
                            Log.i("MainActivity", "heartData: Data Source found");
                            Toast.makeText(this,"Data Source found",Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "heartData: ",e);
                    Toast.makeText(this,"Error"+e,Toast.LENGTH_SHORT).show();
                });
    }

    private void subscribeAndGetRealTimeData(DataType dataType) {
        Fitness.getRecordingClient(this, getGoogleAccount())
                .subscribe(dataType)
                .addOnSuccessListener(aVoid -> {
                    Log.d("MainActivity", "Subscribed");
                })
                .addOnFailureListener(e -> {
                    Log.d("MainActivity", "Not subscribed: " + e.getLocalizedMessage());
                });

//        getDataUsingSensor(dataType);
    }

    private void getDataUsingSensor(DataType dataType) {
        Fitness.getSensorsClient(this, getGoogleAccount())
                .add(new SensorRequest.Builder()
                                .setDataType(dataType)
                                .setSamplingRate(1, TimeUnit.SECONDS)
                                .build(),
                        new OnDataPointListener() {
                            @Override
                            public void onDataPoint(@NonNull DataPoint dataPoint) {
                                float value = Float.parseFloat(dataPoint.getValue(Field.FIELD_STEPS).toString());
//                                float value1 = Float.parseFloat(dataPoint.getValue(Field.FIELD_BPM).toString());
//                                Toast.makeText(getBaseContext(),,Toast.LENGTH_SHORT).show();
                                fitnessDataResponseModel.steps = Float.parseFloat(new DecimalFormat("#.##").format(value + fitnessDataResponseModel.steps));
                                activityMainBinding.setFitnessData(fitnessDataResponseModel);
                            }
                        }
                );
    }

    private void getTodayData() {
        Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(this);
        Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_CALORIES_EXPENDED)
                .addOnSuccessListener(this);
        Fitness.getHistoryClient(this, getGoogleAccount())
                .readDailyTotal(DataType.TYPE_DISTANCE_DELTA)
                .addOnSuccessListener(this);
    }

    private GoogleSignInAccount getGoogleAccount() {
        return GoogleSignIn.getAccountForExtension(MainActivity.this, fitnessOptions);
    }

    private void requestForHistory() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        long endTime = cal.getTimeInMillis();

        cal.set(2021, 10, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0); //so it get all day and not the current hour
        cal.set(Calendar.MINUTE, 0); //so it get all day and not the current minute
        cal.set(Calendar.SECOND, 0); //so it get all day and not the current second
        long startTime = cal.getTimeInMillis();


        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA)
                .aggregate(DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getHistoryClient(this, getGoogleAccount())
                .readData(readRequest)
                .addOnSuccessListener(this);
    }

    private void initialization() {
        fitnessDataResponseModel = new FitnessDataResponseModel();

        activityMainBinding.buttonLastWeekData.setOnClickListener(v -> {
            if(!inLastWeekData) {
                requestForHistory();
                inLastWeekData = true;
                activityMainBinding.buttonLastWeekData.setText("Get Today's data");
            }else{
                inLastWeekData = false;
                getTodayData();
                activityMainBinding.buttonLastWeekData.setText("Get Last week's data");
            }

        });
    }

    @Override
    public void onPermissionsGranted(List<String> perms) {
        if (perms != null && perms.size() == Permissions.LOCATION_PERMISSION.length) {
            checkGoogleFitPermission();
        }
    }

    @Override
    public void onPermissionsDenied(List<String> perms) {
        if (perms.size() > 0) {
            PermissionManager.requestPermissions(this, this, "", Permissions.LOCATION_PERMISSION);
        }
    }

    @Override
    public void onPermissionNeverAsked(List<String> perms) {
        CommonUtils.openSettingForPermission(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.onRequestPermissionsResult(this, this, requestCode, permissions, grantResults);
    }

    @Override
    public void onSuccess(Object o) {
        if (o instanceof DataSet) {
            DataSet dataSet = (DataSet) o;
            if (dataSet != null) {
                getDataFromDataSet(dataSet);
            }
        } else if (o instanceof DataReadResponse) {
            fitnessDataResponseModel.steps = 0f;
            fitnessDataResponseModel.calories = 0f;
            fitnessDataResponseModel.distance = 0f;
            DataReadResponse dataReadResponse = (DataReadResponse) o;
            if (dataReadResponse.getBuckets() != null && !dataReadResponse.getBuckets().isEmpty()) {
                List<Bucket> bucketList = dataReadResponse.getBuckets();

                if (bucketList != null && !bucketList.isEmpty()) {
                    for (Bucket bucket : bucketList) {
                        DataSet stepsDataSet = bucket.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);
                        getDataFromDataReadResponse(stepsDataSet);
                        DataSet caloriesDataSet = bucket.getDataSet(DataType.TYPE_CALORIES_EXPENDED);
                        getDataFromDataReadResponse(caloriesDataSet);
                        DataSet distanceDataSet = bucket.getDataSet(DataType.TYPE_DISTANCE_DELTA);
                        getDataFromDataReadResponse(distanceDataSet);
                    }
                }
            }
        }
    }

    private void getDataFromDataReadResponse(DataSet dataSet) {
        List<DataPoint> dataPoints = dataSet.getDataPoints();
        for (DataPoint dataPoint : dataPoints) {
            for (Field field : dataPoint.getDataType().getFields()) {

                float value = Float.parseFloat(dataPoint.getValue(field).toString());
                Log.e(TAG, " data : " + value);

                if (field.getName().equals(Field.FIELD_STEPS.getName())) {
                    fitnessDataResponseModel.steps = Float.parseFloat(new DecimalFormat("#.##").format(value + fitnessDataResponseModel.steps));
                } else if (field.getName().equals(Field.FIELD_CALORIES.getName())) {
                    fitnessDataResponseModel.calories = Float.parseFloat(new DecimalFormat("#.##").format(value + fitnessDataResponseModel.calories));
                } else if (field.getName().equals(Field.FIELD_DISTANCE.getName())) {
                    fitnessDataResponseModel.distance = Float.parseFloat(new DecimalFormat("#.##").format(value + fitnessDataResponseModel.distance));
                }
            }
        }
        activityMainBinding.setFitnessData(fitnessDataResponseModel);
    }

    private void getDataFromDataSet(DataSet dataSet) {
        List<DataPoint> dataPoints = dataSet.getDataPoints();
        for (DataPoint dataPoint : dataPoints) {
            Log.e(TAG, " data manual : " + dataPoint.getOriginalDataSource().getStreamName());

            for (Field field : dataPoint.getDataType().getFields()) {

                float value = Float.parseFloat(dataPoint.getValue(field).toString());
                Log.e(TAG, " data : " + value);

                if (field.getName().equals(Field.FIELD_STEPS.getName())) {
                    fitnessDataResponseModel.steps = Float.parseFloat(new DecimalFormat("#.##").format(value));
                } else if (field.getName().equals(Field.FIELD_CALORIES.getName())) {
                    fitnessDataResponseModel.calories = Float.parseFloat(new DecimalFormat("#.##").format(value));
                } else if (field.getName().equals(Field.FIELD_DISTANCE.getName())) {
                    fitnessDataResponseModel.distance = Float.parseFloat(new DecimalFormat("#.##").format(value));
                }
            }
        }
        activityMainBinding.setFitnessData(fitnessDataResponseModel);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startDataReading();
            }
        }
    }

}