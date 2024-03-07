package com.example.bakalar

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DatabaseNameManager private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        @Volatile
        private var instance: DatabaseNameManager? = null
        const val DATABASE_NAME = "map_polygons_name_database.db"
        const val DATABASE_VERSION = 1

        fun getInstance(context: Context): DatabaseNameManager {
            return instance ?: synchronized(this) {
                instance ?: DatabaseNameManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun polyExist(polyId: Int): Boolean {
        val query = "SELECT * FROM map_polygons_name_database WHERE polygon_id = $polyId"
        val cursor = readableDatabase.rawQuery(query, null)
        val exist = cursor.count > 0
        cursor.close()
        return exist
    }
    private fun removePoly(polyId: Int) {
        writableDatabase.execSQL("DELETE FROM map_polygons_name_database WHERE polygon_id= $polyId")
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE IF NOT EXISTS map_polygons_name_database (id INTEGER PRIMARY KEY, polygon_id INTEGER, name TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    fun insertPolygon(polyId: Int, name: String): Long {
        if (polyExist(polyId)) removePoly(polyId)
        val contentValues = ContentValues()
        contentValues.put("polygon_id", polyId)
        contentValues.put("name", name)
        return writableDatabase.insertOrThrow("map_polygons_name_database", null, contentValues)

    }


    fun selectAll(): MutableList<Pair<Int,String>> {
        val arr : MutableList<Pair<Int,String>> = mutableListOf()
        val query = "SELECT * FROM map_polygons_name_database"
        val cursor = readableDatabase.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val polygonId = cursor.getInt(cursor.getColumnIndexOrThrow("polygon_id"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val pair = Pair(polygonId,name)
            arr.add(pair)
        }

        cursor.close()
        return arr
    }

    fun deleteAll() {
        writableDatabase.execSQL("DELETE FROM map_polygons_name_database")
    }


}



