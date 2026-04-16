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

package org.cangnova.cangjie.debugger.protobuf.event

import org.jetbrains.annotations.NonNls
import java.lang.AutoCloseable
import java.util.function.BiConsumer


class EventSpan(
    @NonNls eventCategory: String,
    @NonNls eventName: () -> String,
    @NonNls details: () -> String,
    @field:NonNls private val threadName: String = EventTracer.currentThreadName()
) : BiConsumer<String?, Any?>, AutoCloseable {
    @JvmOverloads
    constructor(
        @NonNls eventCategory: String,
        @NonNls eventName: String,
        @NonNls details: Any?,
        @NonNls threadName: String = EventTracer.currentThreadName()
    ) : this(
        eventCategory,
        { eventName },
        { details.toString() },

        threadName
    )


    private var updater: BiConsumer<String?, Any?>? =
        ChromeTracingEventTracer.getInstance().begin(eventCategory, eventName, details, this.threadName);

    override fun accept(
        @NonNls eventName: String?,
        @NonNls details: Any?
    ) {
        this.updater?.accept(eventName, details)
    }

    override fun close() {

        if (updater != null) {
            ChromeTracingEventTracer.getInstance().end(threadName)
            updater = null
        }
    }
}

