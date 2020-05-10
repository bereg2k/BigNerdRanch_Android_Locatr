package com.bignerdranch.android.locatr.fragment;

import android.Manifest;
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
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bignerdranch.android.locatr.R;
import com.bignerdranch.android.locatr.model.GalleryItem;
import com.bignerdranch.android.locatr.util.FlickrFetchr;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static androidx.core.content.ContextCompat.getColor;


/**
 * Main fragment to host images found by current location
 * <p>
 * For location mocker:
 * Get Mock Walker APK at
 * <a href=https://www.bignerdranch.com/solutions/MockWalker.apk>
 * https://www.bignerdranch.com/solutions/MockWalker.apk</a>
 */
public class LocatrFragment extends Fragment {
    private static final String TAG = LocatrFragment.class.getSimpleName();

    private static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    private static final int REQUEST_LOCATION_PERMISSIONS = 0;
    private static final int REQUEST_RATIONALE = 1;
    private static final String RATIONALE_DIALOG_TAG = "RationaleFragment";

    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private GoogleApiClient mClient;

    public static Fragment newInstance() {
        return new LocatrFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

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

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_locatr, container, false);

        mImageView = view.findViewById(R.id.image);

        mProgressBar = view.findViewById(R.id.progress_bar);
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
                .requestLocationUpdates(mClient, locationRequest, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        Log.i(TAG, "Got a location fix: " + location);
                        new SearchTask().execute(location);
                    }
                });
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
     * Asynchronous request to search for a photo on Flickr at desired location
     */
    private class SearchTask extends AsyncTask<Location, Void, Void> {
        private GalleryItem mGalleryItem;
        private Bitmap mBitmap;

        @Override
        protected void onPreExecute() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Location... locations) {
            List<GalleryItem> items = new FlickrFetchr().searchPhotos(locations[0]);

            if (items.isEmpty()) {
                Log.i(TAG, "No images at current location! :( - " + locations[0]);
                return null;
            }

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
            mProgressBar.setVisibility(View.GONE);
            mImageView.setImageBitmap(mBitmap);
        }
    }
}
