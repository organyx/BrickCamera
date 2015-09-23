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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ImageView ivLastPic;
    private TextView tvOrientation;
    private TextView tvLong;
    private TextView tvLat;

    private static final int REQUEST_CODE = 123;
    public static final String SAVED_PREFERENCES = "SAVED_PREFERENCES";
    public static final String SAVED_PICTURE_PATH = "SAVED_PICTURE_PATH";

    private File pictureDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivLastPic = (ImageView) findViewById(R.id.ivLastPicture);
        tvOrientation = (TextView) findViewById(R.id.tvOrientationValue);
        tvLat = (TextView) findViewById(R.id.tvLatValue);
        tvLong = (TextView) findViewById(R.id.tvLongValue);

        if(isExternalStorageReadable() && isExternalStorageWritable())
            Toast.makeText(this, "Can do stuff", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "Can't do stuff", Toast.LENGTH_LONG).show();

        pictureDirectory = getMyPicDirectory();
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

        if(requestCode == REQUEST_CODE)
        {
            if(resultCode == RESULT_OK)
            {
                String filename = loadLastAttemptedImageCaptureFilename();

                setPictureToSize(filename, ivLastPic);
            }
            if(resultCode == RESULT_CANCELED)
            {
                setPictureToSize(loadLastAttemptedImageCaptureFilename(), ivLastPic);
            }
        }

    }

    private String loadLastAttemptedImageCaptureFilename() {
        SharedPreferences prefs = getSharedPreferences(SAVED_PREFERENCES, MODE_PRIVATE);
        String saved_path = prefs.getString(SAVED_PICTURE_PATH, "DEFAULT PATH");
        Log.d("FILE_PATH", "Loaded value: " + saved_path);
        return saved_path;
    }

    private void saveLastAttemptedImageCaptureFilename(String filename) {
        SharedPreferences prefs = getSharedPreferences(SAVED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SAVED_PICTURE_PATH, filename);
        Log.d("FILE_PATH", "Saved value: " + filename);
        editor.apply();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        String filename = loadLastAttemptedImageCaptureFilename();

        setPictureToSize(filename, ivLastPic);
    }

    public boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }

    public boolean isExternalStorageReadable()
    {
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
            return true;
        return false;
    }

    private void setPictureToSize(String filename, ImageView iv)
    {
        int targetW = 200;
        int targetH = 200;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        Bitmap bitmap = BitmapFactory.decodeFile(filename, bmOptions);

        iv.setRotation(90);

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(exif.getAttribute(ExifInterface.TAG_ORIENTATION) != null)
            tvOrientation.setText(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
        tvLat.setText(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) + " " + exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
        tvLong.setText(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) + " " + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));

        iv.setImageBitmap(bitmap);
    }

    private File getMyPicDirectory()
    {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "BrickCamera");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (checkMyPicDirectory(mediaStorageDir))
            return mediaStorageDir;
        else
            return null;
    }

    private boolean checkMyPicDirectory(File filename)
    {
        if (! filename.exists()){
            if (! filename.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return false;
            }
        }
        return true;
    }

    public void onBtnTakePicClick(View view) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = pictureDirectory.getPath() + File.separator+"IMG_"+timeStamp+".jpg";
        File imageFile = new File(filename);
        Uri imageUri = Uri.fromFile(imageFile);

        saveLastAttemptedImageCaptureFilename(filename);

        takePicture(imageUri);
    }

    public void takePicture(Uri filepath)
    {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, filepath);
        if(cameraIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(cameraIntent, REQUEST_CODE);
    }
}
