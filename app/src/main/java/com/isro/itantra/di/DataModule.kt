package com.isro.itantra.di

import android.content.Context
import androidx.room.Room
import com.isro.itantra.data.db.ITantraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ITantraDatabase =
        Room.databaseBuilder(context, ITantraDatabase::class.java, "itantra.db").build()
}
