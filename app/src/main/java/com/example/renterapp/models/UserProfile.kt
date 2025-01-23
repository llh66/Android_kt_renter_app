package com.example.renterapp.models

import java.io.Serializable

data class UserProfile(val watchlist: MutableList<String> = mutableListOf()): Serializable