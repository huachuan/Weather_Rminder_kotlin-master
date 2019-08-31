package com.g18.weatherReminder.koin

import com.g18.weatherReminder.ui.addcity.AddCityPresenter
import com.g18.weatherReminder.ui.cities.CitiesPresenter
import com.g18.weatherReminder.ui.main.ColorHolderSource
import com.g18.weatherReminder.ui.main.MainPresenter

import com.g18.weatherReminder.ui.main.currentweather.CurrentWeatherPresenter
import com.g18.weatherReminder.ui.main.fivedayforecast.DailyWeatherPresenter
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.module

val presenterModule = module {
  factory { getCitiesPresenter() }

  factory { getCurrentWeatherPresenter() }

  factory { getAddCityPresenter() }

  factory { getDailyWeatherPresenter() }

  factory { getMainPresenter() }



  single { getColorHolderSource() }
}

private fun ModuleDefinition.getColorHolderSource() = ColorHolderSource(androidApplication())



private fun ModuleDefinition.getMainPresenter(): MainPresenter {
  return MainPresenter(get(), get(), androidApplication())
}

private fun ModuleDefinition.getDailyWeatherPresenter(): DailyWeatherPresenter {
  return DailyWeatherPresenter(get(), get(), get(), get(), androidApplication())
}

private fun ModuleDefinition.getAddCityPresenter(): AddCityPresenter {
  return AddCityPresenter(get(), get(), androidApplication())
}

private fun ModuleDefinition.getCurrentWeatherPresenter(): CurrentWeatherPresenter {
  return CurrentWeatherPresenter(get(), get(), androidApplication(), get())
}

private fun ModuleDefinition.getCitiesPresenter(): CitiesPresenter {
  return CitiesPresenter(get(), get(), get(), get(), androidApplication())
}