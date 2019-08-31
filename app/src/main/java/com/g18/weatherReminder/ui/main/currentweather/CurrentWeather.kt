package com.g18.weatherReminder.ui.main.currentweather

data class CurrentWeather(
  val temperatureString: String,
  val pressureString: String,
  val humidity: Long,
  val rainVolumeForThe3HoursMm: Double,
  val weatherConditionId: Long,
  val weatherIcon: String,
  val description: String,
  val dataTimeString: String,
  val zoneId: String,
  val winSpeed: Double,
  val winSpeedString: String,
  val winDirection: String,
  val visibilityKm: Double
)