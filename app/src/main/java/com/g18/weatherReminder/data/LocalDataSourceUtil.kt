package com.g18.weatherReminder.data

import com.g18.weatherReminder.data.local.CityLocalDataSource
import com.g18.weatherReminder.data.local.CurrentWeatherLocalDataSource
import com.g18.weatherReminder.data.local.FiveDayForecastLocalDataSource
import com.g18.weatherReminder.data.models.apiresponse.currentweatherapiresponse.CurrentWeatherResponse
import com.g18.weatherReminder.data.models.apiresponse.forecastweatherapiresponse.FiveDayForecastResponse
import com.g18.weatherReminder.data.models.entity.CityAndCurrentWeather
import com.g18.weatherReminder.data.models.entity.DailyWeather
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

object LocalDataSourceUtil {
  /**
   * Save city and current weather to local database
   * @param cityLocalDataSource
   * @param currentWeatherLocalDataSource
   * @param response Response from api
   * @return [Single] emit [CityAndCurrentWeather]
   */
  @JvmStatic
  fun saveCityAndCurrentWeather(
    cityLocalDataSource: CityLocalDataSource,
    currentWeatherLocalDataSource: CurrentWeatherLocalDataSource,
    response: CurrentWeatherResponse,
    zoneId: String
  ): Single<CityAndCurrentWeather> {
    val city = Mapper.responseToCity(response, zoneId)
    val weather = Mapper.responseToCurrentWeatherEntity(response)
    return cityLocalDataSource
      .insertOrUpdateCity(city)
      .andThen(
        currentWeatherLocalDataSource
          .insertOrUpdateCurrentWeather(weather)
      )
      .toSingleDefault(
        CityAndCurrentWeather().apply {
          this.city = city
          this.currentWeather = weather
        }
      )
      .subscribeOn(Schedulers.io())
  }

  /**
   * Save five day forecast weather to local database
   * @param fiveDayForecastLocalDataSource
   * @param response
   * @return [Single] emit [List] of [DailyWeather]s
   */
  @JvmStatic
  fun saveFiveDayForecastWeather(
    fiveDayForecastLocalDataSource: FiveDayForecastLocalDataSource,
    response: FiveDayForecastResponse
  ): Single<List<DailyWeather>> {
    val city = Mapper.responseToCity(response)
    val weathers = Mapper.responseToListDailyWeatherEntity(response)
    return fiveDayForecastLocalDataSource
      .deleteDailyWeathersByCityIdAndInsert(weathers = weathers, cityId = city.id)
      .toSingleDefault(weathers)
      .subscribeOn(Schedulers.io())
  }
}