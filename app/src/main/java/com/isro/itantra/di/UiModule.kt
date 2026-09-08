package com.isro.itantra.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Reserved for UI-scope Hilt bindings. Do not remove or rename this file.
 */
@Module
@InstallIn(SingletonComponent::class)
object UiModule
