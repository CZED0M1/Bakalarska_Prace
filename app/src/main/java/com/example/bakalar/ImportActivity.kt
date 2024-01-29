package com.example.bakalar

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testosmroid.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ImportActivity : AppCompatActivity() {

    private val fileRequestCode = 1
    private lateinit var databaseManager: DatabaseManager
    private var inputStream: InputStream? = null
    private lateinit var reader: BufferedReader

    private val filePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onActivityRes(result.resultCode, result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)
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

    private fun onActivityRes(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                inputStream = contentResolver.openInputStream(uri)
                reader = BufferedReader(InputStreamReader(inputStream))
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
                            for (j in 0 until coordinatesArray.length()) {
                                if (coordinatesArray.length() > 0) {
                                    val firstCoordinate = coordinatesArray.getJSONArray(j)
                                    if (firstCoordinate.length() == 2) {

                                        val latitude = firstCoordinate.getDouble(1)
                                        val longitude = firstCoordinate.getDouble(0)
                                        insertToDb(highestPolygonId + 1, latitude, longitude)
                                    }
                                }
                            }
                        }
                    }
                }
                reader.close()
                inputStream?.close()
                finish()
            }
        } else {
            finish()
        }
    }


    private fun insertToDb(highestPolygonId: Int, latitude: Double, longitude: Double) {
        val insertedId = databaseManager.insertPolygon(highestPolygonId, latitude, longitude)
        if (insertedId != -1L) {
            Log.d("FileReader", "Done")
        } else {
            Log.d("FileReader", "Chyba")
        }
    }
}
