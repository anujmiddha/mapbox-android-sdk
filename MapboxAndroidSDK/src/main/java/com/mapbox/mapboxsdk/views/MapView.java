package com.mapbox.mapboxsdk.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.Scroller;
import android.widget.Toast;
import com.mapbox.mapboxsdk.DefaultResourceProxyImpl;
import com.mapbox.mapboxsdk.R;
import com.mapbox.mapboxsdk.ResourceProxy;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.events.MapListener;
import com.mapbox.mapboxsdk.events.ScrollEvent;
import com.mapbox.mapboxsdk.events.ZoomEvent;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.tile.TileSystem;
import com.mapbox.mapboxsdk.tileprovider.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.MapboxTileLayer;
import com.mapbox.mapboxsdk.tileprovider.TileLayer;
import com.mapbox.mapboxsdk.tileprovider.util.SimpleInvalidationHandler;
import com.mapbox.mapboxsdk.util.GeometryMath;
import com.mapbox.mapboxsdk.util.NetworkUtils;
import com.mapbox.mapboxsdk.views.util.Projection;
import com.mapbox.mapboxsdk.views.util.TileLoadedListener;
import com.mapbox.mapboxsdk.views.util.TilesLoadedListener;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;
import com.mapbox.mapboxsdk.views.util.constants.MapViewLayouts;
import org.json.JSONException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The MapView class manages all of the content and
 * state of a single map, including layers, markers,
 * and interaction code.
 */
public class MapView extends ViewGroup implements MapViewConstants, MapboxConstants {

    /**
     * A copy of the app context.
     */
    private Context context;
    /**
     * Whether or not a marker has been placed already.
     */
    private boolean firstMarker = true;

    private static final String TAG = "MapBox MapView";
    private static Method sMotionEventTransformMethod;

    /**
     * Current zoom level for map tiles.
     */
	private float mZoomLevel = 11;
	protected float mMinimumZoomLevel = 0;
	protected float mMaximumZoomLevel = 22;

    private Projection mProjection;

    private final GestureDetector mGestureDetector;

    /**
     * Handles map scrolling
     */
    protected final Scroller mScroller;
    protected boolean mIsFlinging;

    protected final AtomicInteger mTargetZoomLevel = new AtomicInteger();
    protected final AtomicBoolean mIsAnimating = new AtomicBoolean(false);

    private final MapController mController;

    private final ResourceProxy mResourceProxy;

	protected ScaleGestureDetector mScaleGestureDetector;
    protected float mMultiTouchScale = 1.0f;
    protected PointF mMultiTouchScalePoint = new PointF();

    protected MapListener mListener;

    private float mapOrientation = 0;
    private final Matrix mRotateMatrix = new Matrix();
    private final float[] mRotatePoints = new float[2];
    private final Rect mInvalidateRect = new Rect();

    protected BoundingBox mScrollableAreaBoundingBox;
    protected RectF mScrollableAreaLimit;

    // for speed (avoiding allocations)
    protected TileLayer tileLayer;

    private final Handler mTileRequestCompleteHandler;

    /* a point that will be reused to design added views */
    private final PointF mPoint = new PointF();

    private TilesLoadedListener tilesLoadedListener;
    TileLoadedListener tileLoadedListener;

