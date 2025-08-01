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
package cn.cangnova.cangjie.storage


/**
 * A storage for the value that should exist and be accessible in the single thread.
 *
 *
 * Unlike ThreadLocal, thread doesn't store a reference to the value that makes it inaccessible globally, but simplifies memory
 * management.
 *
 *
 * The other difference from ThreadLocal is inability to have different values per each thread, so SingleThreadValue instance
 * should be protected with external lock from rewrites.
 *
 * @param <T>
</T> */
internal class SingleThreadValue<T>(  val value: T) {
    private val thread: Thread

    init {
        thread = Thread.currentThread()
    }

    fun hasValue(): Boolean {
        return thread === Thread.currentThread()
    }


}
