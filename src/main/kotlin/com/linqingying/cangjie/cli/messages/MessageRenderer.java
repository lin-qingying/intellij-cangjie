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

package com.linqingying.cangjie.cli.messages;

import com.intellij.openapi.util.io.FileUtil;

import com.linqingying.cangjie.utils.FileUtilsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.io.File;

    public interface MessageRenderer {

    String PROPERTY_KEY = "com.linqingying.cangjie.cliMessageRenderer";

    MessageRenderer XML = new XmlMessageRenderer();

    MessageRenderer WITHOUT_PATHS = new PlainTextMessageRenderer() {
        @Nullable
        @Override
        protected String getPath(@NotNull CompilerMessageSourceLocation location) {
            return null;
        }

        @Override
        public String getName() {
            return "Pathless";
        }
    };

    MessageRenderer PLAIN_FULL_PATHS = new PlainTextMessageRenderer() {
        @NotNull
        @Override
        protected String getPath(@NotNull CompilerMessageSourceLocation location) {
            return location.getPath();
        }

        @Override
        public String getName() {
            return "FullPath";
        }
    };

    MessageRenderer PLAIN_RELATIVE_PATHS = new PlainTextMessageRenderer() {
        private final File cwd = new File(".").getAbsoluteFile();

        @NotNull
        @Override
        protected String getPath(@NotNull CompilerMessageSourceLocation location) {
            return FileUtilsKt.descendantRelativeTo(new File(location.getPath()), cwd).getPath();
        }

        @Override
        public String getName() {
            return "RelativePath";
        }
    };

    MessageRenderer SYSTEM_INDEPENDENT_RELATIVE_PATHS = new PlainTextMessageRenderer() {
        private final File cwd = new File(".").getAbsoluteFile();

        @Nullable
        @Override
        protected String getPath(@NotNull CompilerMessageSourceLocation location) {
            return FileUtil.toSystemIndependentName(
                    FileUtilsKt.descendantRelativeTo(new File(location.getPath()), cwd).getPath()
            );
        }

        @Override
        public String getName() {
            return "SystemIndependentRelativePath";
        }
    };

    MessageRenderer GRADLE_STYLE = new GradleStyleMessageRenderer();

    MessageRenderer XCODE_STYLE = new XcodeStyleMessageRenderer();

    String renderPreamble();

    String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageSourceLocation location);

    String renderUsage(@NotNull String usage);

    String renderConclusion();

    String getName();
}
