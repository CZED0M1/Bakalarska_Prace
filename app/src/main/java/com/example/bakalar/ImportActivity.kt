package com.example.bakalar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.testosmroid.R

// Request code for selecting a PDF document.
const val PICK_FILE = 2
class ImportActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)
        openFile(null)
    }



    private fun openFile(pickerInitialUri: Uri?) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            //TODO problém tu s application/json geo+json
            type = "application/octet-stream"
            putExtra(DocumentsContract.EXTRA_INITIAL_URI+"/Downloads", pickerInitialUri)
        }

        startActivityForResult(intent, PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (data != null) {
                val selectedFileUri = data.data
                Log.d("myTag", selectedFileUri.toString());
            }
        }
    }
}