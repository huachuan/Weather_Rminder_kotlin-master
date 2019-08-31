package com.g18.weatherReminder.data.local

import com.g18.weatherReminder.data.models.entity.DailyWeather
import io.reactivex.Completable
import io.reactivex.Observable

class FiveDayForecastLocalDataSource(private val fiveDayForecastDao: FiveDayForecastDao) {
  fun getAllDailyWeathersByCityId(id: Long): Observable<List<DailyWeather>> {
    return fiveDayForecastDao.getAllDailyWeathersByCityId(id)
  }

  fun deleteDailyWeathersByCityIdAndInsert(
    cityId: Long,
    weathers: List<DailyWeather>
  ): Completable {
    return Completable.fromAction {
      fiveDayForecastDao.deleteDailyWeathersByCityIdAndInsert(cityId, weathers)
    }
  }
}