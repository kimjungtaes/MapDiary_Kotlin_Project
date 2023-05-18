package com.example.mapdiary.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mapdiary.activity.ChatActivity
import com.example.mapdiary.R
import com.example.mapdiary.databinding.UserLayoutBinding
import com.example.mapdiary.dataclass.User
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class UserAdapter(private val context: Context, private val userList:ArrayList<User>): RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return UserViewHolder(binding)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        val binding = holder.binding
        binding.tvName.text = currentUser.userNickname
        binding.root.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            // 넘길 데이터
            intent.putExtra("name", currentUser.userNickname)
            intent.putExtra("uId", currentUser.uid)
            context.startActivity(intent)
        }
        var userUid = currentUser.uid
        val storageReference = Firebase.storage.reference.child(userUid+".png")
        storageReference.downloadUrl.addOnCompleteListener{task ->
            if (task.isSuccessful) {
                Glide.with(context)
                    .load(task.result)
                    .error(R.drawable.img)
                    .into(binding.ivUserProfilePicture)
            }
        }
    }

    class UserViewHolder(val binding:UserLayoutBinding): RecyclerView.ViewHolder(binding.root){
    }
}
