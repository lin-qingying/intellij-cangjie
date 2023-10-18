

package com.huawei.cangjie.stdext

sealed class CjResult<out T, out E> {
    data class Ok<T>(val ok: T) : CjResult<T, Nothing>()
    data class Err<E>(val err: E) : CjResult<Nothing, E>()

    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    fun ok(): T? = when (this) {
        is Ok -> ok
        is Err -> null
    }

    fun err(): E? = when (this) {
        is Ok -> null
        is Err -> err
    }

    inline fun <U> map(mapper: (T) -> U): CjResult<U, E> = when (this) {
        is Ok -> Ok(mapper(ok))
        is Err -> Err(err)
    }

    inline fun <U> mapErr(mapper: (E) -> U): CjResult<T, U> = when (this) {
        is Ok -> Ok(ok)
        is Err -> Err(mapper(err))
    }

    fun unwrap(): T = when (this) {
        is Ok -> ok
        is Err -> if (err is Throwable) {
            throw IllegalStateException("called `CjResult.unwrap()` on an `Err` value", err)
        } else {
            throw IllegalStateException("called `CjResult.unwrap()` on an `Err` value: $err")
        }
    }
}

inline fun <T, E, U> CjResult<T, E>.andThen(action: (T) -> CjResult<U, E>): CjResult<U, E> = when (this) {
    is CjResult.Ok -> action(ok)
    is CjResult.Err -> CjResult.Err(err)
}

@Suppress("unused")
inline fun <T, E, F> CjResult<T, E>.orElse(op: (E) -> CjResult<T, F>): CjResult<T, F> = when (this) {
    is CjResult.Ok -> CjResult.Ok(ok)
    is CjResult.Err -> op(err)
}

inline fun <T, E> CjResult<T, E>.unwrapOrElse(op: (E) -> T): T = when (this) {
    is CjResult.Ok -> ok
    is CjResult.Err -> op(err)
}

fun <T, E: Throwable> CjResult<T, E>.unwrapOrThrow(): T = when (this) {
    is CjResult.Ok -> ok
    is CjResult.Err -> throw err
}

fun <T : Any> T?.toResult(): CjResult<T, Unit> = if (this != null) CjResult.Ok(this) else CjResult.Err(Unit)
