package com.g18.weatherReminder

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g18.weatherReminder.data.local.SettingPreferences
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class CancelNotificationReceiver : BroadcastReceiver(), KoinComponent {
  private val settingPreferences by inject<SettingPreferences>()

  @SuppressLint("CheckResult")
  override fun onReceive(context: Context, intent: Intent) {
    /*if (intent.action == ACTION_CANCEL_NOTIFICATION) {
      val pendingResult = goAsync()
      context.cancelNotificationById(WEATHER_NOTIFICATION_ID)

      Completable
        .fromCallable { settingPreferences.showNotificationPreference.saveActual(false) }
        .subscribeOn(Schedulers.single())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete { settingPreferences.showNotificationPreference.save(false) }
        .doOnTerminate { pendingResult.finish() }
        .subscribeBy(onComplete = {
          LocalBroadcastManager
            .getInstance(context)
            .sendBroadcast(Intent(ACTION_CANCEL_NOTIFICATION))
          debug("[SUCCESS] showNotificationPreference", TAG)
        })
    }*/
  }

  private companion object {
    const val TAG = "CancelNotificationReceiver"
  }
}
