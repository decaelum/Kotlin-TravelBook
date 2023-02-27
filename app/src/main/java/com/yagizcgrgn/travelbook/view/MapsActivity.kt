package com.yagizcgrgn.travelbook.view

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.yagizcgrgn.travelbook.R
import com.yagizcgrgn.travelbook.databinding.ActivityMapsBinding
import com.yagizcgrgn.travelbook.model.Place
import com.yagizcgrgn.travelbook.roomdb.PlaceDao
import com.yagizcgrgn.travelbook.roomdb.PlaceDb
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean : Boolean? = null
    private var selectedLatitude : Double? = null
    private var selectedLongitude : Double? = null

    private lateinit var db : PlaceDb
    private lateinit var placeDao : PlaceDao

    private val compositeDisposable = CompositeDisposable()

    private var placeFromMain : Place? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        selectedLatitude = 0.0
        selectedLongitude = 0.0

        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.yagizcgrgn.travelbook", MODE_PRIVATE)
        trackBoolean = false

        db = Room.databaseBuilder(applicationContext,PlaceDb::class.java,"Places").build()
        placeDao = db.placeDao()

        binding.btSave.isEnabled = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")

        if (info == "new"){

            binding.btSave.visibility = View.VISIBLE
            binding.btDelete.visibility = View.GONE

            //casting
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trackBoolean = sharedPreferences.getBoolean("trackBoolean",false)
                    if(!trackBoolean!!){
                        val userLocation = LatLng(location.latitude,location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15f))
                        sharedPreferences.edit().putBoolean("trackBoolean",true).apply()
                    }
                }

            }
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION )){
                    Snackbar.make(binding.root, "Permission need for location" , Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                        //request permission
                        permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                }else {
                    permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                //permission granted
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,10f, locationListener )
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(lastLocation != null){
                    val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                }
                mMap.isMyLocationEnabled = true

            }

        } else {

            mMap.clear()

            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place
            placeFromMain.let {place ->

                val latLng = place?.let { LatLng(place.latitude, it.longitude) }

                if (place != null) {
                    mMap.addMarker(MarkerOptions().position(latLng!!).title(place.name))
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng!!,15f))

                binding.edTPlaceName.setText(place.name)
                binding.btSave.visibility = View.GONE
                binding.btDelete.visibility = View.VISIBLE
            }

        }




    }

    private fun registerLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            if(equals(true)){
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    //permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,10f, locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation != null){
                        val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                    }
                    mMap.isMyLocationEnabled = true
                }
            }else {
                //permission denied
                Toast.makeText(this@MapsActivity,"You need give a permission", Toast.LENGTH_LONG)
            }
        }
    }

    override fun onMapLongClick(p0: LatLng) {

        mMap.clear()

        mMap.addMarker(MarkerOptions().position(p0))

        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude

        binding.btSave.isEnabled = true
    }

    fun save(view: View?){
        if(selectedLatitude != null && selectedLongitude != null ){
            val place = Place(binding.edTPlaceName.text.toString(),selectedLatitude!!,selectedLongitude!!)
            compositeDisposable.add(
                placeDao.insertPlace(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }
    }

    private fun handleResponse(){
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun delete(view: View?){
        placeFromMain?.let {
            compositeDisposable.add(
                placeDao.deletePlace(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        compositeDisposable.clear()
    }
}