package com.example.y2793623b.map_fotos_pro;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.os.Bundle;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;

import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.client.annotations.Nullable;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private MapView map;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    private CompassOverlay mCompassOverlay;
    //private MinimapOverlay mMinimapOverlay;
    private RadiusMarkerClusterer locationsMarkers;
    private ArrayList<Locations> locationsList = new ArrayList<>();

    private Gps gps;
    double longitude;
    double latitude;

    private Button takeFoto;
    private Button takeVideo;


    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);


        Firebase.setAndroidContext(this.getContext());

        map = (MapView) view.findViewById(R.id.mapView);
        takeFoto = (Button) view.findViewById(R.id.bt_foto);
        takeVideo = (Button) view.findViewById(R.id.bt_video);

        initializeMap();
        setZoom();
        setOverlays();

        map.invalidate();

        takeFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        takeVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakeVideoIntent();
            }
        });

        return view;
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    //Agreguem els items de menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id == R.id.action_markers)
        {
            putMarkers();
        }

        return super.onOptionsItemSelected(item);
    }





    private void initializeMap() {

        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setTilesScaledToDpi(true);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

    }


    private void setZoom() {

        //  Setteamos el zoom al mismo nivel y ajustamos la posición a un geopunto
        mapController = map.getController();
        mapController.setZoom(14);

    }


    private void setOverlays() {

        final DisplayMetrics dm = getResources().getDisplayMetrics();


        myLocationOverlay = new MyLocationNewOverlay(
                getContext(),
                new GpsMyLocationProvider(getContext()),
                map
        );
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
                mapController.animateTo( myLocationOverlay.getMyLocation());
            }
        });


        mScaleBarOverlay = new ScaleBarOverlay(map);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);

        mCompassOverlay = new CompassOverlay(
                getContext(),
                new InternalCompassOrientationProvider(getContext()),
                map
        );
        mCompassOverlay.enableCompass();

        map.getOverlays().add(myLocationOverlay);
        //map.getOverlays().add(this.mMinimapOverlay);
        map.getOverlays().add(this.mScaleBarOverlay);
        map.getOverlays().add(this.mCompassOverlay);


    }


    public void putMarkers(){

        locationsList.clear();
        setupMarkerOverlay();

        Firebase dataBase = new Firebase("https://mapfotospro.firebaseio.com");
        Firebase contenido = dataBase.child("Locations-path");

        contenido.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot Snapshot : dataSnapshot.getChildren()) {

                    Locations loc = Snapshot.getValue(Locations.class);

                    locationsList.add(loc);

                    Marker marker = new Marker(map);

                    GeoPoint point = new GeoPoint(loc.lat,loc.lon);

                    marker.setPosition(point);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setIcon(getResources().getDrawable(R.drawable.imageareaclose));
                    marker.setTitle(loc.path);
                    marker.setAlpha(0.6f);

                    locationsMarkers.add(marker);
                }
                locationsMarkers.invalidate();
                map.invalidate();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


    }


    private void setupMarkerOverlay() {

        locationsMarkers = new RadiusMarkerClusterer(getContext());
        map.getOverlays().add(locationsMarkers);

        Drawable clusterIconD = getResources().getDrawable(R.drawable.imagemultiple);
        Bitmap clusterIcon = ((BitmapDrawable)clusterIconD).getBitmap();

        locationsMarkers.setIcon(clusterIcon);
        locationsMarkers.setRadius(100);

    }

    //Fem La Foto
    static final int REQUEST_TAKE_PHOTO = 1;
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            photoFile = createImageFile();
            // Continue only if the File was successfully created
            if (photoFile != null) {

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                addLocation(photoFile.toString());
            }
        }
    }


    //Video
    static final int REQUEST_TAKE_VIDEO = 2;
    private void dispatchTakeVideoIntent() {

        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takeVideoIntent.resolveActivity(getContext().getPackageManager()) != null) {

            // Create the File where the photo should go
            File videoFile = null;
            videoFile = createVideoFile(REQUEST_TAKE_VIDEO);

            // Continue only if the File was successfully created
            if (videoFile != null) {
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(videoFile));
                startActivityForResult(takeVideoIntent, REQUEST_TAKE_VIDEO);
                addLocation(videoFile.toString());
            }
        }
    }


    //Creem un fitxer on guardar la foto
    String mCurrentPhotoPath;
    private static final int ACTIVITAT_SELECCIONAR_IMATGE = 1;
    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  // prefix
                    ".jpg",         // suffix
                    storageDir      // directory
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.d(" photo path  -------------------- : " , mCurrentPhotoPath );

        Intent i = new Intent(
                Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        startActivityForResult(i, ACTIVITAT_SELECCIONAR_IMATGE);

        return image;
    }


    private File createVideoFile(int requestTakeVideo) {
        // create a video file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String videoFileName = "MP4_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File video = null;
        try {
            video = File.createTempFile(
                    videoFileName,  // prefix
                    ".mp4",         // suffix
                    storageDir      // directory
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return video;
    }


    private void addLocation(String ruta) {

        Firebase ref = new Firebase("https://mapfotospro.firebaseio.com/Locations-path");

        gps = new Gps(getContext());
        if(gps.canGetLocation())
        {
            longitude = gps.getLongitude();
            latitude = gps.getLatitude();

            //Creating object
            final Locations locat = new Locations();

            //Adding values
            locat.lon = longitude;
            locat.lat = latitude;
            locat.path = ruta;

            //Storing values to firebase
            ref.push().setValue(locat);

        }else {gps.showSettingsAlert();}




    }

}
