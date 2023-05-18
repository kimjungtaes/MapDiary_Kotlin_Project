package com.example.mapdiary.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapdiary.R
import com.example.mapdiary.adapter.MessageAdapter
import com.example.mapdiary.databinding.ActivityChatBinding
import com.example.mapdiary.dataclass.Message
import com.example.mapdiary.firebase.FBAuth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class ChatActivity : AppCompatActivity() {
    lateinit var binding: ActivityChatBinding
    private lateinit var receiverName: String
    private lateinit var receiverUid: String
    lateinit var mAuth: FirebaseAuth
    lateinit var mDbRef: DatabaseReference
    private lateinit var receiverRoom: String // 받는 대화방
    private lateinit var senderRoom: String // 보낸 대화방
    private lateinit var messageList: ArrayList<Message>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messageList = ArrayList()
        val messageAdapter: MessageAdapter = MessageAdapter(this, messageList)

        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = messageAdapter

        // 넘어온 데이터 변수에 담기
        receiverName = intent.getStringExtra("name").toString()
        receiverUid = intent.getStringExtra("uId").toString()
        Log.e("dsdfasdf", receiverName)
        Log.e("dsdfasdf", receiverUid)

        mAuth = Firebase.auth
        mDbRef = FirebaseDatabase.getInstance().reference

        // 접속자 Uid
        val senderUid = mAuth.currentUser?.uid

        // 보낸이방
        senderRoom = receiverUid + senderUid

        // 받는이방
        receiverRoom = senderUid + receiverUid

        binding.tvChatName.text = receiverName

        binding.btnSend.setOnClickListener {
            if (!binding.edtMessage.text.isNullOrEmpty()) {
                val message = binding.edtMessage.text.toString()
                val dateTime = FBAuth.getDateTime2()
                val messageObject = Message(message, senderUid, dateTime)

                // 데이터 저장
                mDbRef.child("chats").child(senderRoom).child("messages").push()
                    .setValue(messageObject).addOnCompleteListener {
                        // 저장 성공하면
                        mDbRef.child("chats").child(receiverRoom).child("messages").push()
                            .setValue(messageObject)
                    }
                // 입력값 초기화
                binding.edtMessage.setText("")
                binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
            } else {
                Toast.makeText(this@ChatActivity, R.string.CAA_string1, Toast.LENGTH_SHORT).show()
            }
        }

        // 메시지 가져오기
        mDbRef.child("chats").child(senderRoom).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()

                    for (data in snapshot.children) {
                        val message = data.getValue(Message::class.java)
                        messageList.add(message!!)
                    }
                    // 적용
                    messageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // 오류 발생 시 실행
                }
            })
    }
}