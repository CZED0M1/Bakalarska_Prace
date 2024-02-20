package com.example.bakalar


import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.testosmroid.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay


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
    private lateinit var databaseManager:DatabaseManager
    //TODO remove logs
    //TODO popisky polygonů
    //TODO edit polygon textu
    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    requestPermissionsIfNecessary(requiredPermissions)
    getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

    setContentView(R.layout.activity_maps)

    //handle permissions first, before map is created. not depicted here
    //load/initialize the osmdroid configuration, this can be done
    // This won't work unless you have imported this: org.osmdroid.config.Configuration.*
    //TODO problém <uses-permission android:name="android.permission.INTERNET" /> stahuje mapu
    //TODO tady byl problém, že je zastaralé https://stackoverflow.com/questions/56833657/preferencemanager-getdefaultsharedpreferences-deprecated-in-android-q
    //setting this before the layout is inflated is a good idea
    //it 'should' ensure that the map has a writable location for the map cache, even without permissions
    //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
    //see also StorageUtils
    //note, the load method also sets the HTTP User Agent to your application's package name, if you abuse osm's
    //tile servers will get you banned based on this string.

    //inflate and create the map
    map = findViewById(R.id.mapView)
    map.setTileSource(TileSourceFactory.MAPNIK)
    //TODO problém když nešel přidat obrázek a musel se měnit <LinearLayout na <RelativeLayout (překrytí)


    setButtons()


    addTapOverlay()

    setMap()
    databaseManager = DatabaseManager.getInstance(this@MapsActivity)
    addPolygons()

}


    private fun addPolygons() {
        val arr: ArrayList<PolygonGeopoint> = databaseManager.selectAll()
        val polygons = arr.map { it.polygonId }.distinct()
        polygons.forEach { poly ->
            val samePolygonId = arr.filter { pol ->
                pol.polygonId == poly
            }
            val arrayGeo=arrayListOf<GeoPoint>()
            samePolygonId.forEach {
                arrayGeo.add(GeoPoint(it.latitude,it.longitude))
            }
                val polygon = Polygon()
                polygon.fillPaint.color = Color.parseColor("#4EFF0000") //set fill color
                polygon.outlinePaint.color = Color.parseColor("#4EFF0000")
                polygon.points = arrayGeo

                map.overlays.add(polygon)
                arrayGeo.clear()


        }
        map.invalidate()
    }



    private fun setMap() {

    //TODO problém napsat do bc více map, opraveno
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
    val mapController = map.controller


    mapController.setZoom(19.0)


    val rotationGestureOverlay = RotationGestureOverlay(map)
    rotationGestureOverlay.isEnabled
    map.setMultiTouchControls(true)
    map.overlays.add(rotationGestureOverlay)
}
    private fun setButtons() {
        parkingButton = findViewById(R.id.addParking)
        //TODO <a href="https://www.flaticon.com/free-icons/parking" title="parking icons">Parking icons created by Bartama Graphic - Flaticon</a>
        cancelButton = findViewById(R.id.cancel)
        //TODO <a href="https://www.flaticon.com/free-icons/delete" title="delete icons">Delete icons created by Pixel perfect - Flaticon</a>
        addButton = findViewById(R.id.approve)
        //TODO <a href="https://www.flaticon.com/free-icons/yes" title="yes icons">Yes icons created by juicy_fish - Flaticon</a>
        undoButton = findViewById(R.id.undo)
        //TODO <a href="https://www.flaticon.com/free-icons/back" title="back icons">Back icons created by Roundicons - Flaticon</a>
        rectangle = findViewById(R.id.rectangleView)

        cancelButton.translationY = -600f
        addButton.translationY = -600f
        undoButton.translationY = -600f
        rectangle.translationY = -600f

        parkingButton.setOnClickListener {
            startAnimation()
            clearMapAndPoints()
            isParkClicked = !isParkClicked
        }

        undoButton.setOnClickListener {
            if (markers.size == 0) return@setOnClickListener
            geoPoints.removeLast()
            markers.removeLast()
            map.overlays.removeLast()
            map.invalidate()
        }
        addButton.setOnClickListener {
            if (markers.size > 1) {
                val polygon = Polygon()    //see note below
                geoPoints.add(geoPoints[0])   //forces the loop to close(connect last point to first point)
                polygon.fillPaint.color = Color.parseColor("#4EFF0000") //set fill color
                polygon.outlinePaint.color=Color.parseColor("#4EFF0000")
                polygon.points = geoPoints
                map.overlays.add(polygon)
                val maxPolyId=databaseManager.getMaxPolygonId()
                for (i in 0..<geoPoints.size) {
                    databaseManager.insertPolygon(maxPolyId+1,geoPoints[i].latitude,geoPoints[i].longitude)
                }

                clearMapAndPoints()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Jsou potřeba alespoň 2 body!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        cancelButton.setOnClickListener {
            clearMapAndPoints()
        }
    }

    private fun addTapOverlay() {
        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (!isParkClicked) return false
                if (p != null) {
                    val marker = Marker(map)
                    marker.position = GeoPoint(p.latitude, p.longitude)
                    geoPoints.add(GeoPoint(p.latitude, p.longitude))
                    marker.icon = ContextCompat.getDrawable(
                        this@MapsActivity,
                        org.osmdroid.library.R.drawable.marker_default
                    )
                    marker.title = "Bod ${markers.size}"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    markers.add(marker)
                    map.overlays.add(marker)
                    map.invalidate()
                }


                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return true
            }
        })
        map.overlays.add(tapOverlay)
    }

    private fun clearMapAndPoints() {
        geoPoints.clear()
        for (marker in markers) {
            map.overlays.remove(marker)
        }
        markers.clear()
        map.invalidate()
    }

    private fun startAnimation() {
        val translationYValue = if (isParkClicked) -600f else 0f

        val animator1 = ObjectAnimator.ofFloat(undoButton, "translationY", translationYValue)
        val animator2 = ObjectAnimator.ofFloat(cancelButton, "translationY", translationYValue)
        val animator3 = ObjectAnimator.ofFloat(addButton, "translationY", translationYValue)
        val animator4 = ObjectAnimator.ofFloat(rectangle, "translationY", translationYValue)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator1, animator2, animator3, animator4)
        animatorSet.duration = 1000

        animatorSet.start()
    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
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
    private fun showPermissionExplanationDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Povolení potřebná pro aplikaci")
        builder.setMessage("Aplikace potřebuje povolení pro ...")
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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val permissionsToRequest = ArrayList<String>()
        for (i in grantResults.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permissions[i])
                break
            }
        }

        if (permissionsToRequest.isNotEmpty()) {

            showPermissionExplanationDialog(this)
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
                if (location != null) {
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
        }
    }




}