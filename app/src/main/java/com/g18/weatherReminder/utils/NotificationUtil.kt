package com.g18.weatherReminder.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_NOTIFICATION
import androidx.core.app.NotificationCompat
import androidx.core.text.HtmlCompat
import com.g18.weatherReminder.App
import com.g18.weatherReminder.data.local.SettingPreferences
import com.g18.weatherReminder.data.models.TemperatureUnit
import com.g18.weatherReminder.data.models.entity.City
import com.g18.weatherReminder.data.models.entity.CityAndCurrentWeather
import com.g18.weatherReminder.data.models.entity.CurrentWeather
import com.g18.weatherReminder.ui.SplashActivity
import com.g18.weatherReminder.utils.ui.getIconDrawableFromCurrentWeather
import org.threeten.bp.format.DateTimeFormatter

const val WEATHER_NOTIFICATION_ID = 2
const val ACTION_CANCEL_NOTIFICATION = "com.hoc.weatherapp.CancelNotificationReceiver"

private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm, MM/dd/yy")
private const val TAG = "__notification__"

fun Context.showOrUpdateNotification(
  weather: CurrentWeather,
  city: City,
  unit: TemperatureUnit,
  popUpAndSound: Boolean // TODO:something is wrong
) {
  val temperature = unit.format(weather.temperature)
  val rainAlert = weather.rainVolumeForThe3Hours
  val snowAlert = weather.snowVolumeForThe3Hours
  val alert : String = if (rainAlert > 10 || snowAlert > 10)  "Yes" else "No"
  val text = HtmlCompat.fromHtml(
    """$temperature
      |<br>
      |Current ${weather.description.capitalize()}
      |<br>
      |<i> Rain/Snow Alert in next 3 hours: ${alert}</i>
      |<br>
      |<i>Update time: ${weather.dataTime.toZonedDateTime(city.zoneId).format(DATE_TIME_FORMATTER)}</i>
      """.trimMargin(),
    HtmlCompat.FROM_HTML_MODE_LEGACY
  )
  val notification = NotificationCompat.Builder(this, App.CHANNEL_ID)
    .setSmallIcon(
      getIconDrawableFromCurrentWeather(
        weatherConditionId = weather.weatherConditionId,
        weatherIcon = weather.icon
      )
    )
    .setContentTitle("${city.name} - ${city.country}")
    .setContentText(temperature)
    .setStyle(NotificationCompat.BigTextStyle().bigText(text))

    .setAutoCancel(false)
    .setOngoing(true)
    .setWhen(System.currentTimeMillis())
    .apply {
      if (popUpAndSound) {
        priority = NotificationCompat.PRIORITY_HIGH
        setDefaults(NotificationCompat.DEFAULT_ALL)
        setSound(RingtoneManager.getDefaultUri(TYPE_NOTIFICATION))
      }

      val resultPendingIntent = PendingIntent.getActivity(
        this@showOrUpdateNotification,
        0,
        Intent(applicationContext, SplashActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
      )
      setContentIntent(resultPendingIntent)
    }.build()

  debug(
    "<top>.showOrUpdateNotification weather = [$weather], city = [$city], unit = [$unit], popUpAndSound = [$popUpAndSound]",
    TAG
  )
  debug(
    "<top>.showOrUpdateNotification notification = [$notification]",
    TAG
  )
  (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
    WEATHER_NOTIFICATION_ID,
    notification
  )
}

fun Context.cancelNotificationById(id: Int) =
  (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    .cancel(id).also { debug("<top>.cancelNotificationById id = [$id]", TAG) }


fun Context.showNotificationIfEnabled(
  cityAndCurrentWeather: CityAndCurrentWeather,
  settingPreferences: SettingPreferences
) {
  debug("<top>.showNotificationIfEnabled", TAG)
  debug(
    "cityAndCurrentWeather = [$cityAndCurrentWeather], settingPreferences = [$settingPreferences]",
    TAG
  )
  if (settingPreferences.showNotificationPreference.value) {
    showOrUpdateNotification(
      weather = cityAndCurrentWeather.currentWeather,
      city = cityAndCurrentWeather.city,
      unit = settingPreferences.temperatureUnitPreference.value,
      popUpAndSound = settingPreferences.soundNotificationPreference.value
    )
  }
}