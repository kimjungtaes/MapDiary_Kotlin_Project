package com.example.mapdiary.fragment

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapdiary.activity.MainActivity
import com.example.mapdiary.dataclass.ListLayout
import com.example.mapdiary.R
import com.example.mapdiary.adapter.CustomBalloonAdapter
import com.example.mapdiary.adapter.ListAdapter
import com.example.mapdiary.databinding.FragmentTwoBinding
import com.example.mapdiary.mapdata.KakaoAPI
import com.example.mapdiary.mapdata.ResultSearchKeyword
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapFragment : Fragment(), MapView.MapViewEventListener, View.OnClickListener {
    companion object {
        const val BASE_URL = "https://dapi.kakao.com/"
        const val API_KEY = "YOUER_REST_API_KEY"  // REST API 키
    }

    private val bottomSheetBehavior by lazy { BottomSheetBehavior.from(binding.bottomSheet) }
    private lateinit var mainActivity: MainActivity
    private lateinit var eventListener: MarkerEventListener
    lateinit var binding: FragmentTwoBinding
    lateinit var mapView: MapView
    private val listItems = arrayListOf<ListLayout>()
    private val listAdapter = ListAdapter(listItems)
    private var pageNumber = 1
    private var keyword = ""
    private val ACCESS_FINE_LOCATION = 777 // Request Code
    private val handler = Handler()
    var trakingFlag = false
    var isMarkerAdded = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity // 형변환
    }

    override fun onStop() {
        super.onStop()
        try {
            val mapViewContainer = binding.mapView as ViewGroup
            mapViewContainer.removeView(mapView)
        } catch (e: java.lang.Exception) {
            Log.e("TwoFragment", "맵 초기화 오류")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTwoBinding.inflate(inflater)
        return binding.root
    }// end of create

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.isHideable = false

        bottomSheetBehavior.apply {
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_COLLAPSED -> { //접힘
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {  //펼쳐짐
                        }
                        BottomSheetBehavior.STATE_HIDDEN -> {}    //숨겨짐
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {} //절반 펼쳐짐
                        BottomSheetBehavior.STATE_DRAGGING -> {}  //드래그하는 중
                        BottomSheetBehavior.STATE_SETTLING -> {}  //(움직이다가) 안정화되는 중
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    //슬라이드 될때 offset / hide -1.0 ~ collapsed 0.0 ~ expended 1.0

                    binding.bottomSheet.apply {
                        if (slideOffset > 0) {
                            // 슬라이딩 중일 때
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        } else {
                            // 슬라이딩이 끝났을 때
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    }
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        MapRefresh()
        startTracking()
        handler.postDelayed({ stopTracking() }, 2000)
    }

    override fun onPause() {
        super.onPause()
        binding.etSearchField.clearFocus()
        binding.etSearchField.setText("")
        listItems.clear()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        startTracking()
        handler.postDelayed({ stopTracking() }, 2000)
    }

    fun MapRefresh() {
        try {
            mapView = MapView(mainActivity)
            val mapViewContainer = binding.mapView as ViewGroup
            mapViewContainer.addView(mapView)
        } catch (e: java.lang.Exception) {
            Log.e("TwoFragment", "맵 생성 오류")
        }
        eventListener = MarkerEventListener(mainActivity)

        mapView.setCalloutBalloonAdapter(CustomBalloonAdapter(layoutInflater))  // 커스텀 말풍선 등록
        mapView.setPOIItemEventListener(eventListener)

        // 리사이클러 뷰
        binding.rvList.layoutManager =
            LinearLayoutManager(mainActivity, LinearLayoutManager.VERTICAL, false)
        binding.rvList.adapter = listAdapter
        //리스트 아이템 클릭 시 해당 위치로 이동
        listAdapter.setItemClickListener(object : ListAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int) {
                val mapPoint =
                    MapPoint.mapPointWithGeoCoord(listItems[position].y, listItems[position].x)
                mapView.setMapCenterPointAndZoomLevel(mapPoint, 1, true)
            }
        })

        // 검색 버튼
        binding.btnSearch.setOnClickListener(this)
        // 이전 페이지 버튼
        binding.btnPrevPage.setOnClickListener(this)
        // 다음 페이지 버튼
        binding.btnNextPage.setOnClickListener(this)
        // 마커 추가 버튼
        binding.btnAddMaker.setOnClickListener(this)
        // 내 위치 확인 버튼
        binding.btnTarget.setOnClickListener(this)
        // 엔터키 이벤트

        val inputMethodManager =
            mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        binding.etSearchField.setOnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnSearch.performClick()
                v.clearFocus()
                handled = true
                inputMethodManager.hideSoftInputFromWindow(binding.btnSearch.windowToken, 0)
            }
            handled
        }
        // 파이어베이스에서 마커 정보 가져오기
        val database = Firebase.database.reference
        val markersRef = database.child("markers")
        markersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (markerSnapshot in snapshot.children) {
                    val markerInfo = markerSnapshot.value as HashMap<String, Any>
                    val name = markerInfo["name"] as String
                    val latitude = markerInfo["latitude"] as Double
                    val longitude = markerInfo["longitude"] as Double
                    val image = markerInfo["image"] as String
                    // 마커 생성
                    val marker = MapPOIItem()
                    marker.itemName = name
                    marker.tag = 0
                    marker.mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude)
                    marker.markerType = MapPOIItem.MarkerType.CustomImage          // 마커 모양 (커스텀)
                    marker.customImageResourceId = resources.getIdentifier(
                        image,
                        "drawable",
                        mainActivity.packageName
                    )   // 커스텀 마커 이미지
                    marker.selectedMarkerType =
                        MapPOIItem.MarkerType.CustomImage  // 클릭 시 마커 모양 (커스텀)
                    marker.customSelectedImageResourceId = resources.getIdentifier(
                        "red_$image",
                        "drawable",
                        mainActivity.packageName
                    )   // 클릭 시 커스텀 마커 이미지
                    marker.isCustomImageAutoscale = false      // 커스텀 마커 이미지 크기 자동 조정
                    marker.setCustomImageAnchor(0.5f, 1.0f)    // 마커 이미지 기준점
                    // 지도에 마커 추가
                    mapView.addPOIItem(marker)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
    }

    // 위치 권한 확인
    private fun permissionCheck() {
        val preference = mainActivity.getPreferences(AppCompatActivity.MODE_PRIVATE)
        val isFirstCheck = preference.getBoolean("isFirstPermissionCheck", true)
        if (ContextCompat.checkSelfPermission(
                mainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 없는 상태
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    mainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // 권한 거절 (다시 한 번 물어봄)
                val builder = AlertDialog.Builder(mainActivity)
                builder.setMessage("현재 위치를 확인하시려면 위치 권한을 허용해주세요.")
                builder.setPositiveButton("확인") { dialog, which ->
                    ActivityCompat.requestPermissions(
                        mainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        ACCESS_FINE_LOCATION
                    )
                }
                builder.setNegativeButton("취소") { dialog, which ->

                }
                builder.show()
            } else {
                if (isFirstCheck) {
                    // 최초 권한 요청
                    preference.edit().putBoolean("isFirstPermissionCheck", false).apply()
                    ActivityCompat.requestPermissions(
                        mainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        ACCESS_FINE_LOCATION
                    )
                } else {
                    // 다시 묻지 않음 클릭 (앱 정보 화면으로 이동)
                    val builder = AlertDialog.Builder(mainActivity)
                    builder.setMessage("현재 위치를 확인하시려면 설정에서 위치 권한을 허용해주세요.")
                    builder.setPositiveButton("설정으로 이동") { dialog, which ->
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:$mainActivity.packageName")
                        )
                        startActivity(intent)
                    }
                    builder.setNegativeButton("취소") { dialog, which ->

                    }
                    builder.show()
                }
            }
        }
    }

    // 권한 요청 후 행동
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACCESS_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 요청 후 승인됨 (추적 시작)
                Toast.makeText(mainActivity, "위치 권한이 승인되었습니다", Toast.LENGTH_SHORT).show()
                startTracking()
            } else {
                // 권한 요청 후 거절됨 (다시 요청 or 토스트)
                Toast.makeText(mainActivity, "위치 권한이 거절되었습니다", Toast.LENGTH_SHORT).show()
                permissionCheck()
            }
        }
    }

    // GPS가 켜져있는지 확인
    private fun checkLocationService(): Boolean {
        val locationManager =
            mainActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    // 위치추적 시작
    private fun startTracking() {
        mapView.currentLocationTrackingMode =
            MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading  //이 부분
    }

    // 위치추적 중지
    private fun stopTracking() {
        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
    }

    // 키워드 검색 함수
    private fun searchKeyword(keyword: String, page: Int) {
        val retrofit = Retrofit.Builder()          // Retrofit 구성
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(KakaoAPI::class.java)            // 통신 인터페이스를 객체로 생성
        val call = api.getSearchKeyword(API_KEY, keyword, page)    // 검색 조건 입력

        // API 서버에 요청
        call.enqueue(object : Callback<ResultSearchKeyword> {
            override fun onResponse(
                call: Call<ResultSearchKeyword>,
                response: Response<ResultSearchKeyword>
            ) {
                // 통신 성공
                addItemsAndMarkers(response.body())
            }

            override fun onFailure(call: Call<ResultSearchKeyword>, t: Throwable) {
                // 통신 실패
                Log.w("LocalSearch", "통신 실패: ${t.message}")
            }
        })
    }

    // 검색 결과 처리 함수
    private fun addItemsAndMarkers(searchResult: ResultSearchKeyword?) {
        if (searchResult?.documents.isNullOrEmpty()) {
            binding.rvList.visibility = View.GONE
        }
        if (!searchResult?.documents.isNullOrEmpty()) {
            binding.rvList.visibility = View.VISIBLE
            // 검색 결과 있음
            listItems.clear()                   // 리스트 초기화
            mapView.removeAllPOIItems() // 지도의 마커 모두 제거


            for (document in searchResult!!.documents) {
                // 결과를 리사이클러 뷰에 추가
                val item = ListLayout(
                    document.place_name,
                    document.road_address_name,
                    document.address_name,
                    document.x.toDouble(),
                    document.y.toDouble()
                )
                listItems.add(item)

                // 지도에 마커 추가
                val point = MapPOIItem()
                point.apply {
                    itemName = document.place_name
                    mapPoint = MapPoint.mapPointWithGeoCoord(
                        document.y.toDouble(),
                        document.x.toDouble()
                    )
                    markerType = MapPOIItem.MarkerType.BluePin
                    selectedMarkerType = MapPOIItem.MarkerType.RedPin
                }
                mapView.addPOIItem(point)
            }
            listAdapter.notifyDataSetChanged()

            binding.btnNextPage.isEnabled = !searchResult.meta.is_end // 페이지가 더 있을 경우 다음 버튼 활성화
            binding.btnPrevPage.isEnabled = pageNumber != 1             // 1페이지가 아닐 경우 이전 버튼 활성화

        } else {
            // 검색 결과 없음
            Toast.makeText(mainActivity, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 마커 클릭 이벤트 리스너
    class MarkerEventListener(val context: Context) : MapView.POIItemEventListener {
        override fun onPOIItemSelected(mapView: MapView?, poiItem: MapPOIItem?) {
            // 마커 클릭 시
        }

        override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?) {
            // 말풍선 클릭 시 (Deprecated)
            // 이 함수도 작동하지만 그냥 아래 있는 함수에 작성하자
        }

        override fun onCalloutBalloonOfPOIItemTouched(
            mapView: MapView?,
            poiItem: MapPOIItem?,
            buttonType: MapPOIItem.CalloutBalloonButtonType?
        ) {
            // 말풍선 클릭 시
        }

        override fun onDraggablePOIItemMoved(
            mapView: MapView?,
            poiItem: MapPOIItem?,
            mapPoint: MapPoint?
        ) {
            // 마커의 속성 중 isDraggable = true 일 때 마커를 이동시켰을 경우
        }
    }

    override fun onMapViewInitialized(p0: MapView?) {}

    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {}

    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {}

    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
        if (isMarkerAdded) { // 마커가 추가된 상태인 경우
            return // 함수 종료
        }
        // 마커 생성
        val marker = MapPOIItem()
        marker.itemName = "마커 이름"
        marker.tag = 0
        marker.mapPoint = MapPoint.mapPointWithGeoCoord(
            p1?.mapPointGeoCoord!!.latitude,
            p1?.mapPointGeoCoord!!.longitude
        )
        marker.markerType = MapPOIItem.MarkerType.CustomImage          // 마커 모양 (커스텀)
        marker.customImageResourceId = R.drawable.blue_pin               // 커스텀 마커 이미지
        marker.selectedMarkerType = MapPOIItem.MarkerType.CustomImage  // 클릭 시 마커 모양 (커스텀)
        marker.customSelectedImageResourceId = R.drawable.pin       // 클릭 시 커스텀 마커 이미지
        marker.isCustomImageAutoscale = false      // 커스텀 마커 이미지 크기 자동 조정
        marker.setCustomImageAnchor(0.5f, 1.0f)    // 마커 이미지 기준점
        // 지도에 마커 추가
        mapView.addPOIItem(marker)
        isMarkerAdded = true // 마커 추가 상태로 변경
        // 파이어베이스에 마커 정보 저장
        val database = Firebase.database.reference
        val markerRef = database.child("markers").push()
        val markerInfo = HashMap<String, Any>()
        markerInfo["name"] = "마커 이름"
        markerInfo["latitude"] = p1?.mapPointGeoCoord!!.latitude
        markerInfo["longitude"] = p1?.mapPointGeoCoord!!.longitude
        markerInfo["image"] = "blue_pin" // 마커 이미지 이름
        markerRef.setValue(markerInfo)
    }

    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {}

    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {}

    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {}

    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {}

    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {}

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_addMaker -> {
                Toast.makeText(mainActivity, "원하는 위치를 터치해서 마커를 추가하세요", Toast.LENGTH_SHORT).show()
                mapView.setMapViewEventListener(this)
                isMarkerAdded = false // 버튼을 누를 때마다 마커 추가 가능하도록 상태 변경
            }
            R.id.btn_target -> {
                if (checkLocationService()) {
                    // GPS가 켜져있을 경우
                    permissionCheck()
                    if (trakingFlag == false) {
                        Toast.makeText(mainActivity, "현재 위치 표시중", Toast.LENGTH_SHORT).show()
                        startTracking()
                        trakingFlag = true
                    } else if (trakingFlag == true) {
                        Toast.makeText(mainActivity, "현재 위치 표시중이 아님", Toast.LENGTH_SHORT).show()
                        stopTracking()
                        trakingFlag = false
                    }
                } else {
                    // GPS가 꺼져있을 경우
                    Toast.makeText(mainActivity, "GPS를 켜주세요", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.btn_search -> {
                val inputMethodManager =
                    mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyword = binding.etSearchField.text.toString()
                pageNumber = 1
                searchKeyword(keyword, pageNumber)
                inputMethodManager.hideSoftInputFromWindow(binding.btnSearch.windowToken, 0)
                bottomSheetBehavior.state =
                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                        BottomSheetBehavior.STATE_HALF_EXPANDED
                    } else
                        BottomSheetBehavior.STATE_COLLAPSED
            }
            R.id.btn_prevPage -> {
                pageNumber--
                binding.tvPageNumber.text = pageNumber.toString()
                searchKeyword(keyword, pageNumber)
            }
            R.id.btn_nextPage -> {
                pageNumber++
                binding.tvPageNumber.text = pageNumber.toString()
                searchKeyword(keyword, pageNumber)
            }
        }
    }
}