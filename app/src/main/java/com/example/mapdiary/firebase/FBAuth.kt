package com.example.mapdiary.firebase

import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class FBAuth {

    companion object {

        var auth: FirebaseAuth = FirebaseAuth.getInstance()

        fun getUid(): String {

            auth = FirebaseAuth.getInstance()

            return auth.currentUser?.uid.toString()
        }
        fun getDateTime2(): String {

            val currentDateTime = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(currentDateTime)

            return dateFormat
        }
    }
}