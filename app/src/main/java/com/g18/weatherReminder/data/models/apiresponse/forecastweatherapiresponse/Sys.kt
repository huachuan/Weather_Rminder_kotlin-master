package com.g18.weatherReminder.data.models.apiresponse.forecastweatherapiresponse

import com.squareup.moshi.Json

class Sys(
  @Json(name = "pod")
  val pod: String? = null
)