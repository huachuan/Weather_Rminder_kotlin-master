package com.g18.weatherReminder.data.local

import com.g18.weatherReminder.data.models.entity.City
import io.reactivex.Completable

/**
 * A wrapper of [City]
 */

class CityLocalDataSource(private val city: com.g18.weatherReminder.data.local.City) {
  fun deleteCity(city: City): Completable {
    return Completable.fromAction {
      this.city.deleteCity(city)
    }
  }

  fun insertOrUpdateCity(city: City): Completable {
    return Completable.fromAction {
      this.city.upsert(city)
    }
  }
}