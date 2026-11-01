package com.example.dukkanapp.core.error.mapper

import androidx.datastore.core.CorruptionException
import com.example.dukkanapp.core.error.AppException
import java.io.IOException

/** Step 1 for on-device storage: DataStore exception → [AppException]. */
object AppLocalExceptionMapper {
    fun map(throwable: Throwable): AppException = when (throwable) {
        is AppException -> throwable
        is CorruptionException -> AppException.Storage.Corrupted(throwable) // must be before IOException
        is IOException -> AppException.Storage.Io(throwable)
        else -> AppException.Unexpected(throwable)
    }
}