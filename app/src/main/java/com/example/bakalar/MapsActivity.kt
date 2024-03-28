package com.example.bakalar


import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.net.Uri
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


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



    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    requestPermissionsIfNecessary(requiredPermissions)
    getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))



    setContentView(R.layout.activity_maps)
    //TODO problém <uses-permission android:name="android.permission.INTERNET" /> stahuje mapu
    //TODO tady byl problém, že je zastaralé https://stackoverflow.com/questions/56833657/preferencemanager-getdefaultsharedpreferences-deprecated-in-android-q

    map = findViewById(R.id.mapView)

    map.setTileSource(TileSourceFactory.MAPNIK)


        //TODO problém když nešel přidat obrázek a musel se měnit <LinearLayout na <RelativeLayout (překrytí)


    setButtons()


    addTapOverlay()

    setMap()
        createDb()
        addPolygons()


}

    private fun createDb() {
        val databaseFile =
            File(this@MapsActivity.getDatabasePath(DatabaseManager.DATABASE_NAME).path)
        databaseManager = DatabaseManager.getInstance(this@MapsActivity)
        databaseNameManager = DatabaseNameManager.getInstance(this@MapsActivity)
        if (!databaseFile.exists()) {
            val files = arrayOf(
                File(this@MapsActivity.filesDir, "startData/parkingFirst.geojson"),
                File(this@MapsActivity.filesDir, "startData/parkingSecond.geojson")
            )
            for (file in files) readGeoJson(Uri.fromFile(file))
        }
    }

    private fun readGeoJson(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.readText()
        val jsonObject = JSONObject(jsonString)
        val featuresArray = jsonObject.getJSONArray("features")
        if (featuresArray.length() > 0) {
            for (i in 0 until featuresArray.length()) {
                val highestPolygonId = databaseManager.getMaxPolygonId()
                val feature = featuresArray.getJSONObject(i)
                val geometry = feature.getJSONObject("geometry")
                val coordinatesArray = geometry.getJSONArray("coordinates")

                val polygonPoints = mutableListOf<Pair<Double, Double>>()
                loadPolygon(coordinatesArray, polygonPoints)
                checkPolyDuplicity(polygonPoints, highestPolygonId)
            }
        }
    }
    private fun loadPolygon(
        coordinatesArray: JSONArray,
        polygonPoints: MutableList<Pair<Double, Double>>
    ) {
        for (j in 0 until coordinatesArray.length()) {
            if (coordinatesArray.length() > 0) {
                val firstCoordinate =
                    coordinatesArray.getJSONArray(j)
                if (firstCoordinate.length() == 2) {
                    val latitude = firstCoordinate.getDouble(1)
                    val longitude = firstCoordinate.getDouble(0)
                    polygonPoints.add(Pair(latitude, longitude))
                }
            }
        }
    }
    private fun checkPolyDuplicity(
        polygonPoints: MutableList<Pair<Double, Double>>,
        highestPolygonId: Int
    ) {


            Log.d("testing","jeede")
            for (point in polygonPoints) {
                databaseManager.insertPolygon(
                    highestPolygonId + 1,
                    point.first,
                    point.second
                )
            }
            polygonPoints.clear()

    }

    private fun loadPoly() {
        val arr: ArrayList<PolygonGeopoint> = databaseManager.selectAll()

        val polygons = arr.map { it.polygonId }.distinct()
        polygons.forEach { poly ->
            val samePolygonId = arr.filter { pol ->
                pol.polygonId == poly
            }
            val arrayGeo = arrayListOf<GeoPoint>()
            samePolygonId.forEach {
                arrayGeo.add(GeoPoint(it.latitude, it.longitude))
            }
            val polygon = Polygon()
            polygon.fillPaint.color = Color.parseColor("#D73A2B72")
            polygon.outlinePaint.color = Color.parseColor("#D73A2B72")
            polygon.points = arrayGeo
            polygon.id = poly.toString()
            
            polygon.setOnClickListener { _, _, _ ->
                val input = EditText(map.context)

                val alertDialog = AlertDialog.Builder(map.context)
                    .setTitle("Název Polygonu")
                    .setView(input)
                    .setPositiveButton("OK") { _, _ ->
                        val enteredText = input.text.toString()
                        databaseNameManager.insertPolygon(polygon.id.toInt(), enteredText)
                        map.overlays.removeAll { it is TextMarker }
                        loadMarks()
                        map.invalidate()
                    }
                    .setNegativeButton("Zrušit", null)
                    .create()


                alertDialog.show()
                false
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
        cancelButton = findViewById(R.id.cancel)
        addButton = findViewById(R.id.approve)
        undoButton = findViewById(R.id.undo)

        rectangle = findViewById(R.id.rectangleView)
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = 32f
        gradientDrawable.colors = intArrayOf(0xFF222354.toInt(),0xFF3A2B72.toInt())
        gradientDrawable.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        rectangle.background = gradientDrawable

        menuButton = findViewById(R.id.menu)

        rectangleDown = findViewById(R.id.rectangleViewDown)
        val gradientDrawableDown = GradientDrawable()
        gradientDrawableDown.shape = GradientDrawable.RECTANGLE
        gradientDrawableDown.cornerRadius = 32f
        gradientDrawableDown.colors = intArrayOf(0xFF222354.toInt(),0xFF3A2B72.toInt())
        gradientDrawableDown.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        rectangleDown.background = gradientDrawableDown


        clearDBButton = findViewById(R.id.clearDb)
        importButton = findViewById(R.id.upload)
        cancelButton.translationY = -220f
        addButton.translationY = -220f
        undoButton.translationY = -220f
        rectangle.translationY = -220f
        rectangleDown.translationY = 220f
        clearDBButton.translationY = 220f
        importButton.translationY = 220f

        parkingButton.setOnClickListener {
            parkingButtonFunction()
        }
        menuButton.setOnClickListener{
            menuButtonFunction()
        }
        clearDBButton.setOnClickListener{
           databaseManager.deleteAll()
            databaseNameManager.deleteAll()
            map.overlays.removeAll { it is Polygon }
            map.overlays.removeAll { it is TextMarker }


            map.invalidate()
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

    private fun parkingButtonFunction() {
        startAnimation()
        if (isMenuClicked) menuButtonFunction()
        clearMapAndPoints()
        isParkClicked = !isParkClicked
        if (isParkClicked) Toast.makeText(map.context, "Vložte 3 a více bodů", Toast.LENGTH_LONG)
            .show()
    }

    private fun menuButtonFunction() {
        if (isParkClicked) parkingButtonFunction()
        startAnimationDown()
        isMenuClicked = !isMenuClicked
        if (isMenuClicked) menuButton.setImageResource(R.drawable.close)
        else menuButton.setImageResource(R.drawable.menu)
    }

    private fun addPolyOnClick() {
        val polygon = Polygon()
        geoPoints.add(geoPoints[0])
        polygon.fillPaint.color = Color.parseColor("#D73A2B72")
        polygon.outlinePaint.color = Color.parseColor("#D73A2B72")
        polygon.points = geoPoints
        val maxId=databaseManager.getMaxPolygonId()
        polygon.id= (maxId+1).toString()
        polygon.setOnClickListener { _, _, _ ->

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
                        R.drawable.parking_marker
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

        val animator3 = ObjectAnimator.ofFloat(rectangleDown, "translationY", translationYValue)
        val animator4 = ObjectAnimator.ofFloat(clearDBButton, "translationY", translationYValue)
        val animator5 = ObjectAnimator.ofFloat(importButton, "translationY", translationYValue)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator3,animator4,animator5)
        animatorSet.duration = 500

        animatorSet.start()
    }

    override fun onResume() {
        super.onResume()
        map.overlays.removeAll { it is Polygon }
        map.overlays.removeAll { it is TextMarker }
        addPolygons()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
    private fun showPermissionExplanationDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Povolení aktuální polohy")
        builder.setMessage("Opravdu nechcete povolit zjištění polohy?")
        builder.setPositiveButton("Povolit") { _, _ ->
            navigateToAppSettings(context)
        }
        builder.setNegativeButton("Zrušit") { dialog, _ ->
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
        intent.data = Uri.parse("package:" + context.packageName)
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
            initializeLocation()
        }
    }

    private fun initializeLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED //kontrola udělení povolení
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
                    runOnUiThread { //načítání
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
                        hideLoadingOverlay()
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
            initializeLocation()
        }
    }




}