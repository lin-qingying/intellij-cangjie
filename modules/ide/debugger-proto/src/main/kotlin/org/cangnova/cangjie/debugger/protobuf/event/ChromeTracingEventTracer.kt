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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.messages.Topic
import org.jetbrains.annotations.NonNls
import java.io.Writer
import java.util.function.BiConsumer


@Service
class ChromeTracingEventTracer {
    companion object {
        private val LOG: Logger = Logger.getInstance("#cidr.util")

        @JvmStatic
        fun getInstance() =

            ApplicationManager.getApplication().getService(ChromeTracingEventTracer::class.java)

    }

    private var connection: MessageBusConnection? = null

    var impl: EventTracer = NoopEventTracer()

    fun begin(
        @NonNls eventCategory: String,
        @NonNls eventName: () -> String,
        @NonNls details: () -> String,
        @NonNls threadName: String
    ): BiConsumer<String?, Any?> = impl.begin(eventCategory, eventName, details, threadName)


    fun end(@NonNls threadName: String) {

        impl.end(threadName)
    }

    fun startTracing() {
        if (connection == null) {
            connection = ApplicationManager.getApplication().messageBus.connect()
            connection?.let { conn ->
                val topic: Topic<AnActionListener> = AnActionListener.TOPIC

                conn.subscribe(topic, object : AnActionListener {

                    private val eventToSpan: HashMap<AnActionEvent, EventSpan> = HashMap()

                    fun getEventToSpan(): HashMap<AnActionEvent, EventSpan> {
                        return eventToSpan
                    }

                    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {

                        if (!eventToSpan.containsKey(event)) {
                            val description = action.toString()
                            val context = event.dataContext.toString()
                            eventToSpan[event] = EventSpan("action", description, context)
                        }
                    }

                    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {

                        val span = eventToSpan.remove(event)
                        span?.close()
                    }
                })
            }
            impl = ArrayListBackedEventTracer()
            LOG.info("Chrome tracing started")
        }
    }

    fun stopTracing() {
        connection?.disconnect()
        connection = null
        impl = NoopEventTracer()
        LOG.info("Chrome tracing stopped")
    }

    fun write(out: Writer) {
        impl.write(out)
    }
}

const val CIDR_ENABLE_JSON_TRACER: String = "cidr.enable.json.tracer"

