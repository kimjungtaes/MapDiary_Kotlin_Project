package com.example.mapdiary.activity

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.example.mapdiary.BuildConfig
import com.example.mapdiary.R
import com.example.mapdiary.adapter.ViewPagerAdapter
import com.example.mapdiary.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() ,BottomNavigationView.OnNavigationItemSelectedListener{
    lateinit var binding: ActivityMainBinding
    lateinit var viewPagerAdapter: ViewPagerAdapter
    var exitFlag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BuildConfig.APPLICATION_ID
        //Fragment를 customViewpagerAdapter에 추가
        viewPagerAdapter = ViewPagerAdapter(this)
        //안드로이드 화면 세로 고정하기
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        // 페이저 스와핑 비활성화
        binding.viewPager2.setUserInputEnabled(false);
        // 페이저에 어댑터 연결
        binding.viewPager2.adapter = ViewPagerAdapter(this)

        // 슬라이드하여 페이지가 변경되면 바텀네비게이션의 탭도 그 페이지로 활성화
        binding.viewPager2.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.bottomNavigation.menu.getItem(position).isChecked = true
                }
            }
        )
        // 리스너 연결
        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_one -> {
                // ViewPager의 현재 item에 첫 번째 화면을 대입
                binding.viewPager2.currentItem = 0
                return true
            }
            R.id.action_two -> {
                // ViewPager의 현재 item에 두 번째 화면을 대입
                binding.viewPager2.currentItem = 1
                return true
            }
            R.id.action_three -> {
                // ViewPager의 현재 item에 세 번째 화면을 대입
                binding.viewPager2.currentItem = 2
                return true
            }
            R.id.action_forth -> {
                // ViewPager의 현재 item에 세 번째 화면을 대입
                binding.viewPager2.currentItem = 3
                return true
            }
            else -> {
                return false
            }
        }
    }//onNavigationItemSelected end
    override fun onBackPressed() {
        if (exitFlag) {
            finishAffinity()
        } else {
            Toast.makeText(this, R.string.MA_string1, Toast.LENGTH_SHORT).show()
            exitFlag = true
            runDelayed(1500) {
                exitFlag = false
            }
        }
    }
    fun runDelayed(millis: Long, function: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed(function, millis)
    }

}

