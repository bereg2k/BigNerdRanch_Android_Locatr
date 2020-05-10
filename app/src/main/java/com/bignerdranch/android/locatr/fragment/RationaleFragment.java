package com.bignerdranch.android.locatr.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bignerdranch.android.locatr.R;

import java.util.Objects;

/**
 * Fragment that holds a view with rationale for requesting location permissions.
 */
public class RationaleFragment extends DialogFragment {
    private static final String ARG_PERMISSIONS = "permissions";
    private static final String ARG_REQUEST_CODE = "request_code";

    public static RationaleFragment newInstance(String[] permissions, int requestCode) {
        Bundle args = new Bundle();
        args.putStringArray(ARG_PERMISSIONS, permissions);
        args.putInt(ARG_REQUEST_CODE, requestCode);

        RationaleFragment fragment = new RationaleFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_rationale, null);

        Button okButton = view.findViewById(R.id.rationale_ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .create();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        String[] permissions = getArguments().getStringArray(ARG_PERMISSIONS);
        int requestCode = getArguments().getInt(ARG_REQUEST_CODE, 0);

        // Call for request permissions from LocatrFragment in order to invoke its own
        // on onRequestPermissionsResult() method after it.
        // Without it, there's no image fetching after granting permission,
        // so user has to press a Search button again.
        Objects.requireNonNull(getTargetFragment()).requestPermissions(permissions, requestCode);
    }
}
