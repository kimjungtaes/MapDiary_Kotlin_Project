package com.example.mapdiary.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mapdiary.activity.BoardDetailsActivity
import com.example.mapdiary.activity.ChatActivity
import com.example.mapdiary.R
import com.example.mapdiary.databinding.BoardListItemBinding
import com.example.mapdiary.dataclass.Board
import com.example.mapdiary.dataclass.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class BoardAdapter(val context: Context, val boardList: MutableList<Board>) : RecyclerView.Adapter<BoardAdapter.CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val binding =
            BoardListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomViewHolder(binding)
    }

    override fun getItemCount(): Int = boardList.size

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val binding = holder.binding
        val boardData = boardList.get(position)
        val currentUser = boardList[position]
        val writerUid = currentUser.boardWriter
        val uid = writerUid
        binding.tvTitle.text = boardData.boardTitle
        binding.tvContent.text = boardData.boardContent
        binding.tvLike.text = boardData.boardLike.toString()
        binding.tvComment.text = boardData.boardCommentCount.toString()
        binding.tvEye.text = boardData.boardHits.toString()
        binding.tvDateTime.text = getBoardWriteDateTime(boardData.boardDateTime)

        val database =
            Firebase.database.reference.child("board").child("${boardData.boardUid}")
        val currentUserUid = Firebase.auth.currentUser!!.uid
        database.child("likePeople").child(currentUserUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.value == true) {
                    binding.ivLike.setImageResource(R.drawable.ic_like)
                } else {
                    binding.ivLike.setImageResource(R.drawable.ic_nolike)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        Firebase.database.reference.child("User").child("users").orderByChild("uid").equalTo(writerUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var writerName = ""
                    for (data in snapshot.children) {
                        var user = data.getValue(User::class.java)
                        writerName = user!!.userNickname!!
                        binding.tvWriter.text = writerName
                    }
                }

                override fun onCancelled(error: DatabaseError) {}

            })

        binding.btnDm.setOnClickListener {
            var writerName = ""
            Firebase.database.reference.child("User").child("users").orderByChild("uid")
                .equalTo(writerUid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (data in snapshot.children) {
                            var user = data.getValue(User::class.java)
                            writerName = user!!.userNickname!!
                            val intent = Intent(context, ChatActivity::class.java)
                            intent.putExtra("name", writerName)
                            intent.putExtra("uId", writerUid)
                            context.startActivity(intent)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        binding.ivLike.setOnClickListener {
            val database =
                Firebase.database.reference.child("board").child("${boardData.boardUid}")
            val currentUserUid = Firebase.auth.currentUser!!.uid

            database.child("likePeople").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists() && snapshot.value == true) {
                            database.child("likePeople").child(currentUserUid).setValue(false)
                            binding.ivLike.setImageResource(R.drawable.ic_nolike)
                            database.child("boardLike").setValue(boardData.boardLike - 1)
                        } else {
                            database.child("likePeople").child(currentUserUid).setValue(true)
                            binding.ivLike.setImageResource(R.drawable.ic_like)
                            database.child("boardLike").setValue(boardData.boardLike + 1)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        val commentCountRef =
            Firebase.database.reference.child("board").child(boardData.boardUid).child("comment")
        commentCountRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                val commentCountMap = mutableMapOf<String, Any>()
                commentCountMap[boardData.boardUid] = count
                binding.tvComment.text = count.toString()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        val pictureRef = Firebase.storage!!.reference.child("images/${boardData.boardUid}.png")
        pictureRef.downloadUrl.addOnCompleteListener {
            if (it.isSuccessful) {
                Glide.with(context).load(it.result).into(binding.ivImage)
            }
        }

        val profileRef = Firebase.storage!!.reference.child(uid + ".png")
        profileRef.downloadUrl.addOnCompleteListener {
            if (it.isSuccessful) {
                Glide.with(context).load(it.result).into(binding.ivUser)
            }
        }

        binding.root.setOnClickListener {
            val tvHits = mutableMapOf<String, Any>()
            val weight = boardData.boardHits + 1
            tvHits["boardHits"] = weight
            Firebase.database.reference.child("board").child("${boardData.boardUid}")
                .updateChildren(tvHits)
            val intent = Intent(binding.root.context, BoardDetailsActivity::class.java)
            intent.putExtra("dataList", boardList as ArrayList<Serializable>)
            intent.putExtra("position", position)
            intent.putExtra("key", boardData.boardUid)
            intent.putExtra("aaa","${binding.tvWriter.text}")
            binding.root.context.startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getBoardWriteDateTime(lastTimeString: String): String {      // 마지막 메세지가 전송된 시각 구하기
        try {
            var currentTime = LocalDateTime.now().atZone(TimeZone.getDefault().toZoneId()) // 현재 시각
            var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            var messageMonth = lastTimeString.substring(4, 6).toInt() // 마지막 메세지 시각 월, 일, 시, 분
            var messageDate = lastTimeString.substring(6, 8).toInt()
            var messageHour = lastTimeString.substring(8, 10).toInt()
            var messageMinute = lastTimeString.substring(10, 12).toInt()
            var formattedCurrentTimeString = currentTime.format(dateTimeFormatter) // 현 시각 월,일,시,분
            var currentMonth = formattedCurrentTimeString.substring(4, 6).toInt()
            var currentDate = formattedCurrentTimeString.substring(6, 8).toInt()
            var currentHour = formattedCurrentTimeString.substring(8, 10).toInt()
            var currentMinute = formattedCurrentTimeString.substring(10, 12).toInt()
            var monthAgo = currentMonth - messageMonth      // 현 시각과 마지막 메세지 시각과의 차이. 월,일,시,분
            var dayAgo = currentDate - messageDate
            var hourAgo = currentHour - messageHour
            var minuteAgo = currentMinute - messageMinute
            if (monthAgo > 0) {     // 1개월 이상 차이 나는 경우
                return monthAgo.toString() + "개월 전"
            } else {
                if (dayAgo > 0) {     // 1일 이상 차이 나는 경우
                    if (dayAgo == 1) {
                        return "어제"
                    } else {
                        return dayAgo.toString() + "일 전"
                    }
                } else {
                    if (hourAgo > 0) {      // 1시간 이상 차이 나는 경우
                        return hourAgo.toString() + "시간 전"
                    } else {
                        if (minuteAgo > 0) {        // 1분 이상 차이 나는 경우
                            return minuteAgo.toString() + "분 전"
                        } else {
                            return "방금"
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return ""
        }
    }

    class CustomViewHolder(val binding: BoardListItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}