package com.saiferwp.currencyexchange.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

abstract class FlowUseCase<in P, out R> {
    open operator fun invoke(parameters: P): Flow<R> {
        return execute(parameters)
            .flowOn(Dispatchers.IO)
    }

    protected abstract fun execute(parameters: P): Flow<R>
}
