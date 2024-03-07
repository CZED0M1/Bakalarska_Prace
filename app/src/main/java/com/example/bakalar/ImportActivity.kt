package com.example.bakalar

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.example.testosmroid.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class ImportActivity : AppCompatActivity() {

    private val fileRequestCode = 1
    private lateinit var databaseManager: DatabaseManager
    private var inputStream: InputStream? = null
    private lateinit var reader: BufferedReader
    private lateinit var progressBar: ProgressBar
    private val loadingLiveData = MutableLiveData<Boolean>()
    private lateinit var loadingOverlay: FrameLayout

    private val filePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onActivityRes(result.resultCode, result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        loadingOverlay = findViewById(R.id.loadingOverlay)
        progressBar = findViewById(R.id.progressBar)
        loadingLiveData.observe(this) { isLoading ->
            if (isLoading) {
                loadingOverlay.visibility = View.VISIBLE
            } else {
                loadingOverlay.visibility = View.GONE
            }
        }

        checkPermissionAndOpenFileExplorer()
    }

    private fun selectFile() {
        databaseManager = DatabaseManager.getInstance(this@ImportActivity)
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"

        filePickerLauncher.launch(intent)
    }

    private fun checkPermissionAndOpenFileExplorer() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showPermissionRationaleDialog()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    fileRequestCode
                )
            }
        } else {
            selectFile()
        }
    }

    private fun showPermissionRationaleDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Oprávnění k procházení souborů")
            .setMessage("Tato aplikace vyžaduje oprávnění pro procházení souborů. Povolte oprávnění v nastavení aplikace.")
            .setPositiveButton("Povolit") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    fileRequestCode
                )
            }.setNegativeButton("Zrušit") { dialog, _ ->
                dialog.dismiss()
                finish()
            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == fileRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectFile()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Odmítnuto oprávnění")
            .setMessage("Pro procházení souborů je potřeba udělit oprávnění.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                redirectToAppSettings()
            }.show()
    }

    private fun redirectToAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
    private fun isGeoJsonFile(contentUri: Uri): Boolean {
        try {
            val inputStream = contentResolver.openInputStream(contentUri)
            val buffer = ByteArray(4096)
            val bytesRead = inputStream?.read(buffer)

            if (bytesRead != null && bytesRead > 0) {
                val fileContent = String(buffer, 0, bytesRead)
                inputStream.close()
                // Kontrola, zda obsah souboru obsahuje typické znaky GeoJSON formátu
                return fileContent.contains("\"type\": \"FeatureCollection\"") ||
                        fileContent.contains("\"type\": \"Feature\"") ||
                        fileContent.contains("\"type\": \"Polygon\"") ||
                        fileContent.contains("\"type\": \"Point\"")

            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return false
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun onActivityRes(resultCode: Int, data: Intent?) {
        if(data==null) {
            finish()
            return
        }
            if (resultCode == Activity.RESULT_OK) {
                if (!isGeoJsonFile(data.data!!)) {
                    finish()
                    Toast.makeText(applicationContext,"Tento soubor není ve formátu .geojson",Toast.LENGTH_SHORT).show()
                    return
                }
                data.data?.let { uri ->
                    loadingLiveData.value = true
                    GlobalScope.launch(Dispatchers.IO) {
                        inputStream = contentResolver.openInputStream(uri)
                        reader = BufferedReader(InputStreamReader(inputStream))
                        try {
                            inputStream?.use { _ ->
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

                                        // Kontrola duplicity polygonu před vložením do databáze
                                        if (!databaseManager.isPolygonInDatabase(polygonPoints)) {
                                            for (point in polygonPoints) {
                                                // Vložení nového bodu polygonu do databáze
                                                databaseManager.insertPolygon(
                                                    highestPolygonId + 1,
                                                    point.first,
                                                    point.second
                                                )
                                            }
                                            polygonPoints.clear()
                                        } else {
                                        }
                                    }
                                }
                            }
                        }catch(err:Exception) {
                            Log.e("Reading file error",err.toString())

                        }
                        reader.close()
                        inputStream?.close()

                        withContext(Dispatchers.Main) {
                            loadingLiveData.value = false
                            finish()
                        }
                    }
                    }
            } else {
                loadingLiveData.value = false
                finish()
            }
        }





}
