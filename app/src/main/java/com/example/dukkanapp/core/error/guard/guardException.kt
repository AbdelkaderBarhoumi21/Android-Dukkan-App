package com.example.dukkanapp.core.error.guard

import com.example.dukkanapp.core.error.mapper.AppFirebaseExceptionMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch


/**
 * The only try/catch a data source needs. Guarantees the call throws nothing but [AppException]
 * (or [CancellationException], which must always propagate).
 */
inline fun <T> guardException(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    throw AppFirebaseExceptionMapper.map(e)
}

/** Same contract for real-time listeners (`snapshots()`). */
fun <T> Flow<T>.guardException(): Flow<T> =
    catch { e -> if (e is CancellationException) e else throw AppFirebaseExceptionMapper.map(e) }