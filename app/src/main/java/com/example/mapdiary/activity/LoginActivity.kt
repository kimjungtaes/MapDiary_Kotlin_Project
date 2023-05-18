package com.example.mapdiary.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.mapdiary.R
import com.example.mapdiary.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityLoginBinding
    lateinit var auth: FirebaseAuth
    lateinit var spf: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()       // Firebase 계정 관련 변수
        spf = getSharedPreferences("loginKeep", Context.MODE_PRIVATE)
        if (spf.getBoolean("isLogin", false)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnLogin.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnLogin -> {
                signInWithEmailAndPassword()
            }
        }
    }

    private fun signInWithEmailAndPassword() {
        try {
            // 아이디 또는 패스워드가 입력되었는지 체크
            if (binding.edtId.text.toString().isNullOrBlank() && binding.edtPassword.text.toString()
                    .isNullOrBlank()
            ) {
                Toast.makeText(this, "아이디 또는 패스워드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(
                    binding.edtId.text.toString(),
                    binding.edtPassword.text.toString()
                )
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "성공", Toast.LENGTH_SHORT).show()
                            Log.e("MainActivity", "로그인 성공")
                            val spfEdit = spf.edit()
                            spfEdit.putBoolean("isLogin", true)
                            spfEdit.apply()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val id = binding.edtId.text.toString()
                            Firebase.database.reference.child("user").orderByChild("userEmail")
                                .equalTo(id)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.value != null) {
                                            Toast.makeText(
                                                this@LoginActivity,
                                                "패스워드를 확인해주세요.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                this@LoginActivity,
                                                "가입된 정보가 없습니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {
                                    }

                                })
                        }
                    }
            }
        } catch (e: java.lang.Exception) {
            Toast.makeText(this, "아이디 또는 패스워드를 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }
}