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

package org.cangnova.cangjie.cli.messages;


import org.cangnova.cangjie.cli.messages.CompilerMessageSeverity;
import org.cangnova.cangjie.cli.messages.CompilerMessageSourceLocation;
import org.cangnova.cangjie.cli.messages.MessageCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

public class PrintingMessageCollector implements MessageCollector {
    private final boolean verbose;
    private final PrintStream errStream;
    private final MessageRenderer messageRenderer;
    private boolean hasErrors = false;

    public PrintingMessageCollector(@NotNull PrintStream errStream, @NotNull MessageRenderer messageRenderer, boolean verbose) {
        this.verbose = verbose;
        this.errStream = errStream;
        this.messageRenderer = messageRenderer;
    }

    @Override
    public void clear() {
        // Do nothing, messages are already reported
    }

    @Override
    public void report(
            @NotNull CompilerMessageSeverity severity,
            @NotNull String message,
            @Nullable CompilerMessageSourceLocation location
    ) {
        if (!verbose && CompilerMessageSeverity.VERBOSE.contains(severity)) return;

        hasErrors |= severity.isError();

        errStream.println(messageRenderer.render(severity, message, location));
    }

    @Override
    public boolean hasErrors() {
        return hasErrors;
    }
}
