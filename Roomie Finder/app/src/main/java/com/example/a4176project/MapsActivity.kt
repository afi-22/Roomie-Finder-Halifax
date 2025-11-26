package com.example.a4176project

import android.app.Activity

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.a4176project.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private lateinit var binding: ActivityMapsBinding
    private lateinit var geocoder: Geocoder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geocoder = Geocoder(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Back"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.confirmButton.setOnClickListener {
            selectedLocation?.let { latLng ->
                val resultIntent = Intent().apply {
                    putExtra("latitude", latLng.latitude)
                    putExtra("longitude", latLng.longitude)
                    putExtra("address", getAddressFromLocation(latLng.latitude, latLng.longitude))
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        binding.searchButton.setOnClickListener {
            val address = binding.addressEditText.text.toString()
            searchLocation(address)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val defaultLocation = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            selectedLocation = latLng
        }
    }

    private fun searchLocation(address: String) {
        try {
            val addressList: List<Address?>? = geocoder.getFromLocationName(address, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val location = addressList[0]
                val latLng = location?.let { LatLng(it.latitude, it.longitude) }
                if (latLng != null) {
                    mMap.clear()
                    mMap.addMarker(MarkerOptions().position(latLng).title(address))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    selectedLocation = latLng
                } else {
                    Toast.makeText(this, "Invalid address", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to geocode address", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        try {
            val addressList = geocoder.getFromLocation(latitude, longitude, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                return address.getAddressLine(0) ?: ""
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }
}
