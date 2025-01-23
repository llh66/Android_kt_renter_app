package com.example.renterapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.renterapp.databinding.ActivityMainBinding
import com.example.renterapp.models.Property
import com.example.renterapp.models.UserProfile
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private var mMap: GoogleMap? = null

    private var properties: HashMap<String, Property> = HashMap<String, Property>()

    private var maxPrice: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        auth.addAuthStateListener {
            invalidateOptionsMenu()
            setMarkers()
        }

        binding.btnClear.setOnClickListener {
            binding.etMaxPrice.setText("")
        }

        binding.btnApply.setOnClickListener {
            maxPrice = binding.etMaxPrice.text.toString().toDoubleOrNull()
            setMarkers()
        }
    }

    private fun setMarkers(){
        binding.etMaxPrice.setText(if (maxPrice == null) "" else maxPrice.toString())

        db.collection("properties")
            .whereEqualTo("available", true)
            .whereLessThanOrEqualTo("price", maxPrice ?: Double.POSITIVE_INFINITY)
            .get()
            .addOnSuccessListener { querySnapshot: QuerySnapshot ->
                properties.clear()
                mMap!!.clear()

                if (auth.currentUser != null) {
                    db.collection("userProfiles")
                        .document(auth.currentUser!!.uid)
                        .get()
                        .addOnSuccessListener { documentSnapshot: DocumentSnapshot ->
                            val userProfile: UserProfile? = documentSnapshot.toObject(UserProfile::class.java)
                            if (userProfile != null) {
                                Log.d("userProfile", userProfile.watchlist.toString())
                            }

                            for (document in querySnapshot) {
                                val property: Property = document.toObject(Property::class.java)

                                val marker = mMap!!.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(property.latitude, property.longitude))
                                        .title(document.id)
                                )
                                if (userProfile != null && userProfile.watchlist.contains(document.id)) {
                                    property.isInWatchlist = true
                                }
                                properties[marker!!.id] = property
                            }
                        }
                }
                else {
                    for (document in querySnapshot) {
                        val property: Property = document.toObject(Property::class.java)

                        val marker = mMap!!.addMarker(
                            MarkerOptions()
                                .position(LatLng(property.latitude, property.longitude))
                                .title(document.id)
                        )
                        properties[marker!!.id] = property
                    }
                }
            }
    }

    private fun startWatchlistActivity() {
        if (auth.currentUser == null) {
            AlertDialog.Builder(this)
                .setTitle("Login required")
                .setMessage("Please login to check your watchlist")
                .setPositiveButton("Login") { _, _ ->
                    startLoginActivity(true)
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
        }
        else {
            val intent = Intent(this, WatchlistActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startLoginActivity(isOpenWatchList: Boolean) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("isOpenWatchList", isOpenWatchList)
        startActivity(intent)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap!!.uiSettings.isZoomControlsEnabled = true

        val downtownToronto = LatLng(43.651070, -79.347015)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(downtownToronto, 10f))
        mMap!!.uiSettings.isZoomGesturesEnabled = true

        if (properties.size == 0) {
            setMarkers()
        }

        mMap!!.setOnMarkerClickListener { marker ->
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 10f))
            binding.apply {
                tvPrice.text = "$${properties[marker.id]?.price.toString()} / month"
                tvAddress.text = properties[marker.id]?.address.toString()
                tvBedrooms.text = "${properties[marker.id]?.bedrooms} bedroom" +
                        "${if (properties[marker.id] != null && properties[marker.id]!!.bedrooms > 1) "s" else "" }"
            }

            Glide
                .with(this)
                .load(properties[marker.id]?.imageUrl)
                .placeholder(R.drawable.baseline_insert_photo_24)
                .into(binding.imgPropertyImage)

            if (properties[marker.id] != null && properties[marker.id]!!.isInWatchlist) {
                binding.btnAddToWatchlist.isEnabled = false
                binding.btnAddToWatchlist.text = "In Watchlist"
            }
            else {
                    binding.btnAddToWatchlist.setOnClickListener{
                        if (auth.currentUser != null && properties[marker.id] != null) {
                            db.collection("userProfiles")
                                .document(auth.currentUser!!.uid)
                                .update("watchlist", FieldValue.arrayUnion(properties[marker.id]!!.documentId))
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        baseContext,
                                        "Success! It has been added to your watchlist.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    setMarkers()
                                    binding.btnAddToWatchlist.isEnabled = false
                                    binding.btnAddToWatchlist.text = "In Watchlist"
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(
                                        baseContext,
                                        "Adding to watchlist failed.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    Log.w("DB", exception)
                                }
                        }
                        else if (auth.currentUser == null) {
                            AlertDialog.Builder(this)
                                .setTitle("Login required")
                                .setMessage("Please login to access your watchlist")
                                .setPositiveButton("Login") { _, _ ->
                                    startLoginActivity(false)
                                }
                                .setNegativeButton("Cancel", null)
                                .create()
                                .show()
                        }
                        else {
                            Toast.makeText(
                                baseContext,
                                "Error getting property info.",
                                Toast.LENGTH_SHORT,
                            ).show()
                            Log.w("DB", "Error getting property info")
                        }
                    }
                    binding.btnAddToWatchlist.text = "Add to Watchlist"
                    binding.btnAddToWatchlist.isEnabled = true
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (mMap != null) {
            setMarkers()
        }
        binding.apply {
            imgPropertyImage.setImageResource(R.drawable.baseline_insert_photo_24)
            tvPrice.text = ""
            tvAddress.text = "Choose a marker\n"
            tvBedrooms.text = ""
            btnAddToWatchlist.isEnabled = false
            btnAddToWatchlist.text = "Add to Watchlist"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.actions_menu, menu)

        if (menu != null) {
            val miLogin = menu.findItem(R.id.miLogin)
            val miLogout = menu.findItem(R.id.miLogout)

            if (auth.currentUser == null) {
                miLogin.setVisible(true)
                miLogout.setVisible(false)
            }
            else {
                miLogin.setVisible(false)
                miLogout.setVisible(true)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.miRefresh -> {
                setMarkers()
                return true
            }
            R.id.miLogin -> {
                startLoginActivity(false)
                return true
            }
            R.id.miLogout -> {
                auth.signOut()
                return true
            }
            R.id.miMyWatchList -> {
                startWatchlistActivity()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}