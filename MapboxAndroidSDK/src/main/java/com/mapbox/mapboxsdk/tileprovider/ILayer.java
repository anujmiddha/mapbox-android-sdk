package com.mapbox.mapboxsdk.tileprovider;

import java.io.InputStream;

import com.mapbox.mapboxsdk.views.MapView;

import android.graphics.drawable.Drawable;

public interface ILayer {
    public ILayer addTo(MapView mv);
}
