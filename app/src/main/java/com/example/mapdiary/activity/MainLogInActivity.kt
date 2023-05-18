package com.example.mapdiary.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.mapdiary.R
import com.example.mapdiary.databinding.ActivityMainLogInBinding

class MainLogInActivity : AppCompatActivity() {
    lateinit var binding:ActivityMainLogInBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startLoading()
        binding.btnLogIn.setOnClickListener {
            val intent = Intent(this@MainLogInActivity, LoginActivity::class.java)
            startActivity(intent)
        }
        binding.btnSignUp.setOnClickListener {
            val intent = Intent(this@MainLogInActivity, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
    fun startLoading() {
        Glide.with(this).load(R.raw.loading2).into(binding.imageView)
    }
}