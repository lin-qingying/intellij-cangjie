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
 * 对位 Kotlin `KotlinBaseTest` 的基础测试模型容器。
 *
 * 这一层不绑定 IntelliJ 平台，也不引入仓颉额外语义，
 * 仅为更高层 generated / multi-file / multi-module 测试提供稳定的数据结构。
 */
open class CangJieBaseTest {
    open class TestFile @JvmOverloads constructor(
        @JvmField val name: String,
        @JvmField val content: String,
        @JvmField val directives: Directives = Directives(),
    ) : Comparable<TestFile> {
        override fun compareTo(other: TestFile): Int = name.compareTo(other.name)

        override fun hashCode(): Int = name.hashCode()

        override fun equals(other: Any?): Boolean = other is TestFile && other.name == name

        override fun toString(): String = name
    }

    open class TestModule(
        @JvmField val name: String,
        @JvmField val dependenciesSymbols: List<String>,
        @JvmField val friendsSymbols: List<String>,
    ) : Comparable<TestModule> {
        val dependencies: MutableList<TestModule> = arrayListOf()
        val friends: MutableList<TestModule> = arrayListOf()

        override fun compareTo(other: TestModule): Int = name.compareTo(other.name)

        override fun toString(): String = name
    }
}
