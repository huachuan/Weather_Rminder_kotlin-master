package com.g18.weatherReminder.ui.main

import android.annotation.SuppressLint
import android.app.Application
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import com.g18.weatherReminder.R
import com.g18.weatherReminder.utils.asObservable
import com.g18.weatherReminder.utils.debug
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject

@SuppressLint("CheckResult")
class ColorHolderSource(androidApplication: Application) {
  private val subject = BehaviorSubject.createDefault<@ColorInt Int>(
    ContextCompat.getColor(
      androidApplication,
      R.color.colorPrimaryDark
    )
  )

  val colorObservable = subject.asObservable()

  init {
    debug("ColorHolderSource::init", "ColorHolderSource")

    colorObservable.subscribeBy {
      debug(
        "ColorHolderSource onNext=$it",
        "ColorHolderSource"
      )
    }
  }

  @MainThread fun change(@ColorInt color: Int) = subject.onNext(color)
}