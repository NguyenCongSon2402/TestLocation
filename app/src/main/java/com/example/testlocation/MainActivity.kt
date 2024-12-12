package com.example.testlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.testlocation.databinding.ActivityMainBinding
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var currentJob: Job? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Quyền đã được cấp, gọi hàm lấy vị trí
                fetchCurrentLocation()
            } else {
                // Quyền bị từ chối
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestLocationPermission()
        binding.btnGetLocation.setOnClickListener {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchCurrentLocation()
        }
    }

    private fun fetchCurrentLocation() {
        currentJob?.cancel()  // Cancel previous job
        currentJob = CoroutineScope(Dispatchers.IO).launch {
            fetchLocationFlow().collectLatest { location ->
                binding.txtLocation.text = location?.let {
                    "Lat: ${it.latitude}, Lng: ${it.longitude}"
                } ?: "Location not found"
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationFlow(): Flow<Location?> = flow {
        try {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
            val currentLocationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            binding.startTime.text = "timeStart: ${getCurrentTimeFormatted()}"
            val location = withTimeout(10_000L) {
                fusedLocationProviderClient.getCurrentLocation(currentLocationRequest, null).await()
            }

            if (location != null) {
                emit(location)
                binding.endTime.text = "endTime: ${getCurrentTimeFormatted()}"
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }

    private fun getCurrentTimeFormatted(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
