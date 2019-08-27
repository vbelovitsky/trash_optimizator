package com.java.vbel.trashlocator.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.java.vbel.trashlocator.R;
import com.java.vbel.trashlocator.adapters.CategoryAdapter;
import com.java.vbel.trashlocator.dto.CategoryItem;
import com.java.vbel.trashlocator.dto.PointInfo;
import com.java.vbel.trashlocator.dto.PointMarker;
import com.java.vbel.trashlocator.fragments.CategoryFragment;
import com.java.vbel.trashlocator.network.NetworkService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, CategoryAdapter.GetCategoryFromDialog {


    private String currentPhotoPath;
    private int CAMERA = 2;


    private static final String TAG = "MainActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;


    //vars and widgets
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Marker mMarker;
    private ImageView mGps;

    //base url for api
    private String BASE_TEST_URL = "https://server-trash-optimizator.herokuapp.com/";

    //coordinates and category id of new marker
    private double[] coordinates = new double[2];
    private long categoryId;
    private String categoryTitle;

    //Category fragment
    private CategoryFragment categoryFragment;
    private ArrayList<CategoryItem> categoryItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Подготовка данных для диалога с категориями
        categoryFragment = new CategoryFragment();
        //prepareCategoryData();
        prepareCategoryData();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("categoryItems", categoryItems);
        categoryFragment.setArguments(bundle);


        //////////////////////////////////
        Button button = findViewById(R.id.adminButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
            }
        });
        //////////////////////////////////

        mGps = findViewById(R.id.ic_gps);

        FirebaseApp.initializeApp(this);
        FirebaseApp.getInstance();

        //App name label
        TextView textView = findViewById(R.id.textView);
        textView.setText(R.string.app_name);

        //Photo button
        ImageButton cameraButton = findViewById(R.id.imageButtonCamera);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMarker == null) Toast.makeText(MainActivity.this, "Пожалуйста, добавьте маркер!", Toast.LENGTH_SHORT).show();
                else{
                    //Вызов диалога со списком категорий мусора
                    if(categoryItems.size() != 0)categoryFragment.show(getSupportFragmentManager(), "category");
                    else Toast.makeText(MainActivity.this, "Загрузка категорий...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        getLocationPermission();
    }

    //region Categories
    private void prepareCategoryData(){
        NetworkService.getInstance(BASE_TEST_URL)
                .getTestApi()
                .getAllCategories()
                .enqueue(new Callback<List<CategoryItem>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<CategoryItem>> call, @NonNull Response<List<CategoryItem>> response) {

                        //Костыльно, нужен рефакторинг
                        try {
                            long[] categoryImages = {R.drawable.trash_small, R.drawable.trash_mid, R.drawable.trash_small};
                            for (int i = 0; i < response.body().size(); i++) {
                                CategoryItem categoryItem = response.body().get(i);
                                try {
                                    categoryItem.setImage(categoryImages[i]);
                                } catch (IndexOutOfBoundsException e) {
                                    categoryItem.setImage(R.drawable.trash_default);
                                }

                                categoryItems.add(categoryItem);
                            }
                        } catch (NullPointerException e){
                            prepareDefaultCategories();
                        }


                    }

                    @Override
                    public void onFailure(@NonNull Call<List<CategoryItem>> call, @NonNull Throwable t) {
                        prepareDefaultCategories();
                        if(t.getClass() == UnknownHostException.class)
                            Toast.makeText(MainActivity.this, "Не удалось загрузить категории", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Ошибка поключения к серверу", Toast.LENGTH_SHORT).show();
                        t.printStackTrace();
                    }
                });

    }

    private void prepareDefaultCategories(){
        long[] categoryIds = {1, 2, 3};
        String[] categoryTitles = {"Мелкий мусор", "Куча мусора", "Свалка", };
        String[] categoryDescriptions = {"Немного мусора вне урны", "Большое количество скопившегося мусора", "Огромная куча бытовых или строительных отходов"};
        long[] categoryImages = {R.drawable.trash_small, R.drawable.trash_mid, R.drawable.trash_big};
        for(int i = 0; i < categoryIds.length; i++){
            CategoryItem categoryItem = new CategoryItem();
            categoryItem.setId(categoryIds[i]);
            categoryItem.setTitle(categoryTitles[i]);
            categoryItem.setDescription(categoryDescriptions[i]);
            categoryItem.setImage(categoryImages[i]);
            categoryItems.add(categoryItem);
        }
    }

    @Override
    public void getCategoryFromDialog(Long id, String title) {
        Toast.makeText(MainActivity.this, "Категория:  "+id, Toast.LENGTH_SHORT).show();
        categoryId = id;
        categoryTitle = title;
        takePhoto();
    }
    //endregion

    //region Map
    public void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        mMap = googleMap;
        getPoints();

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            //Set onClick to gps button
            mGps.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick: clicked gps icon");
                    getDeviceLocation();
                }
            });

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            //init();
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d(TAG, "onMapClick");

                if (mMarker != null) mMarker.remove();
                mMarker = mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                coordinates[0] = mMarker.getPosition().latitude;
                coordinates[1] = mMarker.getPosition().longitude;

                Toast.makeText(MainActivity.this, latLng.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getDeviceLocation(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionsGranted){
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Location currentLocation = (Location) task.getResult();

                            try {
                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                        DEFAULT_ZOOM,
                                        "My Location");
                            }catch (NullPointerException e){
                                //Moscow
                                moveCamera(new LatLng(55.751244, 37.618423),
                                        DEFAULT_ZOOM,
                                        "My Location");
                            }

                        }else{
                            Toast.makeText(MainActivity.this, "Невозможно определить местоположение", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")){
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }

        hideSoftKeyboard();
    }

    //region Permissions
    private void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;
                    //initialize map
                    initMap();
                }
            }
        }
    }
    //endregion

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    //endregion

    //region Camera

    private void takePhoto(){
        Intent callCameraApplicationIntent = new Intent();
        callCameraApplicationIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = null;
        try{
            photoFile = createImageFile();
        }catch(IOException e){
            e.printStackTrace();
        }

        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.vbel.android.fileprovider",
                    photoFile);
            callCameraApplicationIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            callCameraApplicationIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(callCameraApplicationIntent, CAMERA);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == CAMERA && resultCode == RESULT_OK){
            galleryAddPic();
            final Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            getMLLabelsFromImage(bitmap);
            //Костыль
            mMap.setOnMarkerClickListener(null);
        }
    }

    protected File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAGE_" + timeStamp + "";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
