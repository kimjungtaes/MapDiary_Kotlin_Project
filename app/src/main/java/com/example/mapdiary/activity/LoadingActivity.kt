package com.example.mapdiary.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.mapdiary.R
import com.example.mapdiary.databinding.ActivityLoadingBinding

class LoadingActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startLoading()
    }

    private fun startLoading() {
        Handler().postDelayed({
            val intent = if (isLoggedIn()) {
                Intent(this@LoadingActivity, MainActivity::class.java)
            } else {
                Intent(this@LoadingActivity, MainLogInActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 3540)
        Glide.with(this).load(R.raw.loading).into(binding.ivPicture)
    }

    private fun isLoggedIn(): Boolean {
        val spf = getSharedPreferences("loginKeep", Context.MODE_PRIVATE)
        return spf.getBoolean("isLogin", false)
    }
}