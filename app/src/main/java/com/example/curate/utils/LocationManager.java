package com.example.curate.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.parse.ParseGeoPoint;

import java.util.function.Function;

public class LocationManager {
    public static final int PERMISSION_REQUEST_CODE = 7;
    private static final String[] LOCATION_PERMISSION = {Manifest.permission.ACCESS_FINE_LOCATION};

    private FusedLocationProviderClient mFusedLocationClient;
    private Context mContext;
    private Fragment mFragment;

    public LocationManager(Context context) {
        mContext = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
    }

    public LocationManager(Fragment fragment) {
        mFragment = fragment;
        mContext = mFragment.getContext();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
    }

    public boolean hasNecessaryPermissions() {
        return ContextCompat.checkSelfPermission(mContext, LOCATION_PERMISSION[0])
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermissions() {
        if(mFragment == null) {
            ActivityCompat.requestPermissions((Activity) mContext,
                    LOCATION_PERMISSION, PERMISSION_REQUEST_CODE);
        } else {
            mFragment.requestPermissions(LOCATION_PERMISSION, PERMISSION_REQUEST_CODE);
        }
    }

    public void getCurrentLocation(OnSuccessListener<Location> callback) throws PermissionError {
        if (ContextCompat.checkSelfPermission(mContext, LOCATION_PERMISSION[0])
                != PackageManager.PERMISSION_GRANTED) {
            throw new PermissionError();
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(callback);
    }

    public void registerLocationUpdateCallback(LocationCallback callback) throws PermissionError {
        if (ContextCompat.checkSelfPermission(mContext, LOCATION_PERMISSION[0])
                != PackageManager.PERMISSION_GRANTED) {
            throw new PermissionError();
        }

        // Create a request type that balances power and accuracy
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        request.setFastestInterval(60000L);
        request.setInterval(3600000L);

        mFusedLocationClient.requestLocationUpdates(request, callback, mContext.getMainLooper());
    }

    public void deregisterLocationUpdateCallback(LocationCallback callback) {
        mFusedLocationClient.removeLocationUpdates(callback);
    }

    public static ParseGeoPoint createGeoPointFromLocation(Location location) {
        ParseGeoPoint parseLocation = new ParseGeoPoint();
        parseLocation.setLatitude(location.getLatitude());
        parseLocation.setLongitude(location.getLongitude());
        return parseLocation;
    }

    public static class PermissionError extends Error {

    }
}
