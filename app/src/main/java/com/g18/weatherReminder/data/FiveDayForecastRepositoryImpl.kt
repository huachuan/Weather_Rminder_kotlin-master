package com.g18.weatherReminder.data

import com.g18.weatherReminder.data.local.FiveDayForecastLocalDataSource
import com.g18.weatherReminder.data.local.SelectedCityPreference
import com.g18.weatherReminder.data.models.entity.City
import com.g18.weatherReminder.data.models.entity.DailyWeather
import com.g18.weatherReminder.data.remote.OpenWeatherMapApiService
import com.g18.weatherReminder.utils.None
import com.g18.weatherReminder.utils.Some
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class FiveDayForecastRepositoryImpl(
  private val openWeatherMapApiService: OpenWeatherMapApiService,
  private val fiveDayForecastLocalDataSource: FiveDayForecastLocalDataSource,
  private val selectedCityPreference: SelectedCityPreference
) : FiveDayForecastRepository {
  private val fiveDayForecastObservable = selectedCityPreference
    .observable
    .distinctUntilChanged()
    .switchMap { optional ->
      when (optional) {
        is Some -> fiveDayForecastLocalDataSource
          .getAllDailyWeathersByCityId(optional.value.id)
          .subscribeOn(Schedulers.io())
          .map { optional.value to it }
          .map(::Some)
        is None -> Observable.just(None)
      }
    }
    .replay(1)
    .autoConnect(0)
  private val refreshFiveDayForecast = Single
    .fromCallable { selectedCityPreference.value }
    .flatMap { optionalCity ->
      when (optionalCity) {
        is None -> Single.error(NoSelectedCityException)
        is Some -> openWeatherMapApiService
          .get5DayEvery3HourForecastByCityId(optionalCity.value.id)
          .subscribeOn(Schedulers.io())
          .flatMap {
            LocalDataSourceUtil.saveFiveDayForecastWeather(
              fiveDayForecastLocalDataSource,
              it
            )
          }
          .map { optionalCity.value to it }
      }
    }

  /**
   * Get stream of five day weather, get from local database
   * @return [Observable] emit [None] when having no selected city, otherwise emit [Some] of [DailyWeather]s with [City]
   */
  override fun getFiveDayForecastOfSelectedCity() = fiveDayForecastObservable

  /**
   * Refresh five day forecast of selected city, get from api
   * @return [Single] emit result or error, emit [NoSelectedCityException] when having no selected city
   */
  override fun refreshFiveDayForecastOfSelectedCity() = refreshFiveDayForecast
}