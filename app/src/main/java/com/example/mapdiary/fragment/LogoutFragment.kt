package com.example.mapdiary.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.mapdiary.activity.MainActivity
import com.example.mapdiary.activity.MainLogInActivity
import com.example.mapdiary.databinding.FragmentThreeBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LogoutFragment : Fragment() {
    lateinit var mainActivity: MainActivity
    lateinit var binding: FragmentThreeBinding
    lateinit var spf: SharedPreferences

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity // 형변환
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentThreeBinding.inflate(inflater)

        binding.btnLogout.setOnClickListener {
            val builder = AlertDialog.Builder(mainActivity)
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("확인") { dialog, id ->
                    spf = mainActivity.getSharedPreferences("loginKeep", Context.MODE_PRIVATE)
                    val spfEdit = spf.edit()
                    spfEdit.putBoolean("isLogin", false)
                    spfEdit.apply()
                    try {
                        Firebase.auth.signOut()
                        startActivity(Intent(mainActivity, MainLogInActivity::class.java))
                        dialog.dismiss()
                        mainActivity.finish()

                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        dialog.dismiss()
                        Toast.makeText(mainActivity, "로그아웃 중 오류가 발생하였습니다.", Toast.LENGTH_LONG)
                            .show()
                    }
                }.setNegativeButton("취소") {         // 다이얼로그 닫기
                        dialog, id ->
                    dialog.dismiss()
                }
            builder.show()
        }
        return binding.root
    }
}