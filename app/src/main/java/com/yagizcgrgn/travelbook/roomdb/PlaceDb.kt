package com.yagizcgrgn.travelbook.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yagizcgrgn.travelbook.model.Place

@Database(entities = [Place::class], version = 1)
abstract class PlaceDb : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}