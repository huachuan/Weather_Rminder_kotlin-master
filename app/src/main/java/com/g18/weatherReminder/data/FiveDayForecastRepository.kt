package com.g18.weatherReminder.data

import com.g18.weatherReminder.data.models.entity.City
import com.g18.weatherReminder.data.models.entity.DailyWeather
import com.g18.weatherReminder.utils.Optional
import io.reactivex.Observable
import io.reactivex.Single

interface FiveDayForecastRepository {
  /**
   * Get stream of five day weather, get from local database
   * @return [Observable] emit [None] when having no selected city, otherwise emit [Some] of [DailyWeather]s with [City]
   */
  fun getFiveDayForecastOfSelectedCity(): Observable<Optional<Pair<City, List<DailyWeather>>>>

  /**
   * Refresh five day forecast of selected city, get from api
   * @return [Single] emit result or error, emit [NoSelectedCityException] when having no selected city
   */
  fun refreshFiveDayForecastOfSelectedCity(): Single<Pair<City, List<DailyWeather>>>
}