package com.example.dukkanapp.core.di

import com.example.dukkanapp.core.error.mapper.AppFailureMapper
import com.example.dukkanapp.core.error.mapper.DefaultFailureMapper
import com.example.dukkanapp.core.error.reporter.ErrorReporter
import com.example.dukkanapp.core.error.reporter.LogCatErrorReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorModule {
    @Binds
    @Singleton
    abstract fun bindErrorReporter(impl: LogCatErrorReporter): ErrorReporter

    @Binds
    @Singleton
    abstract fun bindFailureMapper(impl: DefaultFailureMapper): AppFailureMapper
}