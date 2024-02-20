package com.example.bakalar
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.testosmroid.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        openMap()
        openFileChooser()
        clearDatabase()
        openCity()
    }

    private fun clearDatabase() {
        val clearDb = findViewById<Button>(R.id.buttonSmazat)
        clearDb.setOnClickListener {
            val manager = DatabaseManager.getInstance(this@MainActivity)
            manager.deleteAll()
        }
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
    private fun openCity() {
        val openCity = findViewById<Button>(R.id.buttonMesta)
        openCity.setOnClickListener {
            val intent = Intent(this, CityActivity::class.java)
            startActivity(intent)
        }
    }


}