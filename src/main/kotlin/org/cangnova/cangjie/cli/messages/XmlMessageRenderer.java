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

package org.cangnova.cangjie.cli.messages;

import com.intellij.openapi.util.text.StringUtil;

import org.cangnova.cangjie.cli.messages.CompilerMessageSeverity;
import org.cangnova.cangjie.cli.messages.CompilerMessageSourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XmlMessageRenderer implements MessageRenderer {
    @Override
    public String renderPreamble() {
        return "<MESSAGES>";
    }

    @Override
    public String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageSourceLocation location) {
        StringBuilder out = new StringBuilder();
        String tagName = severity.getPresentableName();
        out.append("<").append(tagName);
        if (location != null) {
            out.append(" path=\"").append(e(location.getPath())).append("\"");
            out.append(" line=\"").append(location.getLine()).append("\"");
            out.append(" column=\"").append(location.getColumn()).append("\"");
        }
        out.append(">");

        out.append(e(message));

        out.append("</").append(tagName).append(">\n");
        return out.toString();
    }

    private static String e(String str) {
        return StringUtil.escapeXmlEntities(str);
    }

    @Override
    public String renderUsage(@NotNull String usage) {
        return render(CompilerMessageSeverity.STRONG_WARNING, usage, null);
    }

    @Override
    public String renderConclusion() {
        return "</MESSAGES>";
    }

    @Override
    public String getName() {
        return "XML";
    }
}
