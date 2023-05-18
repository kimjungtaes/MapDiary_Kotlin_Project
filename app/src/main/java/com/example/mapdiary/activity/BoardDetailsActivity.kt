package com.example.mapdiary.activity

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.mapdiary.R
import com.example.mapdiary.adapter.CommentAdapter
import com.example.mapdiary.databinding.ActivityBoardDetailsBinding
import com.example.mapdiary.dataclass.Board
import com.example.mapdiary.dataclass.Comment
import com.example.mapdiary.firebase.FBAuth
import com.example.mapdiary.firebase.FBRef
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class BoardDetailsActivity : AppCompatActivity() {
    lateinit var binding: ActivityBoardDetailsBinding
    lateinit var key: String
    lateinit var writerName: String
    lateinit var commentList: MutableList<Comment>
    lateinit var commentAdapter: CommentAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        commentList = mutableListOf()
        commentAdapter = CommentAdapter(this, commentList)
        binding.lvcomment.layoutManager = LinearLayoutManager(this)
        binding.lvcomment.adapter = commentAdapter

        key = intent.getStringExtra("key").toString()
        writerName = intent.getStringExtra("aaa").toString()

        binding.tvName.text = writerName
        getImageData(key)
        getBoard(key)

        binding.ivDelete.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.BDA_string1)
                .setMessage(R.string.BDA_string2)
                .setPositiveButton(R.string.check_string) { dialog, id ->

                    try {
                        Firebase.database.getReference("board").child(key).removeValue()
                        Toast.makeText(this, R.string.BDA_string3, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        finish()

                    } catch (e: java.lang.Exception) {
                        dialog.dismiss()
                    }
                }.setNegativeButton("취소") {
                        dialog, id ->
                    dialog.dismiss()
                }
            builder.show()
        }

        binding.btnInput.setOnClickListener {
            if (!binding.edtComment.text.isNullOrEmpty()) {
                inputComment(key)
            } else {
                Toast.makeText(this@BoardDetailsActivity, R.string.BDA_string4, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        getCommentData(key)


    }

    fun getCommentData(key: String) {
        val postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentList.clear()
                for (data in snapshot.children) {
                    val item = data.getValue(Comment::class.java)
                    commentList.add(item!!)
                }
                commentAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        }

        FBRef.boardRef.child(key).child("comment").addValueEventListener(postListener)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun inputComment(key: String) {
        val currentUserUid = FBAuth.auth.uid
        FBRef.boardRef.child(key).child("comment").push().setValue(
            Comment(binding.edtComment.text.toString(), getDateTimeString(), currentUserUid.toString())
        )

        Toast.makeText(this, R.string.BDA_string5, Toast.LENGTH_SHORT).show()
        binding.edtComment.text.clear()
    }

    fun getImageData(key: String) {
        val storageReference = Firebase.storage.reference.child("images/$key.png")
        val imageView = binding.ivImage

        storageReference.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Glide.with(this)
                    .load(task.result)
                    .into(imageView)
            } else {
                binding.ivImage.isVisible = false
            }
        }
    }

    fun getBoard(key: String) {
        val postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(Board::class.java)

                binding.tvTitle.text = data?.boardTitle
                binding.tvContent.text = data?.boardContent
                val userUid = FBAuth.getUid()
                val writerUid = data?.boardWriter

                if (userUid.equals(writerUid)) {
                    binding.ivDelete.visibility = View.VISIBLE
                } else {
                    binding.ivDelete.visibility = View.INVISIBLE
                }
                val pictureRef = Firebase.storage!!.reference.child("images/${key}.png")
                pictureRef.downloadUrl.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Glide.with(applicationContext).load(it.result)
                            .into(binding.ivUserProfilePicture)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        FBRef.boardRef.child(key).addValueEventListener(postListener)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDateTimeString(): String {
        try {
            var localDateTime = LocalDateTime.now()
            localDateTime.atZone(TimeZone.getDefault().toZoneId())
            var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            return localDateTime.format(dateTimeFormatter).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw java.lang.Exception("getTimeError")
        }
    }
}