package com.g18.weatherReminder.ui.main.fivedayforecast

import android.os.Parcelable
import androidx.annotation.ColorInt
import com.g18.weatherReminder.data.models.WindDirection
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime

interface DailyWeatherListItem {
  @Parcelize
  data class Weather(
    @ColorInt val iconBackgroundColor: Int,
    val weatherIcon: String,
    val dataTime: ZonedDateTime,
    val weatherDescription: String,
    val temperatureMin: String,
    val temperatureMax: String,
    val temperature: String,
    val pressure: String,
    val seaLevel: String,
    val groundLevel: String,
    val humidity: String,
    val main: String,
    val cloudiness: String,
    val winSpeed: String,
    val windDirection: WindDirection,
    val rainVolumeForTheLast3Hours: String,
    val snowVolumeForTheLast3Hours: String
  ) : DailyWeatherListItem, Parcelable

  data class Header(val date: ZonedDateTime) : DailyWeatherListItem
}
