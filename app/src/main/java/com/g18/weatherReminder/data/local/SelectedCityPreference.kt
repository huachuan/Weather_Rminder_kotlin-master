package com.g18.weatherReminder.data.local

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import com.g18.weatherReminder.data.models.entity.City
import com.g18.weatherReminder.utils.None
import com.g18.weatherReminder.utils.Optional
import com.g18.weatherReminder.utils.asObservable
import com.g18.weatherReminder.utils.delegate
import com.g18.weatherReminder.utils.getOrNull
import com.g18.weatherReminder.utils.toOptional
import com.squareup.moshi.Moshi
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

@SuppressLint("CheckResult")
class SelectedCityPreference(sharedPreferences: SharedPreferences, private val moshi: Moshi) :
  PreferenceInterface<Optional<City>> {
  private var selectedCityJsonString by sharedPreferences.delegate<String>()
  private val citySubject = BehaviorSubject.createDefault<Optional<City>>(None)

  init {
    Single
      .fromCallable(::getSelectedCityFromSharedPref)
      .subscribeOn(Schedulers.single())
      .onErrorReturnItem(None)
      .subscribeBy(onSuccess = citySubject::onNext)
  }

  @WorkerThread private fun getSelectedCityFromSharedPref(): Optional<City> {
    return runCatching {
      moshi
        .adapter(City::class.java)
        .fromJson(selectedCityJsonString)
    }.getOrNull().toOptional()
  }

  /**
   * Save [value] to shared preference
   * @param value
   */
  @WorkerThread override fun save(value: Optional<City>) {
    selectedCityJsonString = moshi
      .adapter(City::class.java)
      .toJson(value.getOrNull())
    citySubject.onNext(value)
  }

  override val observable = citySubject.asObservable()

  override val value @WorkerThread get() = getSelectedCityFromSharedPref()
}

