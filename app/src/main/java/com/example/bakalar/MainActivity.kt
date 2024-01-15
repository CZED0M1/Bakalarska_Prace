package com.example.bakalar

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.MimeTypeFilter
import com.example.testosmroid.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        openMap()
        openFileChooser()
    }

    private fun openMap() {
        val openMap = findViewById<Button>(R.id.buttonMapa)
        openMap.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
    private fun openFileChooser() {
        val openFile = findViewById<Button>(R.id.buttonImport)
        openFile.setOnClickListener {
            val intent = Intent(this, ImportActivity::class.java)
            startActivity(intent)
        }
    }


}