package com.trashmuppet.pixelbeat.core.common

/**
 * Lightweight Either-style success / failure type that lets the core and
 * feature layers propagate errors without throwing across boundaries.
 *
 * Real network / cloud calls are explicitly out of scope per
 * `01_PRODUCT_PILLARS.md` and `05_TECH_STACK.md` — this exists for
 * persistence, export, and billing error fan-out.
 */
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val error: Throwable) : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onFailure(action: (Throwable) -> Unit): Result<T> {
        if (this is Failure) action(error)
        return this
    }

    fun getOrNull(): T? = (this as? Success)?.value
}

inline fun <T> runCatchingResult(block: () -> T): Result<T> =
    try {
        Result.Success(block())
    } catch (t: Throwable) {
        Result.Failure(t)
    }
