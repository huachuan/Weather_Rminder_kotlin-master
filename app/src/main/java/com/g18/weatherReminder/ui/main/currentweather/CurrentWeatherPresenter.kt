package com.g18.weatherReminder.ui.main.currentweather

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.g18.weatherReminder.data.CityRepository
import com.g18.weatherReminder.data.CurrentWeatherRepository
import com.g18.weatherReminder.data.NoSelectedCityException
import com.g18.weatherReminder.data.local.SettingPreferences
import com.g18.weatherReminder.data.models.PressureUnit
import com.g18.weatherReminder.data.models.SpeedUnit
import com.g18.weatherReminder.data.models.TemperatureUnit
import com.g18.weatherReminder.data.models.WindDirection
import com.g18.weatherReminder.data.models.entity.CityAndCurrentWeather
import com.g18.weatherReminder.ui.main.currentweather.CurrentWeatherContract.PartialStateChange
import com.g18.weatherReminder.ui.main.currentweather.CurrentWeatherContract.RefreshIntent
import com.g18.weatherReminder.ui.main.currentweather.CurrentWeatherContract.View
import com.g18.weatherReminder.ui.main.currentweather.CurrentWeatherContract.ViewState
import com.g18.weatherReminder.utils.None
import com.g18.weatherReminder.utils.Optional
import com.g18.weatherReminder.utils.Some
import com.g18.weatherReminder.utils.WEATHER_NOTIFICATION_ID
import com.g18.weatherReminder.utils.cancelNotificationById
import com.g18.weatherReminder.utils.debug
import com.g18.weatherReminder.utils.exhaustMap
import com.g18.weatherReminder.utils.notOfType
import com.g18.weatherReminder.utils.showNotificationIfEnabled
import com.g18.weatherReminder.utils.toZonedDateTime
import com.g18.weatherReminder.worker.WorkerUtil
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.ofType
import org.threeten.bp.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class CurrentWeatherPresenter(
  private val currentWeatherRepository: CurrentWeatherRepository,
  private val cityRepository: CityRepository,
  private val androidApplication: Application,
  private val settingPreferences: SettingPreferences
) : MviBasePresenter<View, ViewState>() {

  private val cityAndWeatherPartialChange = Observables.combineLatest(
    source1 = settingPreferences.speedUnitPreference.observable,
    source2 = settingPreferences.pressureUnitPreference.observable,
    source3 = settingPreferences.temperatureUnitPreference.observable,
    source4 = currentWeatherRepository.getSelectedCityAndCurrentWeatherOfSelectedCity(),
    combineFunction = { speedUnit, pressureUnit, temperatureUnit, optional ->
      Tuple4(
        speedUnit,
        pressureUnit,
        temperatureUnit,
        optional
      )
    }
  ).switchMap { (speedUnit, pressureUnit, temperatureUnit, optional) ->
    when (optional) {
      None -> showError(NoSelectedCityException)
      is Some -> Observable.just(
        toCurrentWeather(
          optional.value,
          speedUnit,
          pressureUnit,
          temperatureUnit
        )
      ).map<PartialStateChange> { PartialStateChange.Weather(it) }
    }.onErrorResumeNext(::showError)
  }

  private val refreshWeatherProcessor =
    ObservableTransformer<RefreshIntent, PartialStateChange> {
      it
        .publish { shared ->
          Observable.mergeArray(
            shared.ofType<RefreshIntent.InitialRefreshIntent>()
              .take(1)
              .delay { cityRepository.getSelectedCity().filter { it is Some } },
            shared.notOfType<RefreshIntent.InitialRefreshIntent>()
          )
        }
        .exhaustMap {
          currentWeatherRepository
            .refreshCurrentWeatherOfSelectedCity()
            .doOnSuccess {
              if (settingPreferences.autoUpdatePreference.value) {
                WorkerUtil.enqueueUpdateCurrentWeatherWorkRequest()
              }
              androidApplication.showNotificationIfEnabled(it, settingPreferences)
            }
            .doOnError {
              if (it is NoSelectedCityException) {
                androidApplication.cancelNotificationById(WEATHER_NOTIFICATION_ID)
                WorkerUtil.cancelUpdateCurrentWeatherWorkRequest()
                WorkerUtil.cancelUpdateDailyWeatherWorkRequest()
              }
            }
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .switchMap {
              Observable
                .timer(2_000, TimeUnit.MILLISECONDS)
                .map<PartialStateChange> { PartialStateChange.RefreshWeatherSuccess(showMessage = false) }
                .startWith(PartialStateChange.RefreshWeatherSuccess(showMessage = true))
            }
            .onErrorResumeNext(::showError)
        }
    }

  override fun bindIntents() {
    subscribeViewState(
      Observable.mergeArray(
        intent(View::refreshCurrentWeatherIntent).compose(refreshWeatherProcessor),
        cityAndWeatherPartialChange
      ).scan(ViewState(), reducer)
        .distinctUntilChanged()
        .doOnNext { debug("ViewState=$it", TAG) }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }

  private companion object {
    private const val TAG = "__current_weather__"

    private val LAST_UPDATED_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm")

    private data class Tuple4(
      val speedUnit: SpeedUnit,
      val pressureUnit: PressureUnit,
      val temperatureUnit: TemperatureUnit,
      val optional: Optional<CityAndCurrentWeather>
    )

    @JvmStatic
    private val reducer =
      BiFunction<ViewState, PartialStateChange, ViewState> { viewState, partialStateChange ->
        when (partialStateChange) {
          is PartialStateChange.Error -> viewState.copy(
            showError = partialStateChange.showMessage,
            error = partialStateChange.throwable,
            weather = if (partialStateChange.throwable is NoSelectedCityException) {
              null
            } else {
              viewState.weather
            }
          )
          is PartialStateChange.Weather -> viewState.copy(
            weather = partialStateChange.weather,
            error = null
          )
          is PartialStateChange.RefreshWeatherSuccess -> viewState.copy(
            showRefreshSuccessfully = partialStateChange.showMessage,
            error = null
          )
        }
      }

    @JvmStatic
    private fun toCurrentWeather(
      cityAndCurrentWeather: CityAndCurrentWeather,
      speedUnit: SpeedUnit,
      pressureUnit: PressureUnit,
      temperatureUnit: TemperatureUnit
    ): CurrentWeather {
      val weather = cityAndCurrentWeather.currentWeather
      val dataTimeString = weather
        .dataTime
        .toZonedDateTime(cityAndCurrentWeather.city.zoneId)
        .format(LAST_UPDATED_FORMATTER)
      return CurrentWeather(
        temperatureString = temperatureUnit.format(weather.temperature),
        pressureString = pressureUnit.format(weather.pressure),
        rainVolumeForThe3HoursMm = weather.rainVolumeForThe3Hours,
        visibilityKm = weather.visibility / 1_000,
        humidity = weather.humidity,
        description = weather.description.capitalize(),
        dataTimeString = dataTimeString,
        weatherConditionId = weather.weatherConditionId,
        weatherIcon = weather.icon,
        winSpeed = weather.winSpeed,
        winSpeedString = speedUnit.format(weather.winSpeed),
        winDirection = WindDirection.fromDegrees(weather.winDegrees).toString(),
        zoneId = cityAndCurrentWeather.city.zoneId
      )
    }

    @JvmStatic
    private fun showError(throwable: Throwable): Observable<PartialStateChange> {
      return Observable.timer(2_000, TimeUnit.MILLISECONDS)
        .map<PartialStateChange> {
          PartialStateChange.Error(throwable = throwable, showMessage = false)
        }
        .startWith(
          PartialStateChange.Error(throwable = throwable, showMessage = true)
        )
    }
  }
}