    /**
     * Constructor for XML layout calls. Should not be used programmatically.
     * @param context A copy of the app context
     * @param attrs An AttributeSet object to get extra info from the XML, such as mapbox id or type of baselayer
     */
    protected MapView(final Context context, final int tileSizePixels,
                             final ResourceProxy resourceProxy, TileLayer tileLayer,
                             final Handler tileRequestCompleteHandler, final AttributeSet attrs) {
        super(context, attrs);
        mResourceProxy = resourceProxy;
        this.mController = new MapController(this);
        this.mScroller = new Scroller(context);
        TileSystem.setTileSize(tileSizePixels);

        if (tileLayer == null) {
            this.tileLayer = new MapboxTileLayer("examples.map-zgrqqx0w");
        }

        mTileRequestCompleteHandler = tileRequestCompleteHandler == null
                ? new SimpleInvalidationHandler(this)
                : tileRequestCompleteHandler;
        this.tileLayer = tileLayer;

        this.mGestureDetector = new GestureDetector(context, new MapViewGestureDetectorListener(this));
        mGestureDetector.setOnDoubleTapListener(new MapViewDoubleClickListener(this));

        mScaleGestureDetector = new ScaleGestureDetector(context, new MapViewScaleGestureDetectorListener(this));

        this.context = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MapView);
        String mapid = a.getString(R.styleable.MapView_mapid);
        a.recycle();
        if (mapid != null)
        {
            setTileSource(new MapboxTileLayer(mapid));
        }
        else
        {
			Log.w(MapView.class.getCanonicalName(), "mapid not set.");
        }
    }

    public MapView(final Context context, AttributeSet attrs){
        this(context, 256, new DefaultResourceProxyImpl(context), null, null, attrs);
    }

    protected MapView(Context context, int tileSizePixels, ResourceProxy resourceProxy, TileLayer aTileProvider) {
        this(context, tileSizePixels, resourceProxy, aTileProvider, null, null);
    }

    public void setTileSource(final ITileLayer aTileSource) {
        TileSystem.setTileSize(aTileSource.getTileSizePixels());
        this.setZoom(mZoomLevel);
        postInvalidate();
    }

    /**
     * @param p the position where the event occurred.
     * @return whether the event action is triggered or not
     */
    public boolean singleTapUpHelper(ILatLng p) {
        onTap(p);
        return true;
    }

    /**
     * @param p the position where the event occurred.
     * @return whether the event action is triggered or not
     */
    public boolean longPressHelper(ILatLng p) {
        onLongPress(p);
        return false;
    }

    public void onLongPress(ILatLng p) {
    }
    public void onTap(ILatLng p) {
    }

    public MapController getController() {
        return this.mController;
    }

    public Scroller getScroller() {
        return mScroller;
    }

    /**
     * Compute the current geographical bounding box for this map.
     * @return the current bounds of the map
     */
    public BoundingBox getBoundingBox() {
        return getBoundingBox(getWidth(), getHeight());
    }

    private BoundingBox getBoundingBox(final int pViewWidth, final int pViewHeight) {

        final int world_2 = TileSystem.MapSize(mZoomLevel) / 2;
        final Rect screenRect = getScreenRect(null);
        screenRect.offset(world_2, world_2);

        final ILatLng neGeoPoint = TileSystem.PixelXYToLatLong(screenRect.right, screenRect.top,
                mZoomLevel);
        final ILatLng swGeoPoint = TileSystem.PixelXYToLatLong(screenRect.left,
                screenRect.bottom, mZoomLevel);

        return new BoundingBox(neGeoPoint.getLatitude(), neGeoPoint.getLongitude(),
                swGeoPoint.getLatitude(), swGeoPoint.getLongitude());
    }

    /**
     * Get centerpoint of the phone as latitude and longitude.
     * @return centerpoint
     */
    public LatLng getCenter() {
        return getBoundingBox().getCenter();
    }

    /**
     * Gets the current bounds of the screen in <I>screen coordinates</I>.
     */
    public Rect getScreenRect(final Rect reuse) {
        final Rect out = getIntrinsicScreenRect(reuse);
        if (this.getMapOrientation() != 0 && this.getMapOrientation() != 180) {
            // Since the canvas is shifted by getWidth/2, we can just return our natural scrollX/Y
            // value since that is the same as the shifted center.
            int centerX = this.getScrollX();
            int centerY = this.getScrollY();
            GeometryMath.getBoundingBoxForRotatedRectangle(out, centerX, centerY,
                    this.getMapOrientation(), out);
        }
        return out;
    }

    public Rect getIntrinsicScreenRect(final Rect reuse) {
        final Rect out = reuse == null ? new Rect() : reuse;
        out.set(getScrollX() - getWidth() / 2, getScrollY() - getHeight() / 2, getScrollX()
                + getWidth() / 2, getScrollY() + getHeight() / 2);
        return out;
    }

    /**
     * Get a projection for converting between screen-pixel coordinates and latitude/longitude
     * coordinates. You should not hold on to this object for more than one draw, since the
     * projection of the map could change.
     *
     * @return The Projection of the map in its current state. You should not hold on to this object
     *         for more than one draw, since the projection of the map could change.
     */
    public Projection getProjection() {
        if (mProjection == null) {
            mProjection = new Projection(this);
        }
        return mProjection;
    }

    /**
     * Set the centerpoint of the map view, given a latitude and
     * longitude position.
     * @param aCenter
     * @return the map view, for chaining
     */
    public MapView setCenter(final ILatLng aCenter) {
        getController().setCenter(aCenter);
        return this;
    }

    public MapView panBy(int x, int y) {
        scrollBy(x, y);
        return this;
    }

    public MapView setScale(float scale) {
        mMultiTouchScale = scale;
        invalidate();
        return this;
    }

    public float getScale() {
        return mMultiTouchScale;
    }

    /**
     * @param aZoomLevel the zoom level bound by the tile source
     * @return the map view, for chaining
     */
	public MapView setZoom(final float aZoomLevel) {
		return this.mController.setZoom(aZoomLevel);
	}

    
	protected MapView setZoomInternal(final float aZoomLevel) {
		final float minZoomLevel = getMinZoomLevel();
		final float maxZoomLevel = getMaxZoomLevel();

		final float newZoomLevel = Math.max(minZoomLevel,
				Math.min(maxZoomLevel, aZoomLevel));
		final float curZoomLevel = this.mZoomLevel;

        if (newZoomLevel != curZoomLevel) {
            mScroller.forceFinished(true);
            mIsFlinging = false;
        }

        this.mZoomLevel = newZoomLevel;
	    updateScrollableAreaLimit();

        if (newZoomLevel > curZoomLevel) {
            // We are going from a lower-resolution plane to a higher-resolution plane, so we have
            // to do it the hard way.
            final int worldSize_current_2 = TileSystem.MapSize(curZoomLevel) / 2;
            final int worldSize_new_2 = TileSystem.MapSize(newZoomLevel) / 2;
            final ILatLng centerGeoPoint = TileSystem.PixelXYToLatLong(getScrollX()
                    + worldSize_current_2, getScrollY() + worldSize_current_2, curZoomLevel);
            final PointF centerPoint = TileSystem.LatLongToPixelXY(
                    centerGeoPoint.getLatitude(), centerGeoPoint.getLongitude(),
                    newZoomLevel, null);
            scrollTo((int)centerPoint.x - worldSize_new_2, (int)centerPoint.y - worldSize_new_2);
        } else if (newZoomLevel < curZoomLevel) {
            // We are going from a higher-resolution plane to a lower-resolution plane, so we can do
            // it the easy way.
			scrollTo(
					(int) (GeometryMath.rightShift(getScrollX(), curZoomLevel
							- newZoomLevel)),
					(int) (GeometryMath.rightShift(getScrollY(), curZoomLevel
							- newZoomLevel)));
        }
//        scrollTo(getScrollX(), getScrollY());

        // snap for all snappables
        final Point snapPoint = new Point();
        mProjection = new Projection(this);

        // do callback on listener
        if (newZoomLevel != curZoomLevel && mListener != null) {
            final ZoomEvent event = new ZoomEvent(this, newZoomLevel);
            mListener.onZoom(event);
        }

        // Allows any views fixed to a Location in the MapView to adjust
        this.requestLayout();
        return this;
    }

    /**
     * Zoom the map to enclose the specified bounding box, as closely as possible.
     * Must be called after display layout is complete, or screen dimensions are not known, and
     * will always zoom to center of zoom  level 0.
     * Suggestion: Check getScreenRect(null).getHeight() > 0
     */
    public MapView zoomToBoundingBox(final BoundingBox boundingBox) {
        final BoundingBox currentBox = getBoundingBox();

        // Calculated required zoom based on latitude span
        final double maxZoomLatitudeSpan = mZoomLevel == getMaxZoomLevel() ?
                currentBox.getLatitudeSpan() :
                currentBox.getLatitudeSpan() / Math.pow(2, getMaxZoomLevel() - mZoomLevel);

        final double requiredLatitudeZoom =
                getMaxZoomLevel() -
                        Math.ceil(Math.log(boundingBox.getLatitudeSpan() / maxZoomLatitudeSpan) / Math.log(2));


        // Calculated required zoom based on longitude span
        final double maxZoomLongitudeSpan = mZoomLevel == getMaxZoomLevel() ?
                currentBox.getLongitudeSpan() :
                currentBox.getLongitudeSpan() / Math.pow(2, getMaxZoomLevel() - mZoomLevel);

		final double requiredLongitudeZoom = getMaxZoomLevel()
				- (Math.log(boundingBox.getLongitudeSpan()
						/ maxZoomLongitudeSpan) / Math.log(2));


        // Zoom to boundingBox center, at calculated maximum allowed zoom level
		getController().setZoom(
				(float) Math.max(
						Math.max(requiredLatitudeZoom, requiredLongitudeZoom),
						getMinZoomLevel()));

        getController().setCenter(
                new LatLng(boundingBox.getCenter().getLatitude(), boundingBox.getCenter()
                        .getLongitude()));

        return this;
    }

    public float getClampedZoomLevel(float zoom) {
		final float minZoomLevel = getMinZoomLevel();
		final float maxZoomLevel = getMaxZoomLevel();

		return Math.max(minZoomLevel, Math.min(maxZoomLevel, zoom));
	}

    /**
     * Get the current ZoomLevel for the map tiles.
     *
     * @return the current ZoomLevel between 0 (equator) and 18/19(closest), depending on the tile
     *         source chosen.
     */
	public float getZoomLevel() {
        return getZoomLevel(true);
    }

	private float getAnimatedZoom(){
		return Float.intBitsToFloat(mTargetZoomLevel.get());
	}

    /**
     * Get the current ZoomLevel for the map tiles.
     *
     * @param aPending if true and we're animating then return the zoom level that we're animating
     *                 towards, otherwise return the current zoom level
     * @return the zoom level
     */
	public float getZoomLevel(final boolean aPending) {
        if (aPending && isAnimating()) {
            return getAnimatedZoom();
        } else {
            return mZoomLevel;
        }
    }
	
    /**
     * Get the minimum allowed zoom level for the maps.
     */
	public float getMinZoomLevel() {
		float newMinZoom = mMinimumZoomLevel;

			float boundingDimension = Math.max(getMeasuredWidth(),
					getMeasuredHeight());
			float tileSideLength = TileSystem.getTileSize();
			if (boundingDimension > 0 && tileSideLength > 0) {
				float clampedMinZoom = (float) (Math.log(boundingDimension
						/ tileSideLength) / Math.log(2));
				if (newMinZoom < clampedMinZoom)
					newMinZoom = clampedMinZoom;

    }
			if (newMinZoom < 0)
				newMinZoom = 0;

		return newMinZoom;
	}

    /**
     * Get the maximum allowed zoom level for the maps.
     */
	public float getMaxZoomLevel() {
		return mMaximumZoomLevel;
    }

    /**
     * Set the minimum allowed zoom level, or pass null to use the minimum zoom level from the tile
     * provider.
     */
	public void setMinZoomLevel(float zoomLevel) {
        mMinimumZoomLevel = zoomLevel;
    }

    /**
     * Set the maximum allowed zoom level, or pass null to use the maximum zoom level from the tile
     * provider.
     */
	public void setMaxZoomLevel(float zoomLevel) {
        mMaximumZoomLevel = zoomLevel;
    }

    /**
     * Determine whether the map is at its maximum zoom
     * @return whether the map can zoom in
     */
    protected boolean canZoomIn() {
		final float maxZoomLevel = getMaxZoomLevel();
        if ((isAnimating() ? getAnimatedZoom() : mZoomLevel) >= maxZoomLevel) {
            return false;
        }
        return true;
    }

    /**
     * Determine whether the map is at its minimum zoom
     * @return whether the map can zoom out
     */
    protected boolean canZoomOut() {
		final float minZoomLevel = getMinZoomLevel();
        if ((isAnimating() ? getAnimatedZoom() : mZoomLevel) <= minZoomLevel) {
            return false;
        }
        return true;
    }

    /**
     * Zoom in by one zoom level.
     */
    public boolean zoomIn() {
        return getController().zoomIn();
    }

    public boolean zoomInFixing(final ILatLng point) {
        return getController().zoomInAbout(point);
    }

    /**
     * Zoom out by one zoom level.
     */
    public boolean zoomOut() {
        return getController().zoomOut();
    }

    public boolean zoomOutFixing(final ILatLng point) {
        return getController().zoomOutAbout(point);
    }

    public ResourceProxy getResourceProxy() {
        return mResourceProxy;
    }

    public void setMapOrientation(float degrees) {
        this.mapOrientation = degrees % 360.0f;
        this.invalidate();
    }

    public float getMapOrientation() {
        return mapOrientation;
    }

    public void updateScrollableAreaLimit()
    {
    	if (mScrollableAreaBoundingBox == null) return;
    	float zoom = getZoomLevel();
//    	if (isAnimating()) {
//    		zoom = mZoomLevel + (zoom - mZoomLevel);
//    	}
        final int worldSize_2 = TileSystem.MapSize(zoom) / 2;
        // Get NW/upper-left
        final PointF upperLeft = TileSystem.LatLongToPixelXY(mScrollableAreaBoundingBox.getLatNorth(),
        		mScrollableAreaBoundingBox.getLonWest(), zoom, null);
        upperLeft.offset(-worldSize_2, -worldSize_2);

        // Get SE/lower-right
        final PointF lowerRight = TileSystem.LatLongToPixelXY(mScrollableAreaBoundingBox.getLatSouth(),
        		mScrollableAreaBoundingBox.getLonEast(), zoom, null);
        lowerRight.offset(-worldSize_2, -worldSize_2);
        if (mScrollableAreaLimit == null) {
            mScrollableAreaLimit = new RectF(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);
        }
        else {
            mScrollableAreaLimit.set(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);
        }
    }

    /**
     * Set the map to limit it's scrollable view to the specified BoundingBox. Note this does not
     * limit zooming so it will be possible for the user to zoom to an area that is larger than the
     * limited area.
     *
     * @param boundingBox A lat/long bounding box to limit scrolling to, or null to remove any scrolling
     *                    limitations
     */
    public void setScrollableAreaLimit(BoundingBox boundingBox) {

        mScrollableAreaBoundingBox = boundingBox;

        // Clear scrollable area limit if null passed.
        if (mScrollableAreaBoundingBox == null) {
            mScrollableAreaLimit = null;
            return;
        }

        updateScrollableAreaLimit();
    }

    public BoundingBox getScrollableAreaLimit() {
        return mScrollableAreaBoundingBox;
    }

    public void invalidateMapCoordinates(final Rect dirty) {
        mInvalidateRect.set(dirty);
        final int width_2 = this.getWidth() / 2;
        final int height_2 = this.getHeight() / 2;

        // Since the canvas is shifted by getWidth/2, we can just return our natural scrollX/Y value
        // since that is the same as the shifted center.
        int centerX = this.getScrollX();
        int centerY = this.getScrollY();

        if (this.getMapOrientation() != 0)
            GeometryMath.getBoundingBoxForRotatedRectangle(mInvalidateRect, centerX, centerY,
                    this.getMapOrientation() + 180, mInvalidateRect);
        mInvalidateRect.offset(width_2, height_2);

        super.invalidate(mInvalidateRect);
    }

    /**
     * Returns a set of layout parameters with a width of
     * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}, a height of
     * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT} at the {@link com.mapbox.mapboxsdk.geometry.LatLng} (0, 0) align
     * with {@link MapView.LayoutParams#BOTTOM_CENTER}.
     */
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, null, MapView.LayoutParams.BOTTOM_CENTER, 0, 0);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(final AttributeSet attrs) {
        return new MapView.LayoutParams(getContext(), attrs);
    }

    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
        return p instanceof MapView.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(final ViewGroup.LayoutParams p) {
        return new MapView.LayoutParams(p);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int count = getChildCount();

        int maxHeight = 0;
        int maxWidth = 0;

        // Find out how big everyone wants to be
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        // Find rightmost and bottom-most child
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                final MapView.LayoutParams lp = (MapView.LayoutParams) child.getLayoutParams();
                final int childHeight = child.getMeasuredHeight();
                final int childWidth = child.getMeasuredWidth();
                getProjection().toMapPixels(lp.geoPoint, mPoint);
                final int x = (int)mPoint.x + getWidth() / 2;
                final int y = (int)mPoint.y + getHeight() / 2;
                int childRight = x;
                int childBottom = y;
                switch (lp.alignment) {
                    case MapView.LayoutParams.TOP_LEFT:
                        childRight = x + childWidth;
                        childBottom = y;
                        break;
                    case MapView.LayoutParams.TOP_CENTER:
                        childRight = x + childWidth / 2;
                        childBottom = y;
                        break;
                    case MapView.LayoutParams.TOP_RIGHT:
                        childRight = x;
                        childBottom = y;
                        break;
                    case MapView.LayoutParams.CENTER_LEFT:
                        childRight = x + childWidth;
                        childBottom = y + childHeight / 2;
                        break;
                    case MapView.LayoutParams.CENTER:
                        childRight = x + childWidth / 2;
                        childBottom = y + childHeight / 2;
                        break;
                    case MapView.LayoutParams.CENTER_RIGHT:
                        childRight = x;
                        childBottom = y + childHeight / 2;
                        break;
                    case MapView.LayoutParams.BOTTOM_LEFT:
                        childRight = x + childWidth;
                        childBottom = y + childHeight;
                        break;
                    case MapView.LayoutParams.BOTTOM_CENTER:
                        childRight = x + childWidth / 2;
                        childBottom = y + childHeight;
                        break;
                    case MapView.LayoutParams.BOTTOM_RIGHT:
                        childRight = x;
                        childBottom = y + childHeight;
                        break;
                }
                childRight += lp.offsetX;
                childBottom += lp.offsetY;

                maxWidth = Math.max(maxWidth, childRight);
                maxHeight = Math.max(maxHeight, childBottom);
            }
        }

        // Account for padding too
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        // Check against minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
                resolveSize(maxHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r,
                            final int b) {
        final int count = getChildCount();
		if (changed) {
			float minZoom = getMinZoomLevel();
			if (mZoomLevel < minZoom) {
				setZoom(minZoom);
			}
		}
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                final MapView.LayoutParams lp = (MapView.LayoutParams) child.getLayoutParams();
                final int childHeight = child.getMeasuredHeight();
                final int childWidth = child.getMeasuredWidth();
                getProjection().toMapPixels(lp.geoPoint, mPoint);
                final int x = (int)mPoint.x + getWidth() / 2;
                final int y = (int)mPoint.y + getHeight() / 2;
                int childLeft = x;
                int childTop = y;
                switch (lp.alignment) {
                    case MapView.LayoutParams.TOP_LEFT:
                        childLeft = getPaddingLeft() + x;
                        childTop = getPaddingTop() + y;
                        break;
                    case MapView.LayoutParams.TOP_CENTER:
                        childLeft = getPaddingLeft() + x - childWidth / 2;
                        childTop = getPaddingTop() + y;
                        break;
                    case MapView.LayoutParams.TOP_RIGHT:
                        childLeft = getPaddingLeft() + x - childWidth;
                        childTop = getPaddingTop() + y;
                        break;
                    case MapView.LayoutParams.CENTER_LEFT:
                        childLeft = getPaddingLeft() + x;
                        childTop = getPaddingTop() + y - childHeight / 2;
                        break;
                    case MapView.LayoutParams.CENTER:
                        childLeft = getPaddingLeft() + x - childWidth / 2;
                        childTop = getPaddingTop() + y - childHeight / 2;
                        break;
                    case MapView.LayoutParams.CENTER_RIGHT:
                        childLeft = getPaddingLeft() + x - childWidth;
                        childTop = getPaddingTop() + y - childHeight / 2;
                        break;
                    case MapView.LayoutParams.BOTTOM_LEFT:
                        childLeft = getPaddingLeft() + x;
                        childTop = getPaddingTop() + y - childHeight;
                        break;
                    case MapView.LayoutParams.BOTTOM_CENTER:
                        childLeft = getPaddingLeft() + x - childWidth / 2;
                        childTop = getPaddingTop() + y - childHeight;
                        break;
                    case MapView.LayoutParams.BOTTOM_RIGHT:
                        childLeft = getPaddingLeft() + x - childWidth;
                        childTop = getPaddingTop() + y - childHeight;
                        break;
                }
                childLeft += lp.offsetX;
                childTop += lp.offsetY;
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent event) {

        Log.d(TAG, "dispatchTouchEvent(" + event + ")");

        // Get rotated event for some touch listeners.
        MotionEvent rotatedEvent = rotateTouchEvent(event);

        try {
            if (super.dispatchTouchEvent(event)) {
                Log.d(TAG, "super handled onTouchEvent");
                return true;
            }

            if (event.getPointerCount() == 1) {
                if (mGestureDetector.onTouchEvent(rotatedEvent)) {
                    Log.d(TAG, "GestureDetector handled onTouchEvent");
                    return true;
                }
            } else {
                // despite the docs, scalegesturedetector does not return
                // false if it doesn't handle an event.
                if (mScaleGestureDetector.onTouchEvent(event)) {
                    Log.d(TAG, "ScaleDetector handled onTouchEvent");
                    return true;
                }
            }

        } finally {
            if (rotatedEvent != event) {
                rotatedEvent.recycle();
            }
        }

        Log.d(TAG, "no-one handled onTouchEvent");
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    private MotionEvent rotateTouchEvent(MotionEvent ev) {
        if (this.getMapOrientation() == 0)
            return ev;

        mRotateMatrix.setRotate(-getMapOrientation(), this.getWidth() / 2, this.getHeight() / 2);

        MotionEvent rotatedEvent = MotionEvent.obtain(ev);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            mRotatePoints[0] = ev.getX();
            mRotatePoints[1] = ev.getY();
            mRotateMatrix.mapPoints(mRotatePoints);
            rotatedEvent.setLocation(mRotatePoints[0], mRotatePoints[1]);
        } else {
            // This method is preferred since it will rotate historical touch events too
            try {
                if (sMotionEventTransformMethod == null) {
                    sMotionEventTransformMethod = MotionEvent.class.getDeclaredMethod("transform",
                            new Class[]{Matrix.class});
                }
                sMotionEventTransformMethod.invoke(rotatedEvent, mRotateMatrix);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return rotatedEvent;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mScroller.isFinished()) {
                // One last scrollTo to get to the final destination
                scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
                // This will facilitate snapping-to any Snappable points.
                setZoom(mZoomLevel);
                mIsFlinging = false;
            } else {
                scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            }
            postInvalidate(); // Keep on drawing until the animation has
            // finished.
        }
    }

    public void updateScrollDuringAnimation()
    {
//    	updateScrollableAreaLimit();
//        scrollTo(getScrollX(), getScrollY());
    }

    @Override
    public void scrollTo(int x, int y) {
        final int worldSize_2 = TileSystem.MapSize(this.getZoomLevel(false)) / 2;
        while (x < -worldSize_2) {
            x += worldSize_2 * 2;
        }
        while (x > worldSize_2) {
            x -= worldSize_2 * 2;
        }
        while (y < -worldSize_2) {
            y += worldSize_2 * 2;
        }
        while (y > worldSize_2) {
            y -= worldSize_2 * 2;
        }

        if (mScrollableAreaLimit != null) {
        	final float width_2 = this.getMeasuredWidth()/2;
            final float height_2 = this.getMeasuredHeight()/2;
            // Adjust if we are outside the scrollable area
            if (mScrollableAreaLimit.width() <= width_2*2) {
                if (x - width_2 > mScrollableAreaLimit.left) {
                    x = (int) (mScrollableAreaLimit.left + width_2);
                } else if (x + width_2 < mScrollableAreaLimit.right) {
                    x = (int) (mScrollableAreaLimit.right - width_2);
                }
            } else if (x - width_2 < mScrollableAreaLimit.left) {
                x = (int) (mScrollableAreaLimit.left + width_2);
            } else if (x + width_2 > mScrollableAreaLimit.right) {
                x = (int) (mScrollableAreaLimit.right - width_2);
            }

            if (mScrollableAreaLimit.height() <= height_2*2) {
                if (y - height_2 > mScrollableAreaLimit.top) {
                    y = (int) (mScrollableAreaLimit.top + height_2);
                } else if (y + height_2 < mScrollableAreaLimit.bottom) {
                    y = (int) (mScrollableAreaLimit.bottom - height_2);
                }
            } else if (y - height_2 < mScrollableAreaLimit.top) {
                y = (int) (mScrollableAreaLimit.top + height_2);
            } else if (y + height_2 > mScrollableAreaLimit.bottom) {
                y = (int) (mScrollableAreaLimit.bottom - height_2);
            }
        }
        super.scrollTo(x, y);

        // do callback on listener
        if (mListener != null) {
            final ScrollEvent event = new ScrollEvent(this, x, y);
            mListener.onScroll(event);
        }
    }

    @Override
    protected void dispatchDraw(final Canvas c) {

        mProjection = new Projection(this);

        // Save the current canvas matrix
        c.save();

        c.translate(getWidth() / 2, getHeight() / 2);
        c.scale(mMultiTouchScale, mMultiTouchScale, mMultiTouchScalePoint.x,
            mMultiTouchScalePoint.y);

		// rotate Canvas
        c.rotate(mapOrientation,
                mProjection.getScreenRect().exactCenterX(),
                mProjection.getScreenRect().exactCenterY());


        c.restore();

        super.dispatchDraw(c);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /**
     * Determines if maps are animating a zoom operation. Useful for overlays to avoid recalculating
     * during an animation sequence.
     *
     * @return boolean indicating whether view is animating.
     */
    public boolean isAnimating() {
        return mIsAnimating.get();
    }

    public TileLoadedListener getTileLoadedListener() {
        return tileLoadedListener;
    }

    // ===========================================================
    // Public Classes
    // ===========================================================

    /**
     * Per-child layout information associated with OpenStreetMapView.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams implements MapViewLayouts  {
        /**
         * The location of the child within the map view.
         */
        public ILatLng geoPoint;

        /**
         * The alignment the alignment of the view compared to the location.
         */
        public int alignment;

        public int offsetX;
        public int offsetY;

        /**
         * Creates a new set of layout parameters with the specified width, height and location.
         *
         * @param width     the width, either {@link #FILL_PARENT}, {@link #WRAP_CONTENT} or a fixed size
         *                  in pixels
         * @param height    the height, either {@link #FILL_PARENT}, {@link #WRAP_CONTENT} or a fixed size
         *                  in pixels
         * @param geoPoint  the location of the child within the map view
         * @param alignment the alignment of the view compared to the location {@link #BOTTOM_CENTER},
         *                  {@link #BOTTOM_LEFT}, {@link #BOTTOM_RIGHT} {@link #TOP_CENTER},
         *                  {@link #TOP_LEFT}, {@link #TOP_RIGHT}
         * @param offsetX   the additional X offset from the alignment location to draw the child within
         *                  the map view
         * @param offsetY   the additional Y offset from the alignment location to draw the child within
         *                  the map view
         */
        public LayoutParams(final int width, final int height, final ILatLng geoPoint,
                            final int alignment, final int offsetX, final int offsetY) {
            super(width, height);
            if (geoPoint != null) {
                this.geoPoint = geoPoint;
            } else {
                this.geoPoint = new LatLng(0, 0);
            }
            this.alignment = alignment;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        /**
         * Since we cannot use XML files in this project this constructor is useless. Creates a new
         * set of layout parameters. The values are extracted from the supplied attributes set and
         * context.
         *
         * @param c     the application environment
         * @param attrs the set of attributes fom which to extract the layout parameters values
         */
        public LayoutParams(final Context c, final AttributeSet attrs) {
            super(c, attrs);
            this.geoPoint = new LatLng(0, 0);
            this.alignment = BOTTOM_CENTER;
        }

        public LayoutParams(final ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