//endregion

    //region Points (Markers)
    private void getPoints(){
        NetworkService.getInstance(BASE_TEST_URL)
                .getTestApi()
                .getAllPoints()
                .enqueue(new Callback<List<PointMarker>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<PointMarker>> call, @NonNull Response<List<PointMarker>> response) {

                        setMarkers(response.body());

                    }

                    @Override
                    public void onFailure(@NonNull Call<List<PointMarker>> call, @NonNull Throwable t) {
                        if(t.getClass() == UnknownHostException.class)
                            Toast.makeText(MainActivity.this, "Проверьте соединение с интернетом!", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show();
                        t.printStackTrace();
                    }
                });
    }


    private void setMarkers(List<PointMarker> points){
        for(PointMarker point: points){
            Marker marker =  mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(point.getLat(),point.getLng())));
            marker.setTag(point.getId());
        }
        mMap.setOnMarkerClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() != null) {
            if ((long) marker.getTag() != (long)-1) getPointInfo(marker);
        }
        return false;
    }


    private void getPointInfo(final Marker marker){
        long pointId = (long)marker.getTag();
        NetworkService.getInstance(BASE_TEST_URL)
                .getTestApi()
                .getPoint(pointId)
                .enqueue(new Callback<PointInfo>() {
                    @Override
                    public void onResponse(@NonNull Call<PointInfo> call, @NonNull Response<PointInfo> response) {
                        try {
                            setMarkerInfo(response.body(), marker);
                        }catch (NullPointerException e){
                            Toast.makeText(MainActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PointInfo> call, @NonNull Throwable t) {
                        if(t.getClass() == UnknownHostException.class)
                            Toast.makeText(MainActivity.this, "Проверьте соединение с интернетом!", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show();
                        t.printStackTrace();
                    }
                });
    }

    private void setMarkerInfo(PointInfo pointInfo, Marker marker) throws NullPointerException{
        marker.setTitle(pointInfo.getCategoryTitle());
        marker.setSnippet(pointInfo.getUserName() + ", " +pointInfo.getDate());
        marker.showInfoWindow();
        marker.setTag((long)-1);
    }
    //endregion

    //region ML + intent to result
    public void getMLLabelsFromImage(Bitmap bitmap) {

        if (bitmap != null) {
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();
            labeler.processImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionImageLabel> labels) {

                            ArrayList<String> labelArray = new ArrayList<>();
                            for (FirebaseVisionImageLabel label: labels) {
                                String text = label.getText();
                                String entityId = label.getEntityId();
                                float confidence = label.getConfidence();
                                labelArray.add(text);
                            }
                            goToResultActivity(labelArray);

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            goToResultActivity(null);
                        }
                    });
        } else {
            goToResultActivity(null);
        }
    }


    private void goToResultActivity(ArrayList<String> labelArray){
        Intent resultActivityIntent = new Intent(MainActivity.this, ResultActivity.class);
        resultActivityIntent.putExtra("coordinates", coordinates);
        resultActivityIntent.putExtra("categoryId", categoryId);
        resultActivityIntent.putExtra("categoryTitle", categoryTitle);
        resultActivityIntent.putExtra("labels", labelArray); //Could be null
        startActivity(resultActivityIntent);
    }


    //endregion


}
