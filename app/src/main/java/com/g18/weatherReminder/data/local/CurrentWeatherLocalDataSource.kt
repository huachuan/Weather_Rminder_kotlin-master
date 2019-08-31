package com.g18.weatherReminder.data.local

import com.g18.weatherReminder.data.models.entity.CityAndCurrentWeather
import com.g18.weatherReminder.data.models.entity.CurrentWeather
import io.reactivex.Completable
import io.reactivex.Observable

/**
 * A wrapper of [CurrentWeatherDao]
 */

class CurrentWeatherLocalDataSource(private val currentWeatherDao: CurrentWeatherDao) {
  fun getCityAndCurrentWeatherByCityId(cityId: Long): Observable<CityAndCurrentWeather> {
    return currentWeatherDao
      .getCityAndCurrentWeatherByCityId(cityId)
      .distinctUntilChanged()
  }

  fun getAllCityAndCurrentWeathers(querySearch: String): Observable<List<CityAndCurrentWeather>> {
    return currentWeatherDao
      .getAllCityAndCurrentWeathers(querySearch)
      .distinctUntilChanged()
  }

  fun insertOrUpdateCurrentWeather(weather: CurrentWeather): Completable {
    return Completable.fromAction {
      currentWeatherDao.upsert(weather)
    }
  }
}