/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.utils

/**
 * 将任意 Throwable 转换并重新抛出为未检查异常。
 * 该函数的返回类型指定为 RuntimeException，以便可以像 `throw rethrow(e)` 这样的形式使用，
 * 告诉编译器该调用不会返回，从而便于控制流分析。
 */
fun rethrow(e: Throwable): RuntimeException {
    throw e
}

/**
 * 判断当前 Throwable 是否为 ProcessCanceledException（通过类名判断，避免直接依赖类引用）。
 *
 * @return 如果是 ProcessCanceledException 或其子类则返回 true，否则返回 false。
 */
fun Throwable.isProcessCanceledException(): Boolean {
    var klass: Class<out Any?> = this.javaClass
    while (true) {
        if (klass.canonicalName == "com.intellij.openapi.progress.ProcessCanceledException") return true
        klass = klass.superclass ?: return false
    }
}
