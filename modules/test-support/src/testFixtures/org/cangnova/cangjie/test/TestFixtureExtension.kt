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

import com.intellij.openapi.module.Module
import com.intellij.util.SmartFMap

/**
 * 对位 Kotlin `TestFixtureExtension` 的测试夹具扩展注册表。
 */
interface TestFixtureExtension {
    fun setUp(module: Module)

    fun tearDown()

    companion object {
        @Volatile
        private var instances = SmartFMap.emptyMap<String, TestFixtureExtension>()

        fun loadFixture(className: String, module: Module): TestFixtureExtension {
            instances[className]?.let { return it }

            return (Class.forName(className).getDeclaredConstructor().newInstance() as TestFixtureExtension).apply {
                setUp(module)
                instances = instances.plus(className, this)
            }
        }

        inline fun <reified T : TestFixtureExtension> loadFixture(module: Module): T {
            return loadFixture(T::class.qualifiedName!!, module) as T
        }

        fun getFixture(className: String): TestFixtureExtension? = instances[className]

        inline fun <reified T : TestFixtureExtension> getFixture(): T? {
            return getFixture(T::class.qualifiedName!!) as? T
        }

        fun unloadFixture(className: String) {
            instances[className]?.let {
                it.tearDown()
                instances = instances.minus(className)
            }
        }

        inline fun <reified T : TestFixtureExtension> unloadFixture() {
            unloadFixture(T::class.qualifiedName!!)
        }
    }
}
