package com.example.aleks.brickcamera;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GoogleMap.OnMarkerClickListener, OnMapReadyCallback, View.OnClickListener {

    private ImageView ivLastPic;
    private VideoView vvLastVid;
    private LinearLayout llInfo;
    private TextView tvOrientation;
    private TextView tvLong;
    private TextView tvLat;
    private Button btnPic;
    private Button btnVideo;
    private LinearLayout pop_up;
    private ImageView pop_up_image;

    private GoogleMap gmap;
    private Button btnDoc;

    private static final int PICTURE_REQUEST_CODE = 123;
    private static final int VIDEO_REQUEST_CODE = 321;
    public static final String SAVED_PICTURE_PREFERENCES = "SAVED_PICTURE_PREFERENCES";
    public static final String SAVED_VIDEO_PREFERENCES = "SAVED_VIDEO_PREFERENCES";
    public static final String SAVED_MODE_PREFERENCES = "SAVED_MODE_PREFERENCES";
    public static final String SAVED_PICTURE_PATH = "SAVED_PICTURE_PATH";
    public static final String SAVED_VIDEO_PATH = "SAVED_PICTURE_PATH";
    public static final String SAVED_MODE = "SAVED_MODE";

    // PICTURE MODE BY DEFAULT
    public boolean pictureMode = true;
//    public static final String SAVED_PICTURE_PREV_PATH = "SAVED_PICTURE_PREV_PATH";
//
//    private String picturePrevPath;

    private File pictureDirectory;
    private File videoDirectory;

    private String pictureDirectoryStr;
    private String videoDirectoryStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivLastPic = (ImageView) findViewById(R.id.ivLastPicture);
        vvLastVid = (VideoView) findViewById(R.id.vvLastVideo);
        llInfo = (LinearLayout) findViewById(R.id.llInfo);
        tvOrientation = (TextView) findViewById(R.id.tvOrientationValue);
        tvLat = (TextView) findViewById(R.id.tvLatValue);
        tvLong = (TextView) findViewById(R.id.tvLongValue);
        btnPic = (Button) findViewById(R.id.btnTakePic);
        btnVideo = (Button) findViewById(R.id.btnRecordVideo);
        pop_up = (LinearLayout) findViewById(R.id.pop_up_layout);
        pop_up_image = (ImageView) findViewById(R.id.pop_up_image);
        if(pop_up.getVisibility() == View.VISIBLE)
        {
            pop_up.setVisibility(View.INVISIBLE);
        }

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.googleMap);

        mapFragment.getMapAsync(this);
