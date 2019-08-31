package com.g18.weatherReminder.ui.main.fivedayforecast

import android.app.Application
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import com.g18.weatherReminder.data.CityRepository
import com.g18.weatherReminder.data.FiveDayForecastRepository
import com.g18.weatherReminder.data.NoSelectedCityException
import com.g18.weatherReminder.data.local.SettingPreferences
import com.g18.weatherReminder.data.models.PressureUnit
import com.g18.weatherReminder.data.models.SpeedUnit
import com.g18.weatherReminder.data.models.TemperatureUnit
import com.g18.weatherReminder.data.models.WindDirection
import com.g18.weatherReminder.data.models.entity.City
import com.g18.weatherReminder.data.models.entity.DailyWeather
import com.g18.weatherReminder.ui.main.ColorHolderSource
import com.g18.weatherReminder.ui.main.fivedayforecast.DailyWeatherContract.PartialStateChange
import com.g18.weatherReminder.ui.main.fivedayforecast.DailyWeatherContract.PartialStateChange.Weather
import com.g18.weatherReminder.ui.main.fivedayforecast.DailyWeatherContract.RefreshIntent
import com.g18.weatherReminder.ui.main.fivedayforecast.DailyWeatherContract.View
import com.g18.weatherReminder.ui.main.fivedayforecast.DailyWeatherContract.ViewState
import com.g18.weatherReminder.utils.Some
import com.g18.weatherReminder.utils.WEATHER_NOTIFICATION_ID
import com.g18.weatherReminder.utils.cancelNotificationById
import com.g18.weatherReminder.utils.debug
import com.g18.weatherReminder.utils.exhaustMap
import com.g18.weatherReminder.utils.notOfType
import com.g18.weatherReminder.utils.toZonedDateTime
import com.g18.weatherReminder.utils.trim
import com.g18.weatherReminder.worker.WorkerUtil
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.ofType
import java.util.concurrent.TimeUnit

class DailyWeatherPresenter(
  private val fiveDayForecastRepository: FiveDayForecastRepository,
  private val cityRepository: CityRepository,
  private val settingPreferences: SettingPreferences,
  colorHolderSource: ColorHolderSource,
  private val androidApplication: Application
) : MviBasePresenter<View, ViewState>() {

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
          fiveDayForecastRepository
            .refreshFiveDayForecastOfSelectedCity()
            .doOnSuccess {
              if (settingPreferences.autoUpdatePreference.value) {
                WorkerUtil.enqueueUpdateDailyWeatherWorkRequest()
              }
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
            .onErrorResumeNext(showError)
        }
    }

  private val weatherChangePartialState = Observables.combineLatest(
    source1 = fiveDayForecastRepository.getFiveDayForecastOfSelectedCity()
      .ofType<Some<Pair<City, List<DailyWeather>>>>()
      .map { it.value },
    source2 = settingPreferences.temperatureUnitPreference.observable,
    source3 = settingPreferences.speedUnitPreference.observable,
    source4 = settingPreferences.pressureUnitPreference.observable,
    source5 = colorHolderSource.colorObservable,
    combineFunction = { list, temperatureUnit, speedUnit, pressureUnit, color ->
      Tuple5(list, temperatureUnit, speedUnit, pressureUnit, color)
    }
  ).map(tupleToWeatherPartialChange).onErrorResumeNext(showError)

  override fun bindIntents() {
    subscribeViewState(
      Observable.mergeArray(
        weatherChangePartialState,
        intent(View::refreshDailyWeatherIntent).compose(refreshWeatherProcessor)
      ).scan(ViewState(), reducer)
        .distinctUntilChanged()
        .doOnNext { debug("ViewState=$it", TAG) }
        .observeOn(AndroidSchedulers.mainThread()),
      View::render
    )
  }

  private companion object {
    private const val TAG = "__five_day_forecast__"

    private data class Tuple5(
      val weathers: Pair<City, List<DailyWeather>>,
      val temperatureUnit: TemperatureUnit,
      val speedUnit: SpeedUnit,
      val pressureUnit: PressureUnit,
      val iconBackgroundColor: Int
    )

    @JvmStatic
    private val tupleToWeatherPartialChange = Function<Tuple5, PartialStateChange> {
      val (cityAndWeathers, temperatureUnit, windSpeedUnit, pressureUnit, iconBackgroundColor) = it
      cityAndWeathers
        .second
        .groupBy { it.timeOfDataForecasted.trim() }
        .toSortedMap()
        .flatMap { (date, weathers) ->
          val zoneId = cityAndWeathers.first.zoneId

          listOf(DailyWeatherListItem.Header(date.toZonedDateTime(zoneId))) +
            weathers.map {
              DailyWeatherListItem.Weather(
                weatherIcon = it.icon,
                main = it.main,
                weatherDescription = it.description.capitalize(),
                temperatureMin = temperatureUnit.format(it.temperatureMin),
                temperatureMax = temperatureUnit.format(it.temperatureMax),
                temperature = temperatureUnit.format(it.temperature),
                dataTime = it.timeOfDataForecasted.toZonedDateTime(zoneId),
                cloudiness = "${it.cloudiness}%",
                humidity = "${it.humidity}%",
                rainVolumeForTheLast3Hours = "${it.rainVolumeForTheLast3Hours}mm",
                snowVolumeForTheLast3Hours = "${it.snowVolumeForTheLast3Hours}mm",
                windDirection = WindDirection.fromDegrees(it.winDegrees),
                groundLevel = pressureUnit.format(it.groundLevel),
                pressure = pressureUnit.format(it.pressure),
                seaLevel = pressureUnit.format(it.seaLevel),
                winSpeed = windSpeedUnit.format(it.windSpeed),
                iconBackgroundColor = iconBackgroundColor
              )
            }
        }
        .let(::Weather)
    }

    @JvmStatic
    private val reducer =
      BiFunction<ViewState, PartialStateChange, ViewState> { viewState, partialStateChange ->
        when (partialStateChange) {
          is DailyWeatherContract.PartialStateChange.Error -> viewState.copy(
            showError = partialStateChange.showMessage,
            error = partialStateChange.throwable
          )
          is DailyWeatherContract.PartialStateChange.Weather -> viewState.copy(
            dailyWeatherListItem = partialStateChange.dailyWeatherListItem,
            error = null
          )
          is DailyWeatherContract.PartialStateChange.RefreshWeatherSuccess -> viewState.copy(
            showRefreshSuccessfully = partialStateChange.showMessage,
            error = null
          )
        }
      }

    @JvmStatic
    private val showError = Function<Throwable, Observable<PartialStateChange>> { throwable ->
      Observable.timer(2_000, TimeUnit.MILLISECONDS)
        .map<PartialStateChange> {
          PartialStateChange.Error(
            showMessage = false,
            throwable = throwable
          )
        }
        .startWith(
          PartialStateChange.Error(
            showMessage = true,
            throwable = throwable
          )
        )
    }
  }
}
