package com.example.dukkanapp.core.error.reporter

import android.util.Log
import javax.inject.Inject

/** Sends unexpected throwables to crash reporting. Swap the binding for Crashlytics later. */
interface ErrorReporter {
    fun report(throwable: Throwable)
}

class LogCatErrorReporter @Inject constructor() : ErrorReporter {
    override fun report(throwable: Throwable) {
        Log.e("Dukkan App", "Unexpected failure", throwable)
    }
}