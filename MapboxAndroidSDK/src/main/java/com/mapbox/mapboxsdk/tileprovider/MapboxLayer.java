package com.mapbox.mapboxsdk.tileprovider;

import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;

/**
 * A convenience class to initialize tile layers that use Mapbox.
 */
public class MapboxLayer extends TileLayer implements MapViewConstants, MapboxConstants {
    /**
     * Initialize a new tile layer, directed at a hosted Mapbox tilesource.
     * @param id a valid mapid, of the form account.map
     */
    public MapboxLayer(String id) {
        super(id);
        if (!id.contains("http://") && !id.contains("https://") && id.contains("")) {
            this.setURL(MAPBOX_BASE_URL + id + "/{z}/{x}/{y}{2x}.png");
        }
    }
}