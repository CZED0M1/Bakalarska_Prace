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
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.minimumInteractiveComponentSize
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
    private var isMenuClicked:Boolean = false
    private lateinit var parkingButton: ImageButton
    private lateinit var cancelButton: ImageButton
    private lateinit var addButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var clearDBButton: ImageButton
    private lateinit var cityButton: ImageButton
    private lateinit var importButton: ImageButton
    private lateinit var rectangle: View
    private lateinit var rectangleDown: View
    private var geoPoints = ArrayList<GeoPoint>()
    private var markers = ArrayList<Marker>()
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val permissionRequestCode =1001
    private lateinit var databaseManager:DatabaseManager
    private lateinit var databaseNameManager: DatabaseNameManager
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var progressBar: ProgressBar




    //TODO remove logs
    //TODO popisky polygonů
    //TODO edit polygon textu
    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    requestPermissionsIfNecessary(requiredPermissions)
    getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))



    setContentView(R.layout.activity_maps)
    //TODO problém <uses-permission android:name="android.permission.INTERNET" /> stahuje mapu
    //TODO tady byl problém, že je zastaralé https://stackoverflow.com/questions/56833657/preferencemanager-getdefaultsharedpreferences-deprecated-in-android-q

    //inflate and create the map
    map = findViewById(R.id.mapView)

    map.setTileSource(TileSourceFactory.MAPNIK)
    //TODO problém když nešel přidat obrázek a musel se měnit <LinearLayout na <RelativeLayout (překrytí)


    setButtons()


    addTapOverlay()

    setMap()
    databaseManager = DatabaseManager.getInstance(this@MapsActivity)
    databaseNameManager= DatabaseNameManager.getInstance(this@MapsActivity)
    addPolygons()


}

    private fun loadPoly() {
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
        polygon.id=poly.toString()
        polygon.setOnClickListener { _, _, _ -> // Obsluha kliknutí na polygon

            val input = EditText(map.context)

            AlertDialog.Builder(map.context)
                .setTitle("Název Polygonu")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val enteredText = input.text.toString()
                    databaseNameManager.insertPolygon(polygon.id.toInt(),enteredText)
                    map.overlays.removeAll { it is TextMarker }
                    loadMarks()
                    map.invalidate()
                }
                .setNegativeButton("Zrušit", null)
                .show()
            true
        }
        map.overlays.add(polygon)
        arrayGeo.clear()


    }
}
    private fun loadMarks() {
        val arrMarks = databaseNameManager.selectAll()
        arrMarks.forEach { (first, second) ->
            val selectedOverlay = map.overlays.find { overlay ->
                (overlay is Polygon) && (overlay.id?.toInt() == first)}
            if (selectedOverlay != null) {
                val selectedPolygon = selectedOverlay as Polygon
                val m = TextMarker(map, second, selectedPolygon.infoWindowLocation)
                map.overlays.add(m)


            }
        }
    }
    private fun addPolygons() {
        loadPoly()
        loadMarks()



        map.invalidate()
    }

    private fun initViews() {
        loadingOverlay = findViewById(R.id.loadingOverlay)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun showLoadingOverlay() {
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoadingOverlay() {
        loadingOverlay.visibility = View.GONE
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
        //TODO <a href="https://www.flaticon.com/free-icons/ui" title="ui icons">Ui icons created by khulqi Rosyid - Flaticon</a>
        menuButton = findViewById(R.id.menu)
        rectangleDown = findViewById(R.id.rectangleViewDown)
        clearDBButton = findViewById(R.id.clearDb)
        cityButton = findViewById(R.id.city)
        importButton = findViewById(R.id.upload)
        //TODO <a href="https://www.flaticon.com/free-icons/trash-can" title="trash can icons">Trash can icons created by Ehtisham Abid - Flaticon</a>
        //TODO <a href="https://www.flaticon.com/free-icons/publish" title="publish icons">Publish icons created by Laisa Islam Ani - Flaticon</a>
        //TODO <a href="https://www.flaticon.com/free-icons/town" title="town icons">Town icons created by Circlon Tech - Flaticon</a>

        cancelButton.translationY = -220f
        addButton.translationY = -220f
        undoButton.translationY = -220f
        rectangle.translationY = -220f
        rectangleDown.translationY = 220f
        cityButton.translationY = 220f
        clearDBButton.translationY = 220f
        importButton.translationY = 220f

        parkingButton.setOnClickListener {
            startAnimation()
            clearMapAndPoints()
            isParkClicked = !isParkClicked
            if (isParkClicked) Toast.makeText(map.context,"Vložte 3 a více bodů", Toast.LENGTH_LONG).show()
        }
        menuButton.setOnClickListener{
            startAnimationDown()
            isMenuClicked=!isMenuClicked
            if (isMenuClicked) menuButton.setImageResource(R.drawable.remove)
            //TODO change img
            else menuButton.setImageResource(R.drawable.menu)
        }
        clearDBButton.setOnClickListener{
           databaseManager.deleteAll()
            databaseNameManager.deleteAll()
            map.overlays.removeAll { it is Polygon }
            map.overlays.removeAll { it is TextMarker }


            map.invalidate()
        }
        cityButton.setOnClickListener{
            val intent = Intent(this, CityActivity::class.java)
            startActivity(intent)
        }
        importButton.setOnClickListener{
            val intent = Intent(this, ImportActivity::class.java)
            startActivity(intent)
            addPolygons()
            map.invalidate()
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
            addPolyOnClick()
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

    private fun addPolyOnClick() {
        val polygon = Polygon()
        geoPoints.add(geoPoints[0])
        polygon.fillPaint.color = Color.parseColor("#4EFF0000")
        polygon.outlinePaint.color = Color.parseColor("#4EFF0000")
        polygon.points = geoPoints
        polygon.id= databaseManager.getMaxPolygonId().toString()
        polygon.setOnClickListener { _, _, _ -> // Obsluha kliknutí na polygon

            val input = EditText(map.context)

            AlertDialog.Builder(map.context)
                .setTitle("Název Polygonu")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val enteredText = input.text.toString()
                    databaseNameManager.insertPolygon(polygon.id.toInt(),enteredText)
                    map.overlays.removeAll { it is TextMarker }
                    loadMarks()
                    map.invalidate()
                }
                .setNegativeButton("Zrušit", null)
                .show()
            true
        }

        map.overlays.add(polygon)
        val maxPolyId=databaseManager.getMaxPolygonId()
        for (i in 0..<geoPoints.size) {
            databaseManager.insertPolygon(maxPolyId+1,geoPoints[i].latitude,geoPoints[i].longitude)
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
        val translationYValue = if (isParkClicked) -220f else 0f

        val animator1 = ObjectAnimator.ofFloat(undoButton, "translationY", translationYValue)
        val animator2 = ObjectAnimator.ofFloat(cancelButton, "translationY", translationYValue)
        val animator3 = ObjectAnimator.ofFloat(addButton, "translationY", translationYValue)
        val animator4 = ObjectAnimator.ofFloat(rectangle, "translationY", translationYValue)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator1, animator2, animator3, animator4)
        animatorSet.duration = 500

        animatorSet.start()
    }
    private fun startAnimationDown() {
        val translationYValue = if (isMenuClicked) 220f else 0f
        val translationYValueBtn = if (isMenuClicked) 0f else -220f

        val animator1 = ObjectAnimator.ofFloat(menuButton, "translationY", translationYValueBtn)
        val animator2 = ObjectAnimator.ofFloat(parkingButton, "translationY", translationYValueBtn)
        val animator3 = ObjectAnimator.ofFloat(rectangleDown, "translationY", translationYValue)
        val animator4 = ObjectAnimator.ofFloat(clearDBButton, "translationY", translationYValue)
        val animator5 = ObjectAnimator.ofFloat(cityButton, "translationY", translationYValue)
        val animator6 = ObjectAnimator.ofFloat(importButton, "translationY", translationYValue)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator1,animator2,animator3,animator4,animator5,animator6)
        animatorSet.duration = 500

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
        builder.setMessage("Opravdu nechcete povolit zjištění polohy?")
        builder.setPositiveButton("Povolit") { _, _ ->
            // Uživatel klikl na Povolit, přejde do nastavení aplikace
            navigateToAppSettings(context)
        }
        builder.setNegativeButton("Zrušit") { dialog, _ ->
            // Uživatel klikl na Zrušit
            dialog.dismiss()
            initViews()
            loadingOverlay.visibility=View.GONE
            parkingButton.visibility=View.VISIBLE
            menuButton.visibility=View.VISIBLE

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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                        CancellationTokenSource().token

                    override fun isCancellationRequested() = false
                }
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    runOnUiThread {
                        initViews()
                        showLoadingOverlay()

                        val actualPosition = GeoPoint(location.latitude, location.longitude)

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

                        hideLoadingOverlay() // Skryje overlay s ProgressBar
                        parkingButton.visibility = View.VISIBLE
                        menuButton.visibility = View.VISIBLE
                    }
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