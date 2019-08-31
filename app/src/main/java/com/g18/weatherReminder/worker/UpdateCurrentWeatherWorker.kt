package com.g18.weatherReminder.worker

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.g18.weatherReminder.data.CurrentWeatherRepository
import com.g18.weatherReminder.data.NoSelectedCityException
import com.g18.weatherReminder.data.local.SettingPreferences
import com.g18.weatherReminder.utils.WEATHER_NOTIFICATION_ID
import com.g18.weatherReminder.utils.cancelNotificationById
import com.g18.weatherReminder.utils.debug
import com.g18.weatherReminder.utils.showNotificationIfEnabled
import com.g18.weatherReminder.worker.WorkerUtil.cancelUpdateCurrentWeatherWorkRequest
import io.reactivex.Single
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.LocalDateTime

class UpdateCurrentWeatherWorker(context: Context, workerParams: WorkerParameters) :
  RxWorker(context, workerParams), KoinComponent {
  private val currentWeatherRepository by inject<CurrentWeatherRepository>()
  private val settingPreferences by inject<SettingPreferences>()

  override fun createWork(): Single<Result> {
    return currentWeatherRepository
      .refreshCurrentWeatherOfSelectedCity()
      .doOnSubscribe { debug("[RUNNING] doWork ${LocalDateTime.now()}", TAG) }
      .doOnSuccess {
        debug("[SUCCESS] doWork $it", TAG)
        applicationContext.showNotificationIfEnabled(it, settingPreferences)
      }
      .doOnError {
        debug("[FAILURE] doWork $it", TAG)
        if (it is NoSelectedCityException) {
          debug("[FAILURE] cancel work request and notification", TAG)
          applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          cancelUpdateCurrentWeatherWorkRequest()
        }
      }
      .map { Result.success(workDataOf("RESULT" to "Update current success")) }
      .onErrorReturn { Result.failure(workDataOf("RESULT" to "Update current failure: ${it.message}")) }
  }

  companion object {
    const val UNIQUE_WORK_NAME = "com.hoc.weatherapp.worker.UpdateCurrentWeatherWorker"
    const val TAG = "com.hoc.weatherapp.worker.UpdateCurrentWeatherWorker"
  }
}