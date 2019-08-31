package com.g18.weatherReminder.ui.cities

import com.g18.weatherReminder.data.models.entity.City
import org.threeten.bp.ZonedDateTime

data class CityListItem(
  val city: City,
  val temperatureMin: String,
  val temperatureMax: String,
  val weatherDescription: String,
  val weatherConditionId: Long,
  val weatherIcon: String,
  val isSelected: Boolean = false,
  val lastUpdated: ZonedDateTime
)