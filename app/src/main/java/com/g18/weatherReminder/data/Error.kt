package com.g18.weatherReminder.data

object NoSelectedCityException : Exception() {
  override val message = "No selected city"
}

class SaveSelectedCityError(cause: Throwable) : Exception(cause)
