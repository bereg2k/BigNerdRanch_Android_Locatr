package com.bignerdranch.android.locatr.util.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data transfer object to hold the data for a root of the parsed JSON object with photos.
 */
public class RootJsonDTO {

    @SerializedName("photos")
    private PhotosDTO photos;

    public PhotosDTO getPhotos() {
        return photos;
    }
}
