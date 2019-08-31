package com.g18.weatherReminder.ui.main

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget

import com.bumptech.glide.request.transition.Transition
import com.hannesdorfmann.mosby3.mvi.MviActivity
import com.g18.weatherReminder.R
import com.g18.weatherReminder.data.models.entity.City
import com.g18.weatherReminder.data.models.entity.CurrentWeather
import com.g18.weatherReminder.ui.cities.CitiesActivity

import com.g18.weatherReminder.ui.main.currentweather.CurrentWeatherFragment
import com.g18.weatherReminder.ui.main.fivedayforecast.DailyWeatherFragment
import com.g18.weatherReminder.utils.asObservable
import com.g18.weatherReminder.utils.blur.GlideBlurTransformation
import com.g18.weatherReminder.utils.startActivity
import com.g18.weatherReminder.utils.ui.ZoomOutPageTransformer
import com.g18.weatherReminder.utils.ui.getBackgroundDrawableFromWeather

import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.get
import java.lang.ref.WeakReference

class MainActivity : MviActivity<MainContract.View, MainPresenter>(), MainContract.View {
  private val colorSubject = PublishSubject.create<@ColorInt Int>()
  private var mediaPlayer: MediaPlayer? = null
  private var asyncTask: AsyncTask<*, *, *>? = null
  private var target1: CustomViewTarget<*, *>? = null
  @Suppress("DEPRECATION")


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(FLAG_TRANSLUCENT_STATUS)

    setContentView(R.layout.activity_main)

    setSupportActionBar(toolbar)
    supportActionBar?.run {
      setDisplayShowTitleEnabled(false)
      setDisplayHomeAsUpEnabled(true)
      setHomeAsUpIndicator(R.drawable.ic_playlist_add_white_24dp)
    }

    setupViewPager()
  }

  override fun onStop() {
    super.onStop()

    stopSound()
  }

  override fun onDestroy() {
    super.onDestroy()

    asyncTask?.cancel(true)
    colorSubject.onComplete()

    stopSound()

    // free memory
    mediaPlayer?.release()
    mediaPlayer = null
  }

  private fun setupViewPager() {
    view_pager.run {
      val fragments: List<Fragment> = listOf(
        CurrentWeatherFragment(),
        DailyWeatherFragment()
      )
      adapter = SectionsPagerAdapter(
        supportFragmentManager,
        fragments
      )
      offscreenPageLimit = fragments.size

      setPageTransformer(true, ZoomOutPageTransformer())

      dots_indicator.setViewPager(view_pager)
      dots_indicator.setDotsClickable(true)
    }
  }

  private fun enableIndicatorAndViewPager(isEnable: Boolean) {
    if (isEnable) {
      dots_indicator.visibility = View.VISIBLE
      view_pager.pagingEnable = true
    } else {
      dots_indicator.visibility = View.INVISIBLE
      view_pager.setCurrentItem(0, true)
      view_pager.pagingEnable = false
    }
  }

  private class SectionsPagerAdapter(fm: FragmentManager, private val fragments: List<Fragment>) :
    FragmentPagerAdapter(fm) {
    override fun getItem(position: Int) = fragments[position]
    override fun getCount() = fragments.size
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      android.R.id.home -> true.also { startActivity<CitiesActivity>() }


      else -> super.onOptionsItemSelected(item)
    }
  }


  private fun updateBackground(
    weather: CurrentWeather,
    city: City
  ) {
    Glide
      .with(this)
      .apply { clear(target1);  asyncTask?.cancel(true) }
      .asBitmap()
      .load(getBackgroundDrawableFromWeather(weather, city))
      .apply(
        RequestOptions
          .bitmapTransform(GlideBlurTransformation(this, 20f))
          .fitCenter()
          .centerCrop()
      )
      .transition(BitmapTransitionOptions.withCrossFade())
      .into(object : CustomViewTarget<ImageView, Bitmap>(image_background) {
        override fun onLoadFailed(errorDrawable: Drawable?) = Unit

        override fun onResourceCleared(placeholder: Drawable?) = Unit

        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
          view.setImageBitmap(resource)
          asyncTask?.cancel(true)
          asyncTask = getVibrantColor(resource, WeakReference(this@MainActivity))
        }
      })
      .also { target1 = it }
  }


  private fun stopSound() {
    runCatching {
      mediaPlayer?.takeIf { it.isPlaying }?.stop()
    }
  }

  private fun playSound(weather: CurrentWeather) {
    runCatching {
      mediaPlayer?.takeIf { it.isPlaying }?.stop()
    }

  }

  override fun render(state: MainContract.ViewState) {
    window.statusBarColor = state.vibrantColor
    when (state) {
      is MainContract.ViewState.NoSelectedCity -> renderNoSelectedCity()
      is MainContract.ViewState.CityAndWeather -> renderCityAndWeather(state)
    }
  }

  override fun changeColorIntent() = colorSubject.asObservable()

  private fun renderCityAndWeather(state: MainContract.ViewState.CityAndWeather) {
    updateBackground(state.weather, state.city)

    toolbar_title.text = getString(
      R.string.city_name_and_country,
      state.city.name,
      state.city.country
    )
    playSound(state.weather)
    enableIndicatorAndViewPager(true)
  }


  private fun renderNoSelectedCity() {
    Glide.with(this)
      .apply { clear(target1);  asyncTask?.cancel(true) }
      .load(R.drawable.default_bg)
      .transition(DrawableTransitionOptions.withCrossFade())
      .apply(RequestOptions.fitCenterTransform().centerCrop())
      .apply(RequestOptions.bitmapTransform(GlideBlurTransformation(this, 25f)))
      .into(image_background)


    toolbar_title.text = getString(R.string.no_selected_city)
    stopSound()
    enableIndicatorAndViewPager(false)
  }

  override fun createPresenter() = get<MainPresenter>()

  companion object {
    @JvmStatic
    private fun getVibrantColor(
      resource: Bitmap,
      mainActivity: WeakReference<MainActivity>
    ): AsyncTask<*, *, *> {
      return Palette
        .from(resource)
        .generate { palette ->
          palette ?: return@generate

          @ColorInt val color = listOf(
            palette.getSwatchForTarget(Target.DARK_VIBRANT)?.rgb,
            palette.getSwatchForTarget(Target.VIBRANT)?.rgb,
            palette.getSwatchForTarget(Target.LIGHT_VIBRANT)?.rgb,
            palette.getSwatchForTarget(Target.DARK_MUTED)?.rgb,
            palette.getSwatchForTarget(Target.MUTED)?.rgb,
            palette.getSwatchForTarget(Target.DARK_MUTED)?.rgb
          ).find { it !== null }
            ?: mainActivity.get()?.let { ContextCompat.getColor(it, R.color.colorPrimaryDark) }
            ?: return@generate

          mainActivity.get()?.colorSubject?.onNext(color)
        }
    }
  }
}
