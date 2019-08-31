package com.g18.weatherReminder.data

import com.g18.weatherReminder.data.LocalDataSourceUtil.saveCityAndCurrentWeather
import com.g18.weatherReminder.data.LocalDataSourceUtil.saveFiveDayForecastWeather
import com.g18.weatherReminder.data.local.CityLocalDataSource
import com.g18.weatherReminder.data.local.CurrentWeatherLocalDataSource
import com.g18.weatherReminder.data.local.FiveDayForecastLocalDataSource
import com.g18.weatherReminder.data.local.SelectedCityPreference
import com.g18.weatherReminder.data.models.entity.City
import com.g18.weatherReminder.data.remote.OpenWeatherMapApiService
import com.g18.weatherReminder.data.remote.TimezoneDbApiService
import com.g18.weatherReminder.data.remote.getZoneId
import com.g18.weatherReminder.utils.None
import com.g18.weatherReminder.utils.Optional
import com.g18.weatherReminder.utils.Some
import com.g18.weatherReminder.utils.getOrNull
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.schedulers.Schedulers

class CityRepositoryImpl(
  private val openWeatherMapApiService: OpenWeatherMapApiService,
  private val timezoneDbApiService: TimezoneDbApiService,
  private val cityLocalDataSource: CityLocalDataSource,
  private val fiveDayForecastLocalDataSource: FiveDayForecastLocalDataSource,
  private val currentWeatherLocalDataSource: CurrentWeatherLocalDataSource,
  private val selectedCityPreference: SelectedCityPreference
) : CityRepository {
  /**
   * Synchronous access  selected city
   */
  override val selectedCity get() = selectedCityPreference.value.getOrNull()

  /**
   * Get stream of selected city
   * @return [Observable] emit [None] when having no selected city, otherwise emit [Some] of [City]
   */
  override fun getSelectedCity() = selectedCityPreference.observable

  /**
   * Delete [city]
   * @param city
   * @return a [Completable]
   */
  override fun deleteCity(city: City): Single<City> {
    return Completable.mergeArray(
      cityLocalDataSource
        .deleteCity(city)
        .subscribeOn(Schedulers.io()),
      /**
       * If [city] is current selected city, then [changeSelectedCity] to null
       */
      Single
        .fromCallable { selectedCityPreference.value }
        .filter { it.getOrNull() == city }
        .flatMapCompletable { changeSelectedCity(None) }
    ).toSingleDefault(city)
  }

  /**
   * Add city by [latitude] and [longitude]
   * @param latitude
   * @param longitude
   * @return a [Single] emit added city or emit error
   */
  override fun addCityByLatLng(latitude: Double, longitude: Double): Single<City> {
    return Singles.zip(
      openWeatherMapApiService
        .getCurrentWeatherByLatLng(latitude, longitude)
        .subscribeOn(Schedulers.io()),
      getZoneId(timezoneDbApiService, latitude, longitude)
    )
      .flatMap {
        saveCityAndCurrentWeather(
          cityLocalDataSource,
          currentWeatherLocalDataSource,
          it.first,
          it.second
        )
      }
      .map { it.city }
      .flatMap { city ->
        openWeatherMapApiService
          .get5DayEvery3HourForecastByCityId(city.id)
          .subscribeOn(Schedulers.io())
          .flatMap { saveFiveDayForecastWeather(fiveDayForecastLocalDataSource, it) }
          .map { city }
      }
  }

  /**
   * Change selected city to [city]
   * @param city
   * @return a [Completable], emit [SaveSelectedCityError] when error
   */
  override fun changeSelectedCity(city: City) = changeSelectedCity(Some(city))


  /**
   * Change selected city to [city]
   * @param optionalCity if [optionalCity] is [None], indicates that have not selected city
   * @return a [Completable], emit [SaveSelectedCityError] when error
   */
  private fun changeSelectedCity(optionalCity: Optional<City>): Completable {
    return Completable
      .fromCallable { selectedCityPreference.save(optionalCity) }
      .subscribeOn(Schedulers.single())
      .onErrorResumeNext { Completable.error(SaveSelectedCityError(it)) }
  }
}