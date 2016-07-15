package com.esri.arcgisruntime.sample.auto_parking;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.datasource.arcgis.ArcGISFeature;
import com.esri.arcgisruntime.datasource.arcgis.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;

import com.esri.arcgisruntime.sample.BaseItemAnimator;
import com.mypopsy.drawable.ToggleDrawable;
import com.mypopsy.drawable.model.CrossModel;
import com.mypopsy.drawable.util.Bezier;
import com.mypopsy.widget.FloatingSearchView;
import com.mypopsy.widget.internal.ViewUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AutoParking";
    private MapView mMapView;
    private ArcGISMap mMap;
    private Portal mPortal;
    private PortalItem mPortalItem;
    private android.graphics.Point mClickPoint;
    private ArcGISFeature mSelectedArcGISFeature;
    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private LocationDisplay mLocationDisplay;
    FloatingActionButton mLocationFAB;
    private AlertDialog.Builder builder;


    private static final int REQ_CODE_SPEECH_INPUT = 42;

    private FloatingSearchView mSearchView;
    //private SearchAdapter mAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // inflate MapView from layout
        mMapView = (MapView) findViewById(R.id.mapView);
        // create a map with the BasemapType topographic
        //ArcGISMap mMap = new ArcGISMap(Basemap.createStreets());

        // Build a alert dialog with specified style
        builder = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);

        mLocationFAB = (FloatingActionButton) findViewById(R.id.locationFab);
        // get the portal url for ArcGIS Online
        mPortal = new Portal(getResources().getString(R.string.portal_url));
        // get the pre-defined portal id and portal url
        mPortalItem = new PortalItem(mPortal, getResources().getString(R.string.parking_map));
        // create a map from a PortalItem
        mMap = new ArcGISMap(mPortalItem);
        // set the map to be displayed in this view
        // create feature layer with its service feature table
        // create the service feature table
        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(getResources().getString(R.string.esri_parking_service_url));

        // create the feature layer using the service feature table
        final FeatureLayer featureLayer = new FeatureLayer(serviceFeatureTable);
        featureLayer.setSelectionColor(Color.rgb(0, 255, 255)); //cyan, fully opaque
        featureLayer.setSelectionWidth(3);
        // add the layer to the map
        mMap.getOperationalLayers().add(featureLayer);

        // set the map to be displayed in the mapview
        mMapView.setMap(mMap);

        mSearchView = (FloatingSearchView) findViewById(R.id.search);

        mSearchView.showLogo(true);
        //mSearchView.setItemAnimator(new CustomSuggestionItemAnimator(mSearchView));



        //{"x":-13044550,"y":4040393,"spatialReference":{"wkid":102100}} 13044550, 4040393

        mMap.setInitialViewpoint(new Viewpoint(new Point(-117.195685, 34.056509, SpatialReferences.getWgs84()), 5e4));

        // get the MapView's LocationDisplay
        mLocationDisplay = mMapView.getLocationDisplay();

        // Listen to changes in the status of the location data source.
        mLocationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                // If LocationDisplay started OK, then continue.
                if (dataSourceStatusChangedEvent.isStarted())
                    return;

                // No error is reported, then continue.
                if (dataSourceStatusChangedEvent.getSource().getLocationDataSource().getError() == null)
                    return;

                // If an error is found, handle the failure to start.
                // Check permissions to see if failure may be due to lack of permissions.
                boolean permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) ==
                        PackageManager.PERMISSION_GRANTED;

                if (!(permissionCheck1 && permissionCheck2)) {
                    // If permissions are not already granted, request permission from the user.
                    ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, requestCode);
                } else {
                    // Report other unknown failure types to the user - for example, location services may not
                    // be enabled on the device.
                    String message = String.format("Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
                            .getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

                    buildAlertMessageNoGps();

                }
            }
        });


        // set an on touch listener to listen for click events
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {

                // get the point that was clicked and convert it to a point in map coordinates
                mClickPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());

                // clear any previous selection
                featureLayer.clearSelection();
                mSelectedArcGISFeature = null;
                // identify the GeoElements in the given layer
                final ListenableFuture<IdentifyLayerResult> future = mMapView.identifyLayerAsync(featureLayer, mClickPoint, 5, 1);

                // add done loading listener to fire when the selection returns
                future.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // call get on the future to get the result
                            IdentifyLayerResult result = future.get();

                            List<GeoElement> resultGeoElements = result.getIdentifiedElements();
                            if (resultGeoElements.size() >0) {
                                if (resultGeoElements.get(0) instanceof ArcGISFeature) {
                                    mSelectedArcGISFeature = (ArcGISFeature) resultGeoElements.get(0);
                                    // highlight the selected feature
                                    featureLayer.selectFeature(mSelectedArcGISFeature);
                                    Toast.makeText(getApplicationContext(), "Tapped on feature", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                        }
                    }
                });
                return super.onSingleTapConfirmed(e);
            }
        });

        mLocationFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mLocationDisplay.isStarted()) {
                    mLocationDisplay.startAsync();
                }
                if(mLocationDisplay.isStarted()) {
                    Log.d(TAG,"showLocation");
                    Point currentLocation = mLocationDisplay.getMapLocation();
                    Viewpoint vp = new Viewpoint(currentLocation, 5e3);
                    mMapView.setViewpointWithDurationAsync(vp, 3);
                }
            }
        });



    }

    @Override
    protected void onPause(){
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapView.resume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission was granted. This would have been triggered in response to failing to start the
            // LocationDisplay, so try starting this again.
            mLocationDisplay.startAsync();
        } else {
            // If permission was denied, show toast to inform user what was chosen. If LocationDisplay is started again,
            // request permission UX will be shown again, option should be shown to allow never showing the UX again.
            // Alternative would be to disable functionality so request is not shown again.
            Toast.makeText(MainActivity.this, getResources().getString(R.string.location_permission_denied), Toast
                    .LENGTH_SHORT).show();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.MyAlertDialogStyle);
        builder.setMessage("Please enable your GPS before proceeding")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private static class CustomSuggestionItemAnimator extends BaseItemAnimator {

        private final static Interpolator INTERPOLATOR_ADD = new DecelerateInterpolator(3f);
        private final static Interpolator INTERPOLATOR_REMOVE = new AccelerateInterpolator(3f);

        private final FloatingSearchView mSearchView;

        public CustomSuggestionItemAnimator(FloatingSearchView searchView) {
            mSearchView = searchView;
            setAddDuration(150);
            setRemoveDuration(150);
        }

        @Override
        protected void preAnimateAdd(RecyclerView.ViewHolder holder) {
            if(!mSearchView.isActivated()) return;
            ViewCompat.setTranslationX(holder.itemView, 0);
            ViewCompat.setTranslationY(holder.itemView, -holder.itemView.getHeight());
            ViewCompat.setAlpha(holder.itemView, 0);
        }

        @Override
        protected ViewPropertyAnimatorCompat onAnimateAdd(RecyclerView.ViewHolder holder) {
            if(!mSearchView.isActivated()) return null;
            return ViewCompat.animate(holder.itemView)
                    .translationY(0)
                    .alpha(1)
                    .setStartDelay((getAddDuration() / 2) * holder.getLayoutPosition())
                    .setInterpolator(INTERPOLATOR_ADD);
        }

        @Override
        public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
            dispatchMoveFinished(holder);
            return false;
        }

        @Override
        protected ViewPropertyAnimatorCompat onAnimateRemove(RecyclerView.ViewHolder holder) {
            return ViewCompat.animate(holder.itemView)
                    .alpha(0)
                    .setStartDelay(0)
                    .setInterpolator(INTERPOLATOR_REMOVE);
        }
    }

    private static class CustomDrawable extends ToggleDrawable {

        public CustomDrawable(Context context) {
            super(context);
            float radius = ViewUtils.dpToPx(9);

            CrossModel cross = new CrossModel(radius*2);

            // From circle to cross
            add(Bezier.quadrant(radius, 0), cross.downLine);
            add(Bezier.quadrant(radius, 90), cross.upLine);
            add(Bezier.quadrant(radius, 180), cross.upLine);
            add(Bezier.quadrant(radius, 270), cross.downLine);
        }
    }

}
