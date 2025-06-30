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

package cn.cangnova.cangjie.diagnostics;

import cn.cangnova.cangjie.diagnostics.rendering.DefaultErrorMessages;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 诊断接收器接口
 * 
 * 用于接收和处理诊断信息
 */
public interface DiagnosticSink {
    /**
     * 不执行任何操作的诊断接收器
     * 
     * 忽略所有报告的诊断
     */
    DiagnosticSink DO_NOTHING = new DiagnosticSink() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
        }

        @Override
        public boolean wantsDiagnostics() {
            return false;
        }
    };

    /**
     * 抛出异常的诊断接收器
     * 
     * 当接收到错误级别的诊断时抛出异常
     */
    DiagnosticSink THROW_EXCEPTION = new DiagnosticSink() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getSeverity() == Severity.ERROR) {
                PsiFile psiFile = diagnostic.getPsiFile();
                List<TextRange> textRanges = diagnostic.getTextRanges();
                String diagnosticText = DefaultErrorMessages.render(diagnostic);
                throw new IllegalStateException(diagnostic.getFactory().getName() + ": " + diagnosticText + " " + PsiDiagnosticUtils
                        .atLocation(psiFile, textRanges.get(0)));
            }
        }

        @Override
        public boolean wantsDiagnostics() {
            return true;
        }
    };
    
    /**
     * 诊断回调接口
     * 
     * 用于处理诊断信息的回调
     */
    interface DiagnosticsCallback {
        /**
         * 处理诊断的回调方法
         * 
         * @param diagnostic 要处理的诊断
         */
        void callback(Diagnostic diagnostic);
    }

    /**
     * 报告诊断
     * 
     * @param diagnostic 要报告的诊断
     */
    void report(@NotNull Diagnostic diagnostic);

    /**
     * 设置诊断回调
     * 
     * 使用 {@link #setCallbackIfNotSet(DiagnosticsCallback)} 替代
     * 
     * @param callback 诊断回调
     * @deprecated 已过时
     */
    @Deprecated
    default void setCallback(@NotNull DiagnosticsCallback callback) {
        setCallbackIfNotSet(callback);
    }

    /**
     * 如果未设置回调，则设置诊断回调
     * 
     * @param callback 诊断回调
     * @return 如果成功设置回调则返回true，否则返回false
     */
    default boolean setCallbackIfNotSet(@NotNull DiagnosticsCallback callback) {
        return false;
    }

    /**
     * 重置诊断回调
     */
    default void resetCallback() { }

    /**
     * 检查是否需要诊断
     * 
     * @return 如果需要诊断则返回true，否则返回false
     */
    boolean wantsDiagnostics();
}
