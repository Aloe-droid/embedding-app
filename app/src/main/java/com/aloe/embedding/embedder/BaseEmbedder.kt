package com.aloe.embedding.embedder

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.measureTimedValue

abstract class BaseEmbedder {

    private val scope = CoroutineScope(context = Dispatchers.Default)
    private val mutex = Mutex()
    private val initJob = scope.async { initInternal() }

    protected abstract suspend fun initInternal()
    protected abstract suspend fun embedInternal(input: String, isQuery: Boolean): FloatArray

    protected open fun close() {}

    suspend fun embed(
        input: String,
        isQuery: Boolean
    ): FloatArray {
        initJob.await()
        return withMutexLock { withCheckTime { embedInternal(input, isQuery) } }
    }

    private suspend inline fun <T> withMutexLock(
        crossinline execute: suspend () -> T
    ): T = mutex.withLock {
        execute()
    }

    private suspend inline fun <T> withCheckTime(
        crossinline execute: suspend () -> T
    ): T {
        val (value: T, timeTaken: kotlin.time.Duration) = measureTimedValue { execute() }
        Log.d(TAG, "임베딩에 걸린 총 시간: ${timeTaken.inWholeMilliseconds}ms")
        return value
    }

    companion object {
        private const val TAG = "EMBEDDER"
    }
}
