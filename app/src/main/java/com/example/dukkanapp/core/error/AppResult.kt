package com.example.dukkanapp.core.error


/**
 * Outcome of an operation that can fail in an expected way.
 * Repositories return it, use cases compose it, ViewModels reduce it into UI state.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val failure: AppFailure) : AppResult<Nothing>
}

/** For operations with no meaningful return value (send email, update, delete...). */
typealias EmptyResult = AppResult<Unit>

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(data)
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppFailure) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(failure)
    return this
}

inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppFailure) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(failure)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

fun <T> AppResult<T>.asEmptyResult(): EmptyResult = map { }