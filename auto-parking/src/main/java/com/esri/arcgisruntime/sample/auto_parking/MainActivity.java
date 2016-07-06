package com.esri.arcgisruntime.sample.auto_parking;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.datasource.FeatureQueryResult;
import com.esri.arcgisruntime.datasource.QueryParameters;
import com.esri.arcgisruntime.datasource.arcgis.ArcGISFeature;
import com.esri.arcgisruntime.datasource.arcgis.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AutoParking";
    private MapView mMapView;
    private ArcGISMap mMap;
    private Portal mPortal;
    private PortalItem mPortalItem;
    private android.graphics.Point mClickPoint;
    private ArcGISFeature mSelectedArcGISFeature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // inflate MapView from layout
        mMapView = (MapView) findViewById(R.id.mapView);
        // create a map with the BasemapType topographic
        //ArcGISMap mMap = new ArcGISMap(Basemap.createStreets());

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

        //{"x":-13044550,"y":4040393,"spatialReference":{"wkid":102100}}

        mMap.setInitialViewpoint(new Viewpoint(new Point(-13044550, 4040393, SpatialReferences.getWebMercator()), 5e4));


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
}
