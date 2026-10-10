package com.example.dukkanapp.core.error.guard

import com.example.dukkanapp.core.error.AppResult
import com.example.dukkanapp.core.error.mapper.AppFailureMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The repository boundary: nothing thrown inside escapes as an exception.
 * Catches every [Exception] (not only AppException) so a DTO mapping bug can't crash the app;
 * JVM `Error`s (OOM) and cancellation still propagate.
 */

inline fun <T> AppFailureMapper.guardFailure(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    AppResult.Failure(map(e))
}

fun <T> Flow<T>.guardFailure(failureMapper: AppFailureMapper): Flow<AppResult<T>> =
    map<T, AppResult<T>> { AppResult.Success(it) }.catch { e ->
        if (e is CancellationException) throw e else emit(AppResult.Failure(failureMapper.map(e)))
    }