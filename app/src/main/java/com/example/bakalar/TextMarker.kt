package com.example.bakalar

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class TextMarker(private val mapView: MapView, text: String, center: GeoPoint) : Marker(mapView) {
    private var markerText: String = text
    private val markerTextSize: Float = 44f

    init {
        position = center
        setAnchor(ANCHOR_CENTER, ANCHOR_CENTER)
        setInfoWindow(null)
        setPanToView(true)
        setVisible(false)
        title = markerText
        snippet = ""
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (canvas != null && !shadow) {
            val zoomLevel = mapView?.zoomLevelDouble
            if (zoomLevel != null) {
                if (zoomLevel >= 15) {
                    drawText(canvas)
                    super.draw(canvas, mapView, false)
                }
            }
        }
    }

    private fun drawText(canvas: Canvas) {
        val paint = Paint()
        paint.color = Color.WHITE
        paint.textSize = markerTextSize
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD

        val mapViewPoint = mapView.projection.toPixels(position, null)
        val x = mapViewPoint.x.toFloat()
        val y = mapViewPoint.y.toFloat()

        canvas.drawText(markerText, x, y, paint)
    }
}





