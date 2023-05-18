package com.example.mapdiary.firebase

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FBRef {

    companion object {
        private val database = Firebase.database
        val userRef = database.getReference("User")
        val boardRef = database.getReference("board")
    }
}