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

package cn.cangnova.cangjie.utils

/**
 * 将异常转换为非受检异常并重新抛出。
 * 
 * 此函数的返回类型被指定为RuntimeException，使得可以按如下方式使用：
 * ```
 * throw rethrow(e)
 * ```
 * 在这种情况下，编译器知道在重新抛出异常之后的代码不会被执行。
 * 
 * @param e 需要重新抛出的异常
 * @return 此函数实际上不会返回，因为它总是抛出异常
 * @throws Throwable 总是抛出传入的异常
 */
fun rethrow(e: Throwable): RuntimeException {
    throw e
}

/**
 * 检查当前异常是否为ProcessCanceledException或其子类。
 * 
 * 此函数通过检查异常的类名来判断，而不是直接依赖于类型检查，
 * 这允许在不直接依赖于IntelliJ平台API的情况下检测ProcessCanceledException。
 * 
 * @return 如果异常是ProcessCanceledException或其子类，则返回true；否则返回false
 */
fun Throwable.isProcessCanceledException(): Boolean {
    var klass: Class<out Any?> = this.javaClass
    while (true) {
        if (klass.canonicalName == "com.intellij.openapi.progress.ProcessCanceledException") return true
        klass = klass.superclass ?: return false
    }
}
