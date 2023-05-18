package com.example.mapdiary.adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mapdiary.firebase.FBAuth
import com.example.mapdiary.databinding.CommentListItemBinding
import com.example.mapdiary.dataclass.Comment
import com.example.mapdiary.dataclass.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class CommentAdapter(val context: Context, val commentList: MutableList<Comment>) : RecyclerView.Adapter<CommentAdapter.CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val binding = CommentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomViewHolder(binding)
    }

    override fun getItemCount(): Int = commentList.size

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val comment = commentList.get(position)
        val binding = holder.binding
        binding.tvComment.text = comment.commentTitle
        binding.tvDateTime.text = getCommentWriteDateTime(comment.commentDateTime)

        var commentUserUid = comment.commentUser
        var currentUserName = ""
        Firebase.database.reference.child("User").child("users").orderByChild("uid")
            .equalTo(commentUserUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        var user = data.getValue(User::class.java)
                        currentUserName = user!!.userNickname!!
                        binding.tvName.text = currentUserName
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        val pictureRef = Firebase.storage!!.reference.child(commentUserUid+".png")
        pictureRef.downloadUrl.addOnCompleteListener {
            if (it.isSuccessful) {
                Glide.with(context).load(it.result).into(binding.ivUserProfilePicture)
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCommentWriteDateTime(lastTimeString: String): String {      // 마지막 메세지가 전송된 시각 구하기
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
    inner class CustomViewHolder(val binding: CommentListItemBinding) : RecyclerView.ViewHolder(binding.root)
}