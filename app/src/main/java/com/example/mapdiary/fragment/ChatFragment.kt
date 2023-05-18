package com.example.mapdiary.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapdiary.activity.MainActivity
import com.example.mapdiary.R
import com.example.mapdiary.adapter.UserAdapter
import com.example.mapdiary.databinding.FragmentOneBinding
import com.example.mapdiary.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ChatFragment : Fragment() {
    lateinit var binding: FragmentOneBinding
    lateinit var adapter: UserAdapter
    lateinit var mainActivity: MainActivity
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var userList: ArrayList<User>
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentOneBinding.inflate(layoutInflater)
        mAuth = Firebase.auth
        mDbRef = Firebase.database.reference
        userList = ArrayList()
        adapter = UserAdapter(mainActivity, userList)

        binding.userRecyclerView.layoutManager = LinearLayoutManager(mainActivity)
        binding.userRecyclerView.adapter = adapter

        mDbRef.child("User").child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (data in snapshot.children) {
                    // 유저 정보
                    val currentUser = data.getValue(User::class.java)
                    if (mAuth.currentUser?.uid != currentUser?.uid) {
                        userList.add(currentUser!!)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                // 실패 시 실행
            }
        })

        return binding.root
    }
}