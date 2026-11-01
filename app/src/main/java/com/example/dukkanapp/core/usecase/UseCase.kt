package com.example.dukkanapp.core.usecase

import com.example.dukkanapp.core.error.AppResult
import kotlinx.coroutines.flow.Flow

interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): AppResult<R>
}

interface FlowUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}

object NoParams
