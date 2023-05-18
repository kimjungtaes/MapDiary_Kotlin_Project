package com.example.mapdiary.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.example.mapdiary.R
import com.example.mapdiary.databinding.ActivityBoardWrtieBinding
import com.example.mapdiary.dataclass.Board
import com.example.mapdiary.dataclass.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class BoardWrtieActivity : AppCompatActivity() {
    lateinit var binding: ActivityBoardWrtieBinding
    var imageUri: Uri? = null
    lateinit var dataList: MutableList<User>
    val currentUser = Firebase.auth.currentUser!!.uid
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardWrtieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataList = mutableListOf()

        Firebase.database.reference.child("User").child("users").orderByChild("uid")
            .equalTo("$currentUser")//상대방 사용자 키를 포함하는 채팅방 불러오기
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        val user = data.getValue(User::class.java)!!
                        dataList.add(user)
                    }
                }
            })
        val requestLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                imageUri = it.data?.data
                Glide.with(applicationContext).load(imageUri).into(binding.ivAddPicture)
            }
        }
        // 이벤트 접근
        binding.ivAddPicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            requestLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            binding.btnSave.isEnabled = false
            if (binding.edtTitle.text.isNotEmpty() && binding.edtContent.text.isNotEmpty() && imageUri != null) {
                val docID = FirebaseDatabase.getInstance().getReference("board")?.push()?.key
                val title = binding.edtTitle.text.toString()
                val content = binding.edtContent.text.toString().trim()
                val date = getDateTimeString()
                val board =
                    Board(docID!!, currentUser, title, content, 0, 0, 0, date)

                // firebase realtimeDatabase의 picture 테이블에 클래스 저장
                FirebaseDatabase.getInstance().getReference("board")?.child(docID!!)
                    ?.setValue(board)
                    ?.addOnSuccessListener {
                        Log.e("PictureAddActivity", R.string.PAA_string1.toString())
                        val pictureRef =
                            Firebase.storage?.reference?.child("images/${docID}.png")
                        pictureRef!!.putFile(imageUri!!).addOnSuccessListener {
                            Toast.makeText(
                                applicationContext,
                                R.string.PAA_string2,
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("PictureAddActivity", "이미지 업로드 성공")
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(
                                applicationContext,
                                R.string.PAA_string3,
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("PictureAddActivity", "이미지 업로드 실패")
                            binding.btnSave.isEnabled = true
                        }
                    }?.addOnFailureListener {
                        Log.e("PictureAddActivity", "이미지 정보 업로드 실패")
                        binding.btnSave.isEnabled = true
                    }
            } else {
                Toast.makeText(applicationContext, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDateTimeString(): String {       // 메세지 보낸 시각 정보 반환
        try {
            var localDateTime = LocalDateTime.now()
            localDateTime.atZone(TimeZone.getDefault().toZoneId())
            var  dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            return localDateTime.format(dateTimeFormatter).toString()
        } catch (e:Exception) {
            e.printStackTrace()
            throw java.lang.Exception("getTimeError")
        }
    }
}
