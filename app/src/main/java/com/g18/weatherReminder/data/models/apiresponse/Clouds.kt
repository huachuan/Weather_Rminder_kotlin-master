package com.g18.weatherReminder.data.models.apiresponse

import com.squareup.moshi.Json

class Clouds(
  /**
   * Cloudiness, %
   */
  @Json(name = "all")
  val all: Long? = null
)