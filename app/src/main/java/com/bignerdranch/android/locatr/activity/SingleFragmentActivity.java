package com.bignerdranch.android.locatr.activity;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bignerdranch.android.locatr.R;

/**
 * Parent class activity for all activity classes that host single fragment
 * in its fragment's "container".
 */
public abstract class SingleFragmentActivity extends AppCompatActivity {
    private static final String TAG = SingleFragmentActivity.class.getSimpleName();

    @LayoutRes
    public static final int ACTIVITY_LAYOUT = R.layout.activity_fragment;
    @IdRes
    public static final int FRAGMENT_ID = R.id.fragment_container;

    public abstract Fragment createFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ACTIVITY_LAYOUT);

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(FRAGMENT_ID);

        if (fragment == null) {
            fragment = createFragment();

            fragmentManager.beginTransaction()
                    .add(FRAGMENT_ID, fragment)
                    .commit();

            Log.i(TAG, "onCreate: fragment has been successfully added!");
        }
    }
}
