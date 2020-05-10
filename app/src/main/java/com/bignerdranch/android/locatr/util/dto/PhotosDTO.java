package com.bignerdranch.android.locatr.util.dto;

import com.bignerdranch.android.locatr.model.GalleryItem;
import com.google.gson.annotations.SerializedName;

/**
 * Data transfer object to hold the data for individual container of photo packages.
 * They are all inside the root of the parsed JSON object with photos (see {@link RootJsonDTO}).
 */
public class PhotosDTO {

    @SerializedName("photo")
    private GalleryItem[] mGalleryItems;

    public GalleryItem[] getGalleryItems() {
        return mGalleryItems;
    }
}
