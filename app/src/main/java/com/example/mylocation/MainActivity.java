package com.example.mylocation;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    private static final int CHECK_CODE = 1;
    private static final int REQUEST_LOCATION_PERMISSON = 2;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private SettingsClient settingsClient;
    private LocationSettingsRequest locationSettingsRequest;
    private Location location;
    private LocationCallback locationCallback;
    private boolean islocationUpdateActive = false;
    TextView country, city, address, longitude, latitude;
    Button getLocation;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        country = findViewById(R.id.country);
        city = findViewById(R.id.city);
        address = findViewById(R.id.address);
        longitude = findViewById(R.id.longitude);
        latitude = findViewById(R.id.lagitude);
        getLocation = findViewById(R.id.get_location_btn);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);


        getLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (islocationUpdateActive) {
                    stopLocationUpdates();
                } else {
                    startLocationUpdates();
                }
            }
        });

        buildLocationRequest();
        buildLocationCallback();
        buildLocationSettingsRequest();
    }

    private void startLocationUpdates() {
        islocationUpdateActive = true;
        getLocation.setText("Stop");

        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(MainActivity.this,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        fusedLocationProviderClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                        );

                        updateLocationUi();
                    }
                }).
                addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();

                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                    resolvableApiException.startResolutionForResult(MainActivity.this, CHECK_CODE);
                                } catch (IntentSender.SendIntentException sie) {
                                    sie.printStackTrace();
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                islocationUpdateActive = false;
                                getLocation.setText("Get location");
                                Toast.makeText(MainActivity.this, "Ruxsatlarni tekshiring!!!", Toast.LENGTH_SHORT).show();
                        }
                        updateLocationUi();
                    }
                });
    }

    private void updateLocationUi() {
        if (location != null) {
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses;
            try {

                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    latitude.setText(String.valueOf(addresses.get(0).getLatitude()));
                    longitude.setText(String.valueOf(addresses.get(0).getLongitude()));
                    address.setText(String.valueOf(addresses.get(0).getAddressLine(0)));
                    city.setText(String.valueOf(addresses.get(0).getLocality()));
                    country.setText(String.valueOf(addresses.get(0).getCountryName()));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void buildLocationRequest() {
        locationRequest = new com.google.android.gms.location.LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                location = locationResult.getLastLocation();

                updateLocationUi();

            }
        };
    }

    private void stopLocationUpdates() {
        if (!islocationUpdateActive) {
            return;
        }

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        islocationUpdateActive = false;
                        getLocation.setText("Get Location");
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (islocationUpdateActive && checkLocationPermissions()) {
            startLocationUpdates();
        } else if (!checkLocationPermissions()) {
            requestLocationPermissons();
        }
    }

    private void requestLocationPermissons() {

        boolean requestTek = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (requestTek) {
            showSnackBar("Dastur ishlashi uchun locatsiyani qo'shish kerak", "Ok",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSON);
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSON);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSON) {
            if (grantResults.length <= 0) {

            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (islocationUpdateActive) {
                    startLocationUpdates();
                } else {

                }
            }
        }


    }

    private void showSnackBar(final String messageText, final String buttonText, View.OnClickListener listener) {
        Snackbar.make(
                        findViewById(android.R.id.content), messageText, Snackbar.LENGTH_INDEFINITE)
                .setAction(buttonText, listener).show();

    }

    private boolean checkLocationPermissions() {
        int permisson = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permisson == PackageManager.PERMISSION_GRANTED;
    }
}