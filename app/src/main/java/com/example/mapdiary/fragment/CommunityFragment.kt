package com.example.mapdiary.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapdiary.activity.MainActivity
import com.example.mapdiary.activity.BoardWrtieActivity
import com.example.mapdiary.firebase.FBRef
import com.example.mapdiary.adapter.BoardAdapter
import com.example.mapdiary.databinding.FragmentFourBinding
import com.example.mapdiary.dataclass.Board
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class CommunityFragment : Fragment() {
    lateinit var binding: FragmentFourBinding
    lateinit var mainActivity: MainActivity
    lateinit var boardAdapter: BoardAdapter
    lateinit var boardList: MutableList<Board>
    lateinit var boardKeyList: MutableList<String>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentFourBinding.inflate(layoutInflater)

        boardList = mutableListOf()
        boardKeyList = mutableListOf()
        boardAdapter = BoardAdapter(mainActivity, boardList)
        binding.recyclerView.layoutManager = LinearLayoutManager(mainActivity)
        binding.recyclerView.adapter = boardAdapter

        binding.btnWrite.setOnClickListener {
            val intent = Intent(mainActivity, BoardWrtieActivity::class.java)
            startActivity(intent)
        }
        getFireBaseBoardList()
        return binding.root
    }

    private fun getFireBaseBoardList() {
        val postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                boardList.clear()
                boardKeyList.clear()
                for (data in snapshot.children) {
                    Log.e("BoardListActivity", data.toString())
                    //data.key
                    val item = data.getValue(Board::class.java) ?: return
                    boardList.add(item)
                    boardKeyList.add(data.key.toString())
                }
                boardKeyList.reverse()
                boardList.reverse()

                boardAdapter.notifyDataSetChanged()
                Log.e("BoardListActivity", boardList.toString())
                Log.e("BoardListActivity-key", boardKeyList.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BoardListActivity", error.toException().toString())
            }
        }
        FBRef.boardRef.addValueEventListener(postListener)
    }
}