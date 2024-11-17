/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.ide.run.cjpm.runconfig

import com.fasterxml.jackson.core.JacksonException
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.util.io.BaseOutputReader


inline fun <T, E> CjResult<T, E>.unwrapOrElse(op: (E) -> T): T = when (this) {
    is CjResult.Ok -> ok
    is CjResult.Err -> op(err)
}

class CjCapturingProcessHandler(commandLine: GeneralCommandLine) : CapturingProcessHandler(commandLine) {
    override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.BLOCKING

    companion object {
        fun startProcess(commandLine: GeneralCommandLine): CjResult<CjCapturingProcessHandler, ExecutionException> {
            return try {
                CjResult.Ok(CjCapturingProcessHandler(commandLine))
            } catch (e: ExecutionException) {
                CjResult.Err(e)
            }
        }
    }
}

fun <T, E : Throwable> CjResult<T, E>.unwrapOrThrow(): T = when (this) {
    is CjResult.Ok -> ok
    is CjResult.Err -> throw err
}

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

class CjDeserializationException(cause: JacksonException) : CjProcessExecutionOrDeserializationException(cause)
class CjModuleNotFound : CjProcessExecutionOrDeserializationException("module not found")


sealed class CjProcessExecutionException : CjProcessExecutionOrDeserializationException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)

    abstract val commandLineString: String

    class Start(
        override val commandLineString: String,
        cause: ExecutionException,
    ) : CjProcessExecutionException(cause)

    class Canceled(
        override val commandLineString: String,
        val output: ProcessOutput,
        message: String = errorMessage(commandLineString, output),
    ) : CjProcessExecutionException(message)

    class Timeout(
        override val commandLineString: String,
        val output: ProcessOutput,
    ) : CjProcessExecutionException(errorMessage(commandLineString, output))

    /** The process exited with non-zero exit code */
    class ProcessAborted(
        override val commandLineString: String,
        val output: ProcessOutput,
    ) : CjProcessExecutionException(errorMessage(commandLineString, output))

    companion object {
        fun errorMessage(commandLineString: String, output: ProcessOutput): String = """
            |Execution failed (exit code ${output.exitCode}).
            |$commandLineString
            |stdout : ${output.stdout}
            |stderr : ${output.stderr}
        """.trimMargin()
    }
}

sealed class CjProcessExecutionOrDeserializationException : RuntimeException {
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
}
