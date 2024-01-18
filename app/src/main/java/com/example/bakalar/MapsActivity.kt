package com.example.bakalar


import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.testosmroid.R
<<<<<<< Updated upstream
=======
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
>>>>>>> Stashed changes
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
<<<<<<< Updated upstream
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2


class MapsActivity : AppCompatActivity() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map : MapView
=======


class MapsActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: MapView
    private var isParkClicked: Boolean = false
    private lateinit var parkingButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var addButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var rectangle: View
    private var geoPoints = ArrayList<GeoPoint>()
    private var markers = ArrayList<Marker>()
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val permissionRequestCode =1001
//TODO Databáze
//TODO remove logs
>>>>>>> Stashed changes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        // This won't work unless you have imported this: org.osmdroid.config.Configuration.*
        //TODO tady byl problém, že je zastaralé https://stackoverflow.com/questions/56833657/preferencemanager-getdefaultsharedpreferences-deprecated-in-android-q
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
        //TODO problém když nešel přidat obrázek a musel se měnit <LinearLayout na <RelativeLayout (překrytí)
        //TODO image:<a href="https://www.flaticon.com/free-icons/parking" title="parking icons">Parking icons created by Bartama Graphic - Flaticon</a>
        val myButton : ImageButton = findViewById(R.id.myButton)
        myButton.setOnClickListener {
            Toast.makeText(
                this@MapsActivity,
                "Tlačítko bylo stisknuto",
                Toast.LENGTH_SHORT
            ).show()
        }

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


        val marker = Marker(map)
        marker.position = GeoPoint(48.8583, 2.2944)
        marker.icon = ContextCompat.getDrawable(this@MapsActivity, org.osmdroid.library.R.drawable.marker_default)
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





        val geoPoints = ArrayList<GeoPoint>()
//add your points here
        val polygon = Polygon()    //see note below
        geoPoints.add(GeoPoint(48.8583, 2.2944))
        geoPoints.add(GeoPoint(48.8583, 2.3944))
        geoPoints.add(GeoPoint(49.7583, 2.1944))
        geoPoints.add(geoPoints[0])   //forces the loop to close(connect last point to first point)
        polygon.fillPaint.color = Color.parseColor("#4EFF0000") //set fill color
        polygon.points = geoPoints
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
<<<<<<< Updated upstream
=======
    private fun showPermissionExplanationDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Povolení potřebná pro aplikaci")
        builder.setMessage("Aplikace potřebuje povolení pro zjištění polohy")
        builder.setPositiveButton("Povolit") { _, _ ->
            // Uživatel klikl na Povolit, přejde do nastavení aplikace
            navigateToAppSettings(context)
        }
        builder.setNegativeButton("Zrušit") { dialog, _ ->
            // Uživatel klikl na Zrušit
            dialog.dismiss()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun navigateToAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = android.net.Uri.parse("package:" + context.packageName)
        context.startActivity(intent)
    }
>>>>>>> Stashed changes


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
<<<<<<< Updated upstream
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE)
=======

        if (permissionsToRequest.isNotEmpty()) {
            showPermissionExplanationDialog(this)
            Log.d("PermissionDenied", "Uživatel klikl na 'nepovolit'")
            permissionsToRequest.clear()
        } else {
            initializeLocation() // Requested permissions granted, initialize the location
        }
    }

    private fun initializeLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permissions for location are granted, proceed with location actions
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token

                    override fun isCancellationRequested() = false
                }
            ).addOnSuccessListener { location: Location? ->
                if (location == null) {
                    Log.d("PermissionDenied", "Poloha null")
                } else {
                    Log.d("PermissionDenied", "Poloha not null")
                    val lat = location.latitude
                    val lon = location.longitude

                    val actualPosition = GeoPoint(lat, lon)

                    map.controller.setCenter(actualPosition)

                    val marker = Marker(map)
                    marker.position = actualPosition

                    marker.icon = ContextCompat.getDrawable(
                        this@MapsActivity,
                        org.osmdroid.library.R.drawable.person
                    )
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    map.overlays.add(marker)
                    map.invalidate()
                }
            }
        }
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        Log.d("PermissionDenied", "requestPermissionsIfNecessary")

        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        } else {
            initializeLocation() // Permissions already granted, initialize the location
>>>>>>> Stashed changes
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
