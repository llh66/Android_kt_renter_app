package com.example.renterapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.renterapp.adapters.PropertyAdapter
import com.example.renterapp.databinding.ActivityWatchlistBinding
import com.example.renterapp.interfaces.ClickDetectorInterface
import com.example.renterapp.models.Property
import com.example.renterapp.models.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

class WatchlistActivity : AppCompatActivity(), ClickDetectorInterface {

    private lateinit var binding: ActivityWatchlistBinding

    private val propertiesToShow: MutableList<Property> = mutableListOf()

    lateinit var propertyAdapter: PropertyAdapter

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchlistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.title = "Watchlist"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        propertyAdapter = PropertyAdapter(propertiesToShow, this)
        binding.rvProperties.apply {
            adapter = propertyAdapter
            layoutManager = LinearLayoutManager(this@WatchlistActivity)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            )
        }
    }

    private fun updateList() {
        if (auth.currentUser != null) {
            propertiesToShow.clear()
            db.collection("userProfiles")
                .document(auth.currentUser!!.uid)
                .get()
                .addOnSuccessListener { documentSnapshot: DocumentSnapshot ->
                    val userProfile: UserProfile? = documentSnapshot.toObject(UserProfile::class.java)
                    if (userProfile != null) {
                        for (id in userProfile.watchlist) {
                            db.collection("properties")
                                .document(id)
                                .get()
                                .addOnSuccessListener { propertiesDocumentSnapshot: DocumentSnapshot ->
                                    val property: Property? = propertiesDocumentSnapshot.toObject(Property::class.java)
                                    if (property != null) {
                                        propertiesToShow.add(property)
                                        propertyAdapter.notifyDataSetChanged()
                                    }
                                }
                                .addOnFailureListener {
                                    Log.w("DB", "Error retrieving property")
                                }
                        }
                        if (userProfile.watchlist.size == 0) {
                            propertyAdapter.notifyDataSetChanged()
                        }
                    }
                }
                .addOnFailureListener {
                    Log.w("DB", "Error retrieving user profile")
                }
        }
        else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun removeRow(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Are you sure to remove the property from your watchlist?")
            .setPositiveButton("Yes") { _, _ ->
                if (auth.currentUser != null) {
                    db.collection("userProfiles")
                        .document(auth.currentUser!!.uid)
                        .update("watchlist", FieldValue.arrayRemove(propertiesToShow[position].documentId))
                        .addOnSuccessListener {
                            updateList()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                baseContext,
                                "Error removing property.",
                                Toast.LENGTH_SHORT,
                            ).show()
                            Log.w("DB", "Error removing property")
                        }
                }
                else {
                    finish()
                }
            }
            .setNegativeButton("No", null)
            .create()
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateList()
    }
}