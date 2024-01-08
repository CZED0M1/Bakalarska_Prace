package com.example.bakalar

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {


    private val requestFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleSelectedFile(uri)
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        openFileChooser()
    openMap()
    }
    private fun openMap() {
        val openMap= findViewById<Button>(R.id.buttonMapa)
        openMap.setOnClickListener {
            val intent : Intent = Intent(this,MapsActivity::class.java)
            startActivity(intent)
        }
    }
    private fun openSpecificFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"  //TODO Set the MIME type to filter files
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Documents")
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }
        requestFileLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: android.net.Uri) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                //TODO tady byl problém napsat do dokumentace - SuppressLint("Range")
                //https://stackoverflow.com/questions/71338033/android-sqlite-value-must-be-≥-0-getcolumnindex-kotlin?answert
                println(displayName)
            }
        }
    }

   private fun openFileChooser() {
       val openImport= findViewById<Button>(R.id.buttonImport)
       openImport.setOnClickListener {
           openSpecificFolder()
       }
    }

}