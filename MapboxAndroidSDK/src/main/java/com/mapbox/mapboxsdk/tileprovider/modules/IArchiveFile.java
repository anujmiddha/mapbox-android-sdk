package com.mapbox.mapboxsdk.tileprovider.modules;

import java.io.InputStream;

import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.tileprovider.ITileLayer;

public interface IArchiveFile {

    /**
     * Get the input stream for the requested tile.
     *
     * @return the input stream, or null if the archive doesn't contain an entry for the requested tile
     */
    InputStream getInputStream(ITileLayer tileSource, MapTile tile);

}
