package com.gtxreme.uberclone;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.gtxreme.uberclone.Common.Common;
import com.gtxreme.uberclone.Remote.IGoogleAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Weclome extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private static final int MY_REQUEST_CODE = 7;
    private static final int PLAY_SERVICES_CODE = 8;

    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private IGoogleAPI mService;
    private Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {
            if (index < polyLineList.size() - 1) {
                index++;
                next = index + 1;

            }
            if (index < polyLineList.size() - 1) {
                startPos = polyLineList.get(index);
                endPos = polyLineList.get(next);

            }
            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    v = valueAnimator.getAnimatedFraction();
                    lng = v * endPos.longitude + (1 - v) * startPos.longitude;
                    lat = v * endPos.latitude + (1 - v) * startPos.latitude;
                    LatLng newPos = new LatLng(lat, lng);
                    carMarker.setPosition(newPos);
                    carMarker.setAnchor(0.5f, 0.5f);
                    carMarker.setRotation(getBearing(startPos, endPos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(newPos).zoom(15.5f).build()));

                }
            });
            valueAnimator.start();
            handler.postDelayed(this, 3000);

        }
    };

    private float getBearing(LatLng startPos, LatLng endPos) {
        double lat = Math.abs(startPos.latitude - endPos.latitude);
        double lng = Math.abs(startPos.longitude - endPos.longitude);

        if (startPos.latitude < endPos.latitude && startPos.longitude < endPos.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));

        else if (startPos.latitude >= endPos.latitude && startPos.longitude < endPos.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);

        else if (startPos.latitude >= endPos.latitude && startPos.longitude >= endPos.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);

        else if (startPos.latitude < endPos.latitude && startPos.longitude >= endPos.longitude)
            return (float) (90 - Math.toDegrees(Math.atan(lng / lat)) + 270);
        return -1;
    }


    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference drivers;
    GeoFire geoFire;
    Marker mCurrent;
    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;

    //car Animation

    private List<LatLng> polyLineList;
    private Marker carMarker;
    public float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPos, endPos, currentPos;
    private int index, next;
    private Button btnGo;
    private EditText edtPlace;
    private String destination;
    private PolylineOptions blackOpt, polylineOptions;
    private Polyline blackPolyline, greyPolyline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weclome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        location_switch = findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {
                if (isOnline) {
                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(), "You are now online", Snackbar.LENGTH_SHORT).show();
                }
                else {
                    stopLocationUpdates();
                    mMap.clear();
                    handler.removeCallbacks(drawPathRunnable);
                    Snackbar.make(mapFragment.getView(), "You are now offline", Snackbar.LENGTH_SHORT).show();
                    mCurrent.remove();
                }
            }
        });
        polyLineList = new ArrayList<>();
        btnGo = findViewById(R.id.btnGo);
        edtPlace = findViewById(R.id.edtPlace);

        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destination = edtPlace.getText().toString();
                destination = destination.replace(" ", "+");
                Log.d("Gtxtreme", destination);

                getDirection();
            }
        });

        drivers = FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire = new GeoFire(drivers);

        setUpLocation();
        mService = Common.getGoogleAPI();


    }

    private void getDirection() {
        currentPos = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        String reqAPI = null;
        try {
            reqAPI = "https://maps.googleapis.com/maps/api/directions/json?" + "mode=driving&" + "transit_routing_preference=less_driving&" +
                    "origin=" + currentPos.latitude + "," + currentPos.longitude + "&" + "destination=" + destination + "&" + "key=" + getResources().getString(R.string.google_maps_api_key);
            Log.d("gtxtreme.uberclone", reqAPI);
            mService.getPath(reqAPI).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polyLineList = decodePoly(polyline);

                            //Adjusting Bounds
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            for (LatLng latLng : polyLineList)
                                builder.include(latLng);
                            LatLngBounds bounds = builder.build();
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);

                            polylineOptions = new PolylineOptions().color(Color.GRAY).width(5).startCap(new SquareCap()).endCap(new SquareCap()).jointType(JointType.ROUND).addAll(polyLineList);
                            greyPolyline = mMap.addPolyline(polylineOptions);
                            blackOpt = new PolylineOptions().color(Color.BLACK).width(5).startCap(new SquareCap()).endCap(new SquareCap()).jointType(JointType.ROUND).addAll(polyLineList);
                            blackPolyline = mMap.addPolyline(blackOpt);

                            mMap.addMarker(new MarkerOptions().position(polyLineList.get(polyLineList.size() - 1)).title("Pickup Location"));

                            //Animations

                            ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0, 100);
                            polyLineAnimator.setDuration(2000);
                            polyLineAnimator.setInterpolator(new LinearInterpolator());
                            polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    List<LatLng> points = greyPolyline.getPoints();
                                    int percentValue = (int) animation.getAnimatedValue();
                                    int size = points.size();
                                    int newpoints = (int) (size * (percentValue / 100.0f));
                                    List<LatLng> p = points.subList(0, newpoints);
                                    blackPolyline.setPoints(p);
                                }
                            });


                            polyLineAnimator.start();

                            carMarker = mMap.addMarker(new MarkerOptions().position(currentPos).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                            handler = new Handler();
                            index = -1;
                            next = -1;
                            handler.postDelayed(drawPathRunnable, 3000);


                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildGoogleApiClient();
                        createLocationRequest();
                        if (location_switch.isChecked())
                            displayLocation();
                    }
                }
        }

    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, MY_REQUEST_CODE);
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                if (location_switch.isChecked())
                    displayLocation();
            }
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_CODE).show();
            } else {
                Toast.makeText(Weclome.this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }
        });
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, MY_REQUEST_CODE);
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
            if (location_switch.isChecked()) {
                final double longitude = mLastLocation.getLongitude();
                final double latitude = mLastLocation.getLatitude();

                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (mCurrent != null)
                            mCurrent.remove();
                        mCurrent = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car)).position(new LatLng(latitude, longitude)).title("You"));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));
                        rotateMarker(mCurrent, -360, mMap);

                    }
                });

            }
    }

    private void rotateMarker(final Marker mCurrent, final int i, GoogleMap mMap) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = mCurrent.getRotation();
        final long duration = 1500;
        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                float rot = t * i + (1 - t) * startRotation;
                mCurrent.setRotation(-rot > 180 ? rot / 2 : rot);
                if (t < 1.0) {
                    handler.postDelayed(this, 16);

                }

            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }
        });


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
