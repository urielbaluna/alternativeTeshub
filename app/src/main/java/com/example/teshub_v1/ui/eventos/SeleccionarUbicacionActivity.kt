package com.example.teshub_v1.ui.eventos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.teshub_v1.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SeleccionarUbicacionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccionar_ubicacion)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_picker) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val fabConfirm: FloatingActionButton = findViewById(R.id.fab_confirm_location)
        fabConfirm.setOnClickListener {
            selectedLatLng?.let {
                val resultIntent = Intent()
                resultIntent.putExtra("latitud", it.latitude)
                resultIntent.putExtra("longitud", it.longitude)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // --- Zoom aumentado para una vista más cercana ---
        val tesChalcoLocation = LatLng(19.233519, -98.841230)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tesChalcoLocation, 18.5f)) // Aumentado de 17f a 18.5f

        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Ubicación Seleccionada"))
            selectedLatLng = latLng
        }
    }
}
