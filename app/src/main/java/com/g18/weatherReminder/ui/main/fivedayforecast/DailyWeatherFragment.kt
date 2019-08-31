package com.g18.weatherReminder.ui.main.fivedayforecast

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.g18.weatherReminder.R
import com.g18.weatherReminder.ui.main.fivedayforecast.DailyWeatherContract.RefreshIntent
import com.g18.weatherReminder.utils.debug
import com.g18.weatherReminder.utils.snackBar
import com.g18.weatherReminder.utils.ui.HeaderItemDecoration
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.cast
import io.reactivex.subjects.PublishSubject

import kotlinx.android.synthetic.main.fragment_daily_weather.*
import org.koin.android.ext.android.get

class DailyDetailActivity : AppCompatActivity() {
  companion object {
    const val TAG = "com.hoc.weatherapp.ui.main.fivedayforecast.daily_detail_activity"
    const val ITEM_KEY =
      "com.hoc.weatherapp.ui.main.fivedayforecast.daily_detail_activity_item"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)




  }



}

class DailyWeatherFragment : MviFragment<DailyWeatherContract.View, DailyWeatherPresenter>(),
  DailyWeatherContract.View {

  private var errorSnackBar: Snackbar? = null
  private var refreshSnackBar: Snackbar? = null

  private val dailyWeatherAdapter = DailyWeatherAdapter()
  private val compositeDisposable = CompositeDisposable()
  private val initialRefreshSubject = PublishSubject.create<RefreshIntent.InitialRefreshIntent>()

  override fun refreshDailyWeatherIntent(): Observable<RefreshIntent> {
    return swipe_refresh_layout.refreshes()
      .map { RefreshIntent.UserRefreshIntent }
      .cast<RefreshIntent>()
      .mergeWith(initialRefreshSubject)
      .doOnNext { debug("refreshDailyWeatherIntent", "_daily_weather_") }
  }

  override fun createPresenter() = get<DailyWeatherPresenter>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_daily_weather, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    recycler_daily_weathers.run {
      setHasFixedSize(true)
      val linearLayoutManager = LinearLayoutManager(context)
      layoutManager = linearLayoutManager
      adapter = dailyWeatherAdapter

      DividerItemDecoration(context, linearLayoutManager.orientation)
        .apply {
          ContextCompat.getDrawable(
            context,
            R.drawable.daily_weather_divider
          )?.let(::setDrawable)
        }
        .let(::addItemDecoration)
      addItemDecoration(HeaderItemDecoration(dailyWeatherAdapter))
    }
  }


  private fun showDetail(item: DailyWeatherListItem.Weather) {
    val context = requireContext()
    context.startActivity(
      Intent(context, DailyDetailActivity::class.java).apply {
        putExtra(DailyDetailActivity.ITEM_KEY, item)
      }
    )
  }

  override fun onPause() {
    super.onPause()
    compositeDisposable.clear()
  }

  override fun render(viewState: DailyWeatherContract.ViewState) {
    swipe_refresh_layout.isRefreshing = false

    dailyWeatherAdapter.submitList(viewState.dailyWeatherListItem)

    if (viewState.error != null && viewState.showError) {
      errorSnackBar?.dismiss()
      errorSnackBar = view?.snackBar(viewState.error.message ?: "An error occurred")
    }
    if (!viewState.showError) {
      errorSnackBar?.dismiss()
    }

    if (viewState.showRefreshSuccessfully) {
      refreshSnackBar?.dismiss()
      refreshSnackBar = view?.snackBar("Refresh successfully")
    } else {
      refreshSnackBar?.dismiss()
    }
  }
}
