package com.example.mapdiary.activity

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.example.mapdiary.databinding.ActivitySignUpBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {
    lateinit var binding:ActivitySignUpBinding
    var id: String? = null
    var pw: String? = null
    var emailFlag = false
    var passwordFlag = false
    var flag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.edtRegisterId.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.llPassword.visibility = View.INVISIBLE
                checkRegisterEmail(s.toString())
                idCheckFirebase(s.toString())
                if (emailFlag == true && flag == true) {
                    binding.tvAno.visibility = View.VISIBLE
                    binding.tvAno.text = "사용 가능한 이메일 입니다."
                    binding.llPassword.visibility = View.VISIBLE
                    binding.tvAno.setTextColor(Color.BLUE)
                } else if (emailFlag == false) {
                    binding.tvAno.visibility = View.VISIBLE
                    binding.tvAno.text = """이메일 형식이 올바르지 않습니다. 
                | ex) XXXXX@gmail.com""".trimMargin()
                    binding.tvAno.setTextColor(Color.RED)
                    binding.llPassword.visibility = View.INVISIBLE
                } else if (flag == false) {
                    binding.llPassword.visibility = View.INVISIBLE
                }
            }
        })

        binding.edtRegisterPw.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                if (s!!.matches("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[$@$!%*#?&.])[A-Za-z[0-9]$@$!%*#?&.]{8,15}$".toRegex())) {
                    passwordFlag = true
                    binding.tvAnot.setTextColor(Color.BLACK)
                } else {
                    binding.tvAnot.setTextColor(Color.RED)
                }
                pw = s.toString()
            }
        })

        binding.edtRegisterRePw.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (pw!! == s.toString() && passwordFlag == true) {
                    binding.btnNext.visibility = View.VISIBLE
                    binding.tvAno3.visibility = View.INVISIBLE
                } else {
                    binding.btnNext.visibility = View.INVISIBLE
                    binding.tvAno3.visibility = View.VISIBLE
                    binding.tvAno3.setTextColor(Color.RED)

                }
            }
        })

        binding.btnNext.setOnClickListener {
            id = binding.edtRegisterId.text.toString()
            pw = binding.edtRegisterPw.text.toString()
            val intent = Intent(this, SignUpActivity2::class.java)

            intent.putExtra("id", id)
            intent.putExtra("pw", pw)
            startActivity(intent)

        }
    }

    fun getEditorActionListener(view: View): TextView.OnEditorActionListener { // 키보드에서 done(완료) 클릭 시 , 원하는 뷰 클릭되게 하는 메소드
        return TextView.OnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                view.callOnClick()
            }
            false
        }
    }

    fun idCheckFirebase(id: String) {
        Firebase.database.reference.child("User").child("users").orderByChild("userEmail")
            .equalTo(id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value == null) { //채팅방이 없는 경우
                        flag = true
                    } else if (snapshot.value != null) {
                        flag = false
                        binding.llPassword.visibility = View.INVISIBLE
                        binding.tvAno.visibility = View.VISIBLE
                        binding.tvAno.text = "중복된 이메일입니다."
                        binding.tvAno.setTextColor(Color.RED)
                    } else {
                        binding.tvAno.visibility = View.INVISIBLE
                        binding.tvAno.text = "이메일 입력중"
                    }

                }
            })
    }

    fun checkRegisterEmail(email: String) {
        emailFlag = false
        if (email.isNotEmpty() && !email.contains("@") && !(email.length > 15)) {
            emailFlag = false
            binding.tvAno.visibility = View.VISIBLE
            binding.tvAno.text = """이메일 형식이 올바르지 않습니다. 
                | ex) XXXXX@gmail.com""".trimMargin()
            binding.tvAno.setTextColor(Color.RED)
        } else if (email.isNotEmpty() && email.contains("@") && email.length > 15) {
            binding.tvAno.visibility = View.VISIBLE
            emailFlag = true
        }
    }
}