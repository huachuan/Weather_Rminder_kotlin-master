package com.g18.weatherReminder.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Transaction
import androidx.room.Update
import com.g18.weatherReminder.data.models.entity.City
import com.g18.weatherReminder.utils.debug

@Dao
abstract class City {
  @Insert(onConflict = IGNORE)
  abstract fun insertCity(city: City): Long

  @Update
  abstract fun updateCity(city: City)

  @Delete
  abstract fun deleteCity(city: City)

  @Transaction
  open fun upsert(city: City) {
    insertCity(city)
      .takeIf {
        debug("insertCity => $it", "__DAO__")
        it == -1L
      }
      ?.let { updateCity(city) }
  }
}