//        checkAndChangeMode();
        turnOnPicMode();

        if(isExternalStorageReadable() && isExternalStorageWritable())
            Toast.makeText(this, "Can do stuff", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "Can't do stuff", Toast.LENGTH_LONG).show();
        Log.d("MODE", "onCreate Current mode: " + pictureMode);
        initializeDirectory();
    }

    private void initializeDirectory() {
        Log.d("MODE", "initD Current mode: " + pictureMode);
        pictureDirectory = getMyPicDirectory();
        if(pictureDirectory != null)
            pictureDirectoryStr = pictureDirectory.getPath();

        videoDirectory = getMyVidDirectory();
        if(videoDirectory != null)
            videoDirectoryStr = videoDirectory.getPath();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICTURE_REQUEST_CODE)
        {
            Log.d("onActivityResult", "PICTURE_REQUEST_CODE");
            if(resultCode == RESULT_OK)
            {
                Log.d("onActivityResult", "RESULT_OK");
                String filename = loadLastAttemptedImageCaptureFilename();
//                saveMode();
//                pictureMode = loadMode();
                setPictureToSize(filename, ivLastPic);
            }
            if(resultCode == RESULT_CANCELED)
            {
                Log.d("onActivityResult", "RESULT_CANCELED");
//                if (data == null)
//                    setPictureToSize(picturePrevPath, ivLastPic);
            }
        }

        if(requestCode == VIDEO_REQUEST_CODE)
        {
            Log.d("onActivityResult", "VIDEO_REQUEST_CODE");
            if(resultCode == RESULT_OK)
            {
                Log.d("onActivityResult", "RESULT_OK");
                String filename = loadLastAttemptedVideoCaptureFilename();
//                pictureMode = loadMode();
                setVideo(filename, vvLastVid);
            }
            if(resultCode == RESULT_CANCELED)
            {
                Log.d("onActivityResult", "RESULT_CANCELED");
            }
        }

    }

    private String loadLastAttemptedImageCaptureFilename() {
        SharedPreferences prefs = getSharedPreferences(SAVED_PICTURE_PREFERENCES, MODE_PRIVATE);
        String saved_pic_path;
        Log.d("MODE", "Load. Current mode: " + pictureMode);
        File picStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "BrickCamera");
        saved_pic_path = prefs.getString(SAVED_PICTURE_PATH, picStorageDir + File.separator + "IMG_20150923_185826.jpg");
        return saved_pic_path;
    }

    private String loadLastAttemptedVideoCaptureFilename() {
        SharedPreferences prefs = getSharedPreferences(SAVED_VIDEO_PREFERENCES, MODE_PRIVATE);
        String saved_vid_path;
        File movStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "BrickCamera");
        saved_vid_path = prefs.getString(SAVED_VIDEO_PATH, movStorageDir + File.separator + "VID_20150923_185106.mp4");
        return saved_vid_path;
    }

    private void saveLastAttemptedImageCaptureFilename(String filename) {
        SharedPreferences prefs = getSharedPreferences(SAVED_PICTURE_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Log.d("MODE", "Save. Current mode: " + pictureMode);
        pictureDirectoryStr = filename;
        editor.putString(SAVED_PICTURE_PATH, pictureDirectoryStr);
        Log.d("MODE2", "Saved. Current mode: " + pictureMode);
        Log.d("FILE_PATH", "Saved value: " + filename);
        editor.apply();
    }

    private void saveLastAttemptedVideoCaptureFilename(String filename) {
        SharedPreferences prefs = getSharedPreferences(SAVED_VIDEO_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Log.d("MODE", "Save. Current mode: " + pictureMode);
        videoDirectoryStr = filename;
        editor.putString(SAVED_VIDEO_PATH, videoDirectoryStr);
        editor.apply();
    }

    private boolean loadMode()
    {
        SharedPreferences prefs = getSharedPreferences(SAVED_MODE_PREFERENCES, MODE_PRIVATE);
        boolean mode = prefs.getBoolean(SAVED_MODE, true);
        Log.d("MODE3", "loadMode. Current mode: " + pictureMode);
        return mode;
    }

    private void saveMode()
    {
        SharedPreferences prefs = getSharedPreferences(SAVED_MODE_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Log.d("MODE3", "saveMode. Current mode: " + pictureMode);
        editor.putBoolean(SAVED_MODE, pictureMode);
        editor.apply();
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d("MODE", "onResume. Current mode: " + pictureMode);
//        initializeDirectory(pictureMode);

        if(pop_up.getVisibility() == View.VISIBLE)
        {
            pop_up.setVisibility(View.INVISIBLE);
        }

        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(status == ConnectionResult.SUCCESS)
        {
            Toast.makeText(this, "Google Play is available", Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(this, "Google Play is not available", Toast.LENGTH_LONG).show();
        }
//        pictureMode = loadMode();

        String pic_filename = loadLastAttemptedImageCaptureFilename();
        String vid_filename = loadLastAttemptedVideoCaptureFilename();

//        String filename = loadLastAttemptedImageCaptureFilename();
        //String videoFilename = loadLastAttemptedImageCaptureFilename();
        setPictureToSize(pic_filename, ivLastPic);
        setVideo(vid_filename, vvLastVid);
//        setVideoToSize(videoFilename, vvLastVid);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

//        saveMode();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        pop_up.setVisibility(View.VISIBLE);
    }

    public boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isExternalStorageReadable()
    {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    private void setPictureToSize(String filename, ImageView iv)
    {
        Log.d("MODE", "setSize. Current mode: " + pictureMode);
        Log.d("setPictureToSize", "File: " + filename);

        String orientation = getExifInfo(filename);

        adjustPicOrientation(orientation, iv);

        if(pictureMode)
        {
            Bitmap bitmap = resizePicture(filename, 200, 150);

            iv.setImageBitmap(bitmap);
        }
    }

    private void setVideo(String filename, VideoView vv)
    {
        MediaController mc = new MediaController(this);
        mc.setAnchorView(vv);
        mc.setMediaPlayer(vv);

        vv.setMediaController(mc);
//            vv.setClickable(true);
//            vv.setEnabled(true);

        vv.setVideoPath(filename);
    }

    private String getExifInfo(String filename) {
        String orientation = null;
        String latValue;
        String latRef;
        String longValue;
        String longRef;

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(exif.getAttribute(ExifInterface.TAG_ORIENTATION) != null)
        {
            orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            latValue = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            longValue = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            longRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            tvOrientation.setText(orientation);
            tvLat.setText(latValue + " " + latRef);
            tvLong.setText(longValue + " " + longRef);
        }
        return orientation;
    }

    private File getMyPicDirectory()
    {
        Log.d("MODE", "getD. Current mode: " + pictureMode);
        File mediaStorageDir;

        mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "BrickCamera");

        if (checkMyDirectory(mediaStorageDir))
            return mediaStorageDir;
        else
            return null;
    }

    private File getMyVidDirectory()
    {
        Log.d("MODE", "getD. Current mode: " + pictureMode);
        File mediaStorageDir;

        mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "BrickCamera");

        if (checkMyDirectory(mediaStorageDir))
            return mediaStorageDir;
        else
            return null;
    }

    private boolean checkMyDirectory(File filename)
    {
        Log.d("MODE", "checkD. Current mode: " + pictureMode);
        if (! filename.exists()){
            if (! filename.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return false;
            }
        }
        return true;
    }

    public void onBtnTakePicClick(View view) {

        turnOnPicMode();
        initializeDirectory();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String filename = pictureDirectory.getPath() + File.separator+"IMG_"+timeStamp+".jpg";

        pictureDirectoryStr = filename;

        File imageFile = new File(filename);
        Uri imageUri = Uri.fromFile(imageFile);

        saveLastAttemptedImageCaptureFilename(filename);
//        saveMode();

        takePicture(imageUri);
    }

    public void takePicture(Uri filepath)
    {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, filepath);
        if(cameraIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(cameraIntent, PICTURE_REQUEST_CODE);
    }

    public void onBtnRecordVideoClick(View view) {
        turnOnMovMode();
        initializeDirectory();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String filename = videoDirectory.getPath() + File.separator+"VID_"+timeStamp+".mp4";

        videoDirectoryStr = filename;

        File videoFile = new File(filename);
        Uri videoUri = Uri.fromFile(videoFile);

//        saveLastAttemptedImageCaptureFilename(videoDirectoryStr);
        saveLastAttemptedVideoCaptureFilename(filename);
//saveMode();
        recordVideo(videoUri);
    }

    public void recordVideo(Uri filepath)
    {
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, filepath);
        videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        if(videoIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(videoIntent, VIDEO_REQUEST_CODE);
    }

    public void onBtnChangeModeClick(View view) {

        checkAndChangeMode();
    }

    public void checkAndChangeMode()
    {
        Log.d("MODE", "checkMode. Current mode: " + pictureMode);
        if( pictureMode )
        {
            turnOnMovMode();
        }
        else
        {
            turnOnPicMode();
        }
    }

    public void turnOnPicMode()
    {
        pictureMode = true;
        //HIDE VIDEO
        if (vvLastVid.getVisibility() == View.VISIBLE )
            vvLastVid.setVisibility(View.INVISIBLE);
        if (btnVideo.getVisibility() == View.VISIBLE)
            btnVideo.setVisibility(View.INVISIBLE);
        //SHOW PICTURE
        if (ivLastPic.getVisibility() == View.INVISIBLE)
            ivLastPic.setVisibility(View.VISIBLE);
        if (llInfo.getVisibility() == View.INVISIBLE)
            llInfo.setVisibility(View.VISIBLE);
        if (btnPic.getVisibility() == View.INVISIBLE)
            btnPic.setVisibility(View.VISIBLE);
        Log.d("MODE", "PICTURE MODE. Current mode: " + pictureMode);
    }

    public void turnOnMovMode()
    {
        pictureMode = false;

        //HIDE PICTURE
        if (ivLastPic.getVisibility() == View.VISIBLE)
            ivLastPic.setVisibility(View.INVISIBLE);
        if (llInfo.getVisibility() == View.VISIBLE)
            llInfo.setVisibility(View.INVISIBLE);
        if (btnPic.getVisibility() == View.VISIBLE)
            btnPic.setVisibility(View.INVISIBLE);
        //SHOW VIDEO
        if (btnVideo.getVisibility() == View.INVISIBLE )
            btnVideo.setVisibility(View.VISIBLE);
        if (vvLastVid.getVisibility() == View.INVISIBLE )
            vvLastVid.setVisibility(View.VISIBLE);
        Log.d("MODE", "MOVIE MODE. Current mode: " + pictureMode);
    }

    public void onVideoClick(View view) {
//        if(vvLastVid.isPlaying())
//            vvLastVid.stopPlayback();
//        if(!vvLastVid.isPlaying())
//        {
////            vvLastVid.setMediaController(new MediaController(this));
//            vvLastVid.requestFocus();
//            vvLastVid.start();
//        }
        vvLastVid.start();
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("GMAP", "onMapReady");
        googleMap.setOnMarkerClickListener(this);

        findImagesWithGeoTagAndAddToGmap(googleMap);
    }

    private void findImagesWithGeoTagAndAddToGmap(GoogleMap googleMap) {
        Log.d("GMAP", "findImagesWithGeoTagAndAddToGmap");
        String storageState = Environment.getExternalStorageState();
        if(storageState.equals(Environment.MEDIA_MOUNTED))
        {
            Log.d("GMAP", "Media Mounted");
            File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            pictureDir = new File(pictureDir, "BrickCamera");

            if(pictureDir.exists())
            {
                Log.d("GMAP", "Folder Found");
                File[] files = pictureDir.listFiles();
                for(File file : files)
                {
                    if(file.getName().endsWith(".jpg"))
                    {
                        Log.d("GMAP", "Found .jpgs");
                        LatLng pos = getLatLongFromExif(file.getAbsolutePath());

                        if(pos != null)
                        {
                            Log.d("GMAP", "Image with gtag: " + file.getName());
                            addGeoTag(pos, file.getName(), googleMap);
                        }
                    }
                }
            }
        }
    }

    private void addGeoTag(LatLng pos, String name, GoogleMap gmap) {
        gmap.setMyLocationEnabled(true);
        gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 13));
        gmap.addMarker(new MarkerOptions().position(pos)).setTitle(name);
    }

    private LatLng getLatLongFromExif(String absolutePath) {
        float latLong[] = new float[2];
        LatLng pos = null;
        try{
            ExifInterface exif = new ExifInterface(absolutePath);
            if(exif.getLatLong(latLong))
            {
                pos = new LatLng(latLong[0], latLong[1]);
            }
            else
            {
                return null;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return pos;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        pictureDir = new File(pictureDir, "BrickCamera" + File.separator + marker.getTitle());

        if(pop_up.getVisibility() == View.INVISIBLE)
        {
            TextView title = (TextView) pop_up.getChildAt(0);
            ImageView image = (ImageView) pop_up.getChildAt(1);

            title.setText(marker.getTitle());
            setPictureToPopUpSize(pictureDir.getPath(), image);

            pop_up.setVisibility(View.VISIBLE);
        }
        else
        {
            pop_up.setVisibility(View.INVISIBLE);
        }
        return true;
    }

    private void setPictureToPopUpSize(String filename, ImageView iv) {
        String orientation = getExifInfo(filename);

        adjustPicOrientation(orientation, iv);

        if(pictureMode)
        {
            Bitmap bitmap = resizePicture(filename, 200, 150);
            iv.setImageBitmap(bitmap);
        }
    }

    private Bitmap resizePicture(String filename, int width, int height) {

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW / width, photoH / height);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        return BitmapFactory.decodeFile(filename, bmOptions);
    }

    private void adjustPicOrientation(String orientation, ImageView iv) {
        switch (orientation)
        {
            case "1":
                iv.setRotation(360);
                break;
            case "3":
                iv.setRotation(180);
                break;
            case "6":
                iv.setRotation(90);
                break;
            case "8":
                iv.setRotation(270);
                break;
        }
    }

    public void onPopUpClick(View view) {
        if(pop_up.getVisibility() == View.VISIBLE)
        {
            pop_up.setVisibility(View.INVISIBLE);
        }
    }
}
