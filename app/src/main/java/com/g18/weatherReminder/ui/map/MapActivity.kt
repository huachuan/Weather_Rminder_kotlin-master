package com.g18.weatherReminder.ui.map

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.g18.weatherReminder.data.CityRepository
import org.koin.android.ext.android.inject

class MapActivity : AppCompatActivity() {
  private val cityRepository by inject<CityRepository>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
  }

}