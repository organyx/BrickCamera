package com.example.aleks.brickcamera;

import android.media.ExifInterface;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;

public class SnapshotActivity extends AppCompatActivity implements GoogleMap.OnMarkerClickListener, OnMapReadyCallback, View.OnClickListener {

    private GoogleMap gmap;
    private Button btnDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snapshot);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_snapshot, menu);
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
    public void onClick(View v) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setOnMarkerClickListener(this);

        findImagesWithGeoTagAndAddToGmap(googleMap);
    }

    private void findImagesWithGeoTagAndAddToGmap(GoogleMap googleMap) {
        String storageState = Environment.getExternalStorageState();
        if(storageState.equals(Environment.MEDIA_MOUNTED))
        {
            File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            pictureDir = new File(pictureDir, "BrickCamera");

            if(pictureDir.exists())
            {
                File[] files = pictureDir.listFiles();
                for(File file : files)
                {
                    if(file.getName().endsWith(".jpg"))
                    {
                        LatLng pos = getLatLongFromExif(file.getAbsolutePath());

                        if(pos != null)
                        {
                            addGeoTag(pos, file.getName(), gmap);
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
        Toast.makeText(this, "Image name: " + marker.getTitle(), Toast.LENGTH_LONG).show();
        return true;
    }
}
