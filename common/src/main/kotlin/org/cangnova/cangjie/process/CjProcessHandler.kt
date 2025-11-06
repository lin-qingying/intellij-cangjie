/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.process

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.openapi.util.Key
import com.intellij.util.io.BaseOutputReader
import com.pty4j.PtyProcess
import java.nio.charset.Charset

class CjProcessHandler : KillableProcessHandler, AnsiEscapeDecoder.ColoredTextAcceptor {
    private val decoder: AnsiEscapeDecoder?

    constructor(commandLine: GeneralCommandLine, processColors: Boolean = true) : super(commandLine) {
        setHasPty(commandLine is PtyCommandLine)
        setShouldDestroyProcessRecursively(!hasPty())
        decoder = if (processColors && !hasPty()) CjAnsiEscapeDecoder() else null
    }

    constructor(
        process: Process,
        commandRepresentation: String,
        charset: Charset,
        processColors: Boolean = true
    ) : super(process, commandRepresentation, charset) {
        setHasPty(process is PtyProcess)
        setShouldDestroyProcessRecursively(!hasPty())
        decoder = if (processColors && !hasPty()) CjAnsiEscapeDecoder() else null
    }

    override fun notifyTextAvailable(text: String, outputType: Key<*>) {
        var textN = text

//        if (!textN.contains("\r\n")) {
//            textN = textN.replace("\n", "\r\n")
//        }
        decoder?.escapeText(textN, outputType, this) ?: super.notifyTextAvailable(textN, outputType)

    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        super.notifyTextAvailable(text, attributes)
    }

    override fun readerOptions(): BaseOutputReader.Options =
        if (hasPty()) {
            BaseOutputReader.Options.forTerminalPtyProcess()
        } else {
            BaseOutputReader.Options.forMostlySilentProcess()
        }
}
