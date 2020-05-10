package com.bignerdranch.android.locatr.activity;

import android.app.Dialog;
import android.content.DialogInterface;

import androidx.fragment.app.Fragment;

import com.bignerdranch.android.locatr.fragment.LocatrFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Main activity to host {@link LocatrFragment}
 */
public class LocatrActivity extends SingleFragmentActivity {
    private static final int REQUEST_ERROR = 0;

    @Override
    public Fragment createFragment() {
        return LocatrFragment.newInstance();
    }

    @Override
    public void onResume() {
        super.onResume();

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int errorCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (errorCode != ConnectionResult.SUCCESS) {
            Dialog errorDialog = apiAvailability.getErrorDialog(this, errorCode, REQUEST_ERROR,
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            // Leave if service is unavailable
                            finish();
                        }
                    });
            errorDialog.show();
        }
    }
}
