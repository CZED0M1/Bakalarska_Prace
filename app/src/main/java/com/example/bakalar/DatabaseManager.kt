package com.example.bakalar

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DatabaseManager private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        @Volatile
        private var instance: DatabaseManager? = null
        const val DATABASE_NAME = "map_polygons_database.db"
        const val DATABASE_VERSION = 1

        fun getInstance(context: Context): DatabaseManager {
            return instance ?: synchronized(this) {
                instance ?: DatabaseManager(context.applicationContext).also { instance = it }
            }
        }
    }
    fun isPolygonInDatabase(polygonPoints: List<Pair<Double, Double>>): Boolean {
        // Získání maximálního polygonId z databáze
        val maxPolygonId = getMaxPolygonId()

        // Pro každý možný polygonId v databázi
        for (currentPolygonId in 1..maxPolygonId) {
            val query = "SELECT latitude, longitude FROM map_polygons_database WHERE polygon_id = $currentPolygonId"
            val cursor = readableDatabase.rawQuery(query, null)

            val polygonPointsInDatabase = mutableListOf<Pair<Double, Double>>()

            // Načtení všech bodů pro aktuální polygonId z databáze
            while (cursor.moveToNext()) {
                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                polygonPointsInDatabase.add(Pair(latitude, longitude))
            }

            cursor.close()

            // Porovnání bodů z databáze s aktuálním polygonem
            if (polygonPointsInDatabase == polygonPoints) {
                return true // Nalezen odpovídající polygon
            }
        }

        return false // žádný odpovídající polygon nenalezen
    }
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE IF NOT EXISTS map_polygons_database (id INTEGER PRIMARY KEY, polygon_id INTEGER, latitude REAL, longitude REAL)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    fun insertPolygon(polyId: Int, latitude: Double, longitude: Double): Long {
        val contentValues = ContentValues()
        contentValues.put("polygon_id", polyId)
        contentValues.put("latitude", latitude)
        contentValues.put("longitude", longitude)

        return writableDatabase.insertOrThrow("map_polygons_database", null, contentValues)

    }

    fun getMaxPolygonId(): Int {
        val query = "SELECT MAX(polygon_id) FROM map_polygons_database"
        val cursor = readableDatabase.rawQuery(query, null)
        var highestPolygonId = 0
        if (cursor.moveToFirst()) {
            highestPolygonId = cursor.getInt(0)
        }
        cursor.close()
        return highestPolygonId
    }

    fun selectAll(): ArrayList<PolygonGeopoint> {
        val arr = arrayListOf<PolygonGeopoint>()
        val query = "SELECT * FROM map_polygons_database"
        val cursor = readableDatabase.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val polygonId = cursor.getInt(cursor.getColumnIndexOrThrow("polygon_id"))
            val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
            val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
            val polygonGeo = PolygonGeopoint(latitude,longitude,polygonId)
            arr.add(polygonGeo)
        }

        cursor.close()
        return arr
    }

    fun deleteAll() {
        writableDatabase.execSQL("DELETE FROM map_polygons_database")
    }


}



