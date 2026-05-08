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
 */

package org.cangnova.cangjie.test

/**
 * 仓颉 IntelliJ 测试基类共享的最小测试命名契约。
 */
interface TestCase {
    val testFileExtension: String

    fun getTestName(lowercaseFirstLetter: Boolean): String

    companion object {
        @JvmStatic
        fun camelOrWordsToSnake(name: String): String {
            if (' ' in name) {
                return name.trim().replace(" ", "_")
            }

            return name.split("(?=[A-Z])".toRegex()).joinToString("_", transform = String::lowercase)
        }
    }
}
