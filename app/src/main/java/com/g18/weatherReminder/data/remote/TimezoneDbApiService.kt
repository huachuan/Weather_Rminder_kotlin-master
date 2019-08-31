package com.g18.weatherReminder.data.remote

import com.g18.weatherReminder.data.models.apiresponse.timezonedb.TimezoneDbResponse
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import retrofit2.http.GET
import retrofit2.http.Query

const val BASE_URL_TIMEZONE_DB = "http://api.timezonedb.com/v2.1/"
const val TIMEZONE_DB_API_KEY = "AAHLCYFT7MLW"

interface TimezoneDbApiService {
  @GET("get-time-zone")
  fun getTimezoneByLatLng(
    @Query("lat") lat: Double,
    @Query("lng") lng: Double
  ): Single<TimezoneDbResponse>
}

fun getZoneId(
  timezoneDbApiService: TimezoneDbApiService,
  latitude: Double,
  longitude: Double
): Single<String> {
  return timezoneDbApiService
    .getTimezoneByLatLng(latitude, longitude)
    .subscribeOn(Schedulers.io())
    .map {
      if (it.status != "OK") {
        ""
      } else {
        it.zoneName
      }
    }
    .onErrorReturnItem("")
}
