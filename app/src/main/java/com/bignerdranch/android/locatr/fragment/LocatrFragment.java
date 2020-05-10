package com.bignerdranch.android.locatr.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.bignerdranch.android.locatr.R;
import com.bignerdranch.android.locatr.model.GalleryItem;
import com.bignerdranch.android.locatr.util.FlickrFetchr;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

import static androidx.core.content.ContextCompat.getColor;


/**
 * Main fragment to host a Google Map with current location and images from Flickr found nearby.
 * <p>
 * For location mocker:
 * Get Mock Walker APK at
 * <a href=https://www.bignerdranch.com/solutions/MockWalker.apk>
 * https://www.bignerdranch.com/solutions/MockWalker.apk</a>
 */
public class LocatrFragment extends SupportMapFragment {
    private static final String TAG = LocatrFragment.class.getSimpleName();

    private static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    private static final int REQUEST_LOCATION_PERMISSIONS = 0;
    private static final int REQUEST_RATIONALE = 1;
    private static final String RATIONALE_DIALOG_TAG = "RationaleFragment";

    private ProgressBar mProgressBar;
    private GoogleApiClient mClient;
    private GoogleMap mMap;
    private Bitmap mMapImage;
    private GalleryItem mMapItem;
    private Location mCurrentLocation;

    public static SupportMapFragment newInstance() {
        return new LocatrFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Initialize a Google API client to communicate with Google Services (Location, Maps)
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        // nothing
                    }
                })
                .build();

        getMapAsync(googleMap -> {
            mMap = googleMap;
            updateUI();
        });

    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View view = super.onCreateView(layoutInflater, viewGroup, bundle);

        // Adding my custom layout with Progress Bar to the Map's fragment layout
        FrameLayout frameLayout = (FrameLayout) view;
        View myView = layoutInflater.inflate(R.layout.fragment_locatr, viewGroup, false);
        frameLayout.addView(myView);

        mProgressBar = myView.findViewById(R.id.progress_bar);
        mProgressBar.getIndeterminateDrawable().setColorFilter(
                getColor(getContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_IN);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_locatr, menu);

        MenuItem searchItem = menu.findItem(R.id.action_locate);
        searchItem.setEnabled(mClient.isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_locate:
                if (hasLocationPermission()) {
                    findImage();
                } else {
                    initiateRequestPermissions();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSIONS:
                if (hasLocationPermission()) {
                    findImage();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        getActivity().invalidateOptionsMenu();
        mClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        mClient.disconnect();
    }

    /**
     * Find an image on Flickr near user's current location
     * by instantiating a location request to the location service.
     */
    private void findImage() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setInterval(0);

        LocationServices.FusedLocationApi
                .requestLocationUpdates(mClient, locationRequest, location -> {
                            Log.i(TAG, "Got a location fix: " + location);
                            new SearchTask(this).execute(location);
                        }
                );
    }

    /**
     * Check the app for having a permission to use user's current location
     *
     * @return true, if permission is already granted
     */
    private boolean hasLocationPermission() {
        int result = ContextCompat.checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request app permissions for accessing user's location.
     * <p> 1. If requesting for the 1st time - show system dialog. </p>
     * <p> 2. If user initially denies it or grants a permission "only for now" and reopens app -
     * show rationale dialog before system dialog. </p>
     * <p> 3. If user denies permissions "forever" - don't show any dialogs. </p>
     */
    private void initiateRequestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), LOCATION_PERMISSIONS[0])) {
            FragmentManager fm = getFragmentManager();
            RationaleFragment fragment = RationaleFragment.newInstance(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
            fragment.setTargetFragment(this, REQUEST_RATIONALE);
            fragment.show(fm, RATIONALE_DIALOG_TAG);
        } else {
            requestPermissions(LOCATION_PERMISSIONS,
                    REQUEST_LOCATION_PERMISSIONS);
        }
    }

    /**
     * Updating map and showing a zoomed-in version of it with 2 "points":
     * <p> - current location </p>
     * <p> - thumbnail of image at its location. </p>
     */
    private void updateUI() {
        if (mMap == null || mMapImage == null) {
            return;
        }

        LatLng itemPoint = new LatLng(mMapItem.getLat(), mMapItem.getLon());
        LatLng myPoint = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        BitmapDescriptor itemBitmap = BitmapDescriptorFactory.fromBitmap(mMapImage);
        MarkerOptions itemMarker = new MarkerOptions()
                .position(itemPoint)
                .icon(itemBitmap);
        MarkerOptions myMarker = new MarkerOptions()
                .position(myPoint);

        mMap.clear();
        mMap.addMarker(itemMarker);
        mMap.addMarker(myMarker);

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(itemPoint)
                .include(myPoint)
                .build();

        int margin = getResources().getDimensionPixelSize(R.dimen.map_inset_margin);
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, margin);
        mMap.animateCamera(update);

        // If user presses on the photo - open browser with full photo page.
        // Otherwise - default behavior (camera moves to the marker and an info window appears).
        mMap.setOnMarkerClickListener(marker -> {
            if (itemMarker.getPosition().latitude == marker.getPosition().latitude &&
                    itemMarker.getPosition().longitude == marker.getPosition().longitude) {
                Intent intent = new Intent(Intent.ACTION_VIEW, mMapItem.getPhotoPageUri());
                startActivity(intent);
                return true;
            } else {
                return false;
            }
        });
    }

    /**
     * Asynchronous request to search for a photo on Flickr at desired location.
     * This class is static with WeakReference to the instance of outer class.
     * This prevents possible memory leaks and allows task to safely access fragment's context.
     */
    private static class SearchTask extends AsyncTask<Location, Void, Void> {
        private GalleryItem mGalleryItem;
        private Bitmap mBitmap;
        private Location mLocation;

        private WeakReference<LocatrFragment> mFragmentReference;

        public SearchTask(LocatrFragment fragment) {
            mFragmentReference = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            mFragmentReference.get().mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Location... locations) {
            mLocation = locations[0];
            List<GalleryItem> items = new FlickrFetchr().searchPhotos(locations[0]);

            if (items.isEmpty()) {
                Log.i(TAG, "No images at current location! :( - " + locations[0]);
                return null;
            }

            // Getting a random image from all the images returned by request
            mGalleryItem = items.get(new Random().nextInt(items.size()));

            try {
                byte[] imageBytes = new FlickrFetchr().getUrlBytes(mGalleryItem.getUrl());
                mBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                Log.i(TAG, "Image \"" + mGalleryItem.getCaption() + "\" fetched via url: " + mGalleryItem.getUrl());
            } catch (IOException e) {
                Log.e(TAG, "Unable to download bitmap!", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mFragmentReference.get().mMapImage = mBitmap;
            mFragmentReference.get().mMapItem = mGalleryItem;
            mFragmentReference.get().mCurrentLocation = mLocation;

            mFragmentReference.get().mProgressBar.setVisibility(View.GONE);
            mFragmentReference.get().updateUI();
        }
    }
}
