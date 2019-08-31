package com.g18.weatherReminder.data

import com.g18.weatherReminder.data.LocalDataSourceUtil.saveCityAndCurrentWeather
import com.g18.weatherReminder.data.LocalDataSourceUtil.saveFiveDayForecastWeather
import com.g18.weatherReminder.data.local.CityLocalDataSource
import com.g18.weatherReminder.data.local.CurrentWeatherLocalDataSource
import com.g18.weatherReminder.data.local.FiveDayForecastLocalDataSource
import com.g18.weatherReminder.data.local.SelectedCityPreference
import com.g18.weatherReminder.data.models.entity.City
import com.g18.weatherReminder.data.models.entity.CityAndCurrentWeather
import com.g18.weatherReminder.data.models.entity.DailyWeather
import com.g18.weatherReminder.data.remote.OpenWeatherMapApiService
import com.g18.weatherReminder.data.remote.TimezoneDbApiService
import com.g18.weatherReminder.data.remote.getZoneId
import com.g18.weatherReminder.utils.None
import com.g18.weatherReminder.utils.Optional
import com.g18.weatherReminder.utils.Some
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers

class CurrentWeatherRepositoryImpl(
  private val openWeatherMapApiService: OpenWeatherMapApiService,
  private val timezoneDbApiService: TimezoneDbApiService,
  private val currentWeatherLocalDataSource: CurrentWeatherLocalDataSource,
  private val fiveDayForecastLocalDataSource: FiveDayForecastLocalDataSource,
  private val cityLocalDataSource: CityLocalDataSource,
  private val selectedCityPreference: SelectedCityPreference
) : CurrentWeatherRepository {
  private val selectedCityAndCurrentWeatherObservable =
    selectedCityPreference
      .observable
      .distinctUntilChanged()
      .switchMap { optionalCity ->
        when (optionalCity) {
          is Some -> currentWeatherLocalDataSource
            .getCityAndCurrentWeatherByCityId(optionalCity.value.id)
            .subscribeOn(Schedulers.io())
            .map(::Some)
          is None -> Observable.just(None)
        }
      }
      .replay(1)
      .autoConnect(0)

  private val refreshCurrentWeatherOfSelectedCitySingle =
    Single.fromCallable { selectedCityPreference.value }
      .flatMap {
        when (it) {
          is None -> Single.error(NoSelectedCityException)
          is Some -> openWeatherMapApiService
            .getCurrentWeatherByCityId(it.value.id)
            .subscribeOn(Schedulers.io())
            .zipWith(getZoneIdIfNeeded(it.value))
            .flatMap {
              saveCityAndCurrentWeather(
                cityLocalDataSource,
                currentWeatherLocalDataSource,
                it.first,
                it.second
              )
            }
        }
      }

  /**
   * Get all pair of city and current weather, get from local database
   * @return [Observable] that emits [List]s of [CityAndCurrentWeather]
   */
  override fun getAllCityAndCurrentWeathers(querySearch: String): Observable<List<CityAndCurrentWeather>> {
    return currentWeatherLocalDataSource
      .getAllCityAndCurrentWeathers(querySearch)
      .subscribeOn(Schedulers.io())
  }

  /**
   * Get pair of selected city and current weather, get from local database
   * @return [Observable] that emits [Optional]s of [CityAndCurrentWeather], [None] when having no selected city
   */
  override fun getSelectedCityAndCurrentWeatherOfSelectedCity() =
    selectedCityAndCurrentWeatherObservable

  /**
   * Refresh current weather of selected city, get from api
   * @return [Single] emit result or error ([NoSelectedCityException] when have no selected city)
   */
  override fun refreshCurrentWeatherOfSelectedCity() = refreshCurrentWeatherOfSelectedCitySingle

  /**
   * Refresh both current weather and five day forecast of [city], get from api
   * @return [Single] emit result or error
   */
  override fun refreshWeatherOf(city: City): Single<Pair<CityAndCurrentWeather, List<DailyWeather>>> {
    return openWeatherMapApiService
      .getCurrentWeatherByCityId(city.id)
      .subscribeOn(Schedulers.io())
      .zipWith(getZoneIdIfNeeded(city))
      .flatMap {
        saveCityAndCurrentWeather(
          cityLocalDataSource,
          currentWeatherLocalDataSource,
          it.first,
          it.second
        )
      }
      .zipWith(
        openWeatherMapApiService
          .get5DayEvery3HourForecastByCityId(city.id)
          .subscribeOn(Schedulers.io())
          .flatMap { saveFiveDayForecastWeather(fiveDayForecastLocalDataSource, it) }
      )
  }

  private fun getZoneIdIfNeeded(city: City): Single<String> {
    return if (city.zoneId.isNotEmpty()) {
      Single.just(city.zoneId)
    } else {
      getZoneId(timezoneDbApiService, city.lat, city.lng)
    }
  }
}