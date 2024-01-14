package com.example.bakalar

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testosmroid.R
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2


class MapsActivity : AppCompatActivity() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map : MapView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        // This won't work unless you have imported this: org.osmdroid.config.Configuration.*
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, if you abuse osm's
        //tile servers will get you banned based on this string.

        //inflate and create the map



        setContentView(R.layout.activity_maps)

        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)


        //TODO problém napsat do bc
        map.maxZoomLevel = 20.0
        map.minZoomLevel = 4.0
        map.setScrollableAreaLimitLatitude(
            MapView.getTileSystem().maxLatitude,
            MapView.getTileSystem().minLatitude,
            0
        )
        map.setScrollableAreaLimitLongitude(

            MapView.getTileSystem().minLongitude,
            MapView.getTileSystem().maxLongitude,
            0
        )

        val mapController= map.controller
        mapController.setZoom(9.5)
        val startPoint = GeoPoint(48.8583, 2.2944)
        mapController.setCenter(startPoint)




        val overlay = LatLonGridlineOverlay2()
        map.overlays.add(overlay)

        val rotationGestureOverlay = RotationGestureOverlay(map)
        rotationGestureOverlay.isEnabled
        map.setMultiTouchControls(true)
        map.overlays.add(rotationGestureOverlay)


        var marker = Marker(map)
        marker.position = GeoPoint(48.8583, 2.2944)
        marker.icon = ContextCompat.getDrawable(this@MapsActivity, R.drawable.marker_default)
        marker.title = "ImportActivity Marker"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(marker)
        map.invalidate()


        val tapOverlay = MapEventsOverlay(object: MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                Toast.makeText(
                    baseContext,"short"+
                            p?.latitude.toString() + " - " + p?.longitude,
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean {
                return true
            }
        })
        map.overlays.add(tapOverlay)





        val geoPoints = ArrayList<GeoPoint>();
//add your points here
        val polygon = Polygon();    //see note below
        geoPoints.add(GeoPoint(48.8583, 2.2944))
        geoPoints.add(GeoPoint(48.8583, 2.3944))
        geoPoints.add(GeoPoint(49.7583, 2.1944))
        geoPoints.add(geoPoints.get(0));    //forces the loop to close(connect last point to first point)
        polygon.fillPaint.color = Color.parseColor("#4EFF0000") //set fill color
        polygon.setPoints(geoPoints);
        polygon.title = "A sample polygon"

//polygons supports holes too, points should be in a counter-clockwise order
        val holes = ArrayList<ArrayList<GeoPoint>>()
// Note, you will have to create "moreGeoPoints" yourself.
        // holes.add(moreGeoPoints)
        polygon.setHoles(holes)

        map.overlays.add(polygon)

    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    /*
    private fun requestPermissionsIfNecessary(permissions : Array<String>) {
        val permissionsToRequest = ArrayList<String>()
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toArray(arrayOf<String>()),
                REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
     */






}
