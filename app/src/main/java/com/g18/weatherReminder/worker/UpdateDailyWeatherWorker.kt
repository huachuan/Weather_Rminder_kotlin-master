package com.g18.weatherReminder.worker

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.g18.weatherReminder.data.FiveDayForecastRepository
import com.g18.weatherReminder.data.NoSelectedCityException
import com.g18.weatherReminder.utils.WEATHER_NOTIFICATION_ID
import com.g18.weatherReminder.utils.cancelNotificationById
import com.g18.weatherReminder.utils.debug
import com.g18.weatherReminder.worker.WorkerUtil.cancelUpdateDailyWeatherWorkRequest
import io.reactivex.Single
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.threeten.bp.LocalDateTime

class UpdateDailyWeatherWorker(context: Context, workerParams: WorkerParameters) :
  RxWorker(context, workerParams), KoinComponent {
  private val fiveDayForecastRepository by inject<FiveDayForecastRepository>()

  override fun createWork(): Single<Result> {
    return fiveDayForecastRepository
      .refreshFiveDayForecastOfSelectedCity()
      .doOnSubscribe { debug("[RUNNING] doWork ${LocalDateTime.now()}", TAG) }
      .doOnSuccess { debug("[SUCCESS] doWork $it", TAG) }
      .doOnError {
        debug("[FAILURE] doWork $it", TAG)
        if (it is NoSelectedCityException) {
          debug("[FAILURE] cancel work request and notification", TAG)
          applicationContext.cancelNotificationById(WEATHER_NOTIFICATION_ID)
          cancelUpdateDailyWeatherWorkRequest()
        }
      }
      .map { Result.success(workDataOf("RESULT" to "Update daily success")) }
      .onErrorReturn { Result.failure(workDataOf("RESULT" to "Update daily failure: ${it.message}")) }
  }

  companion object {
    const val UNIQUE_WORK_NAME = "com.hoc.weatherapp.worker.UpdateDailyWeatherWorker"
    const val TAG = "com.hoc.weatherapp.worker.UpdateDailyWeatherWorker"
  }
}