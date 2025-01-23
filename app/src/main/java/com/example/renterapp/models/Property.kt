package com.example.renterapp.models

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Property(
    @DocumentId
    val documentId: String = "",
    val address: String = "",
    val available: Boolean = false,
    val bedrooms: Int = 0,
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val price: Double = 0.0,
    val userId: String = "",
    var isInWatchlist: Boolean = false
): Serializable