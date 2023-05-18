package com.example.mapdiary.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mapdiary.R
import com.example.mapdiary.dataclass.Message
import com.example.mapdiary.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class MessageAdapter(private val context: Context, private val messageList: ArrayList<Message>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val receive = 1 // 받는 타입
    private val send = 2 // 보내는 타입

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == 1){ // 받는 화면
            val view: View = LayoutInflater.from(context).inflate(R.layout.receive,parent,false)
            ReceiveViewHolder(view)
        }else{ // 보내는 화면
            val view: View = LayoutInflater.from(context).inflate(R.layout.send,parent,false)
            SendViewHolder(view)
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // 현재 메시지
        val currentMessage = messageList[position]
        currentMessage.sendedDate = currentMessage.sendedDate
        // 보내는 데이터
        if(holder.javaClass == SendViewHolder::class.java){
            val viewHolder = holder as SendViewHolder
            viewHolder.sendMessage.text = currentMessage.message
            viewHolder.sendMessageDate.text = currentMessage.sendedDate
        }else{ // 받는 데이터
            val viewHolder = holder as ReceiveViewHolder
            viewHolder.receiveMessage.text = currentMessage.message
            viewHolder.receiveMessageDate.text = currentMessage.sendedDate
            val uid = currentMessage.sendId
            var receiverImage = viewHolder.senderImage
            val storageReference = Firebase.storage.reference.child(uid+".png")
            storageReference.downloadUrl.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Glide.with(context)
                        .load(task.result)
                        .error(R.drawable.img)
                        .into(receiverImage)
                }
            }
            Firebase.database.reference.child("User").child("users").orderByChild("uid").equalTo(uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var senderNickname = ""
                        for (data in snapshot.children) {
                            var user = data.getValue(User::class.java)
                            senderNickname = user!!.userNickname!!
                            viewHolder.senderNickName.text = senderNickname
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
        }


    }

    override fun getItemViewType(position: Int): Int {
        // 메시지 값
        val currentMessage = messageList[position]
        return if(FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.sendId)){
            send
        }else{
            receive
        }
    }

    // 보낸 쪽
    class SendViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val sendMessage: TextView = itemView.findViewById(R.id.tvSendMessage)
        val sendMessageDate: TextView = itemView.findViewById(R.id.tvSendMessageDate)
    }

    // 받는 쪽
    inner class ReceiveViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val receiveMessage: TextView = itemView.findViewById(R.id.tvReceiveMessage)
        val receiveMessageDate: TextView = itemView.findViewById(R.id.tvReceiveMessageDate)
        val senderImage: ImageView = itemView.findViewById(R.id.ivUserChat)
        val senderNickName:TextView = itemView.findViewById(R.id.tvChatNickName)

    }
}