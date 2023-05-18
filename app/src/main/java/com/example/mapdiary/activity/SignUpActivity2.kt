package com.example.mapdiary.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.mapdiary.R
import com.example.mapdiary.databinding.ActivitySignUp2Binding
import com.example.mapdiary.dataclass.User
import com.example.mapdiary.firebase.FBAuth
import com.example.mapdiary.firebase.FBRef
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class SignUpActivity2 : AppCompatActivity() {
    lateinit var binding:ActivitySignUp2Binding
    var key: String? = null
    var userFlag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUp2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageView2.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }
        binding.edtRegisterNickName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (!(s.isNullOrBlank()) && s!!.matches("^[가-힣ㄱ-ㅎa-zA-Z0-9._-]{2,}\$".toRegex())) {
                    nickNameCheckFirebase(s.toString())
                } else {
                    binding.tvAno2.text = "한글/영어/숫자/밑줄을 사용할 수 있습니다."
                    binding.tvAno2.setTextColor(Color.BLACK)
                    binding.btnRegister.isEnabled = false
                }
            }
        })

        binding.btnRegister.setOnClickListener {
            val id = intent.getStringExtra("id")
            val pw = intent.getStringExtra("pw")
            val nickName = binding.edtRegisterNickName.text.toString()
            val imgSelected = binding.imageView2

            val key = FBRef.userRef.push().key.toString()
            this.key = key

            if (imgSelected.drawable is VectorDrawable) {
                Toast.makeText(this, "프로필 사진을 선택해주세요.", Toast.LENGTH_SHORT).show()
                binding.btnRegister.isEnabled = false
            } else {
                signUp(id!!, pw!!, nickName)
            }
        }

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }
    private fun userPictureUpload(uid: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val mountainsRef = storageRef.child("$uid.png")

        binding.imageView2.isDrawingCacheEnabled = true
        binding.imageView2.buildDrawingCache()
        val drawable = binding.imageView2.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            var uploadTask = mountainsRef.putBytes(data)
            uploadTask.addOnFailureListener {
            }.addOnSuccessListener { taskSnapshot ->
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1) {
            binding.imageView2.setImageURI(data?.data)
            binding.btnRegister.isEnabled = true
        }
    }

    private fun signUp(email: String, password: String, name: String) { // 회원 가입 실행
        FBAuth.auth.createUserWithEmailAndPassword(email, password)     // FirebaseAuth에 회원가입 성공 시
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {        // 회원가입 성공 시
                    try {
                        val uid = FBAuth.getUid()
                        val id = intent.getStringExtra("id")
                        val pw = intent.getStringExtra("pw")
                        val nickName = binding.edtRegisterNickName.text.toString()
                        FirebaseDatabase.getInstance().getReference("User").child("users")
                            .child(uid.toString()).setValue(
                                User(
                                    uid,
                                    id,
                                    pw,
                                    nickName
                                )
                            )       // Firebase RealtimeDatabase에 User 정보 추가
                        Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@SignUpActivity2, MainActivity::class.java)
                        startActivity(intent)
                        userPictureUpload(uid)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "화면 이동 중 문제 발생", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "회원가입에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun nickNameCheckFirebase(nickName: String) {
        Firebase.database.reference.child("User").child("users").orderByChild("userNickname").equalTo(nickName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value == null) { //닉네임이 없는 경우
                        userFlag = true
                        binding.tvAno2.visibility = View.VISIBLE
                        binding.tvAno2.text = "한글/영어/숫자/밑줄을 사용할 수 있습니다."
                        binding.tvAno2.setTextColor(Color.BLUE)
                        binding.btnRegister.isEnabled = true
                    } else if (snapshot.value != null) {
                        binding.tvAno2.visibility = View.VISIBLE
                        binding.tvAno2.text = "중복된 닉네임입니다."
                        binding.tvAno2.setTextColor(Color.RED)
                        userFlag = false
                        binding.btnRegister.isEnabled = false
                    } else {
                        binding.tvAno2.visibility = View.INVISIBLE
                        binding.tvAno2.text = "한글/영어/숫자/밑줄을 사용할 수 있습니다."
                        binding.tvAno2.setTextColor(Color.BLACK)
                        userFlag = false
                        binding.btnRegister.isEnabled = false
                    }
                }
            })
    }

}