package org.cangnova.cangjie.protodebugger.event

import org.jetbrains.annotations.NonNls
import java.lang.AutoCloseable
import java.util.function.BiConsumer


class EventSpan(
    @NonNls eventCategory: String,
    @NonNls eventName: () -> String,
    @NonNls details: () -> String,
    @field:NonNls private val threadName: String =  EventTracer.currentThreadName()
) : BiConsumer<String?, Any?>, AutoCloseable {
    @JvmOverloads
    constructor(
        @NonNls eventCategory: String,
        @NonNls eventName: String,
        @NonNls details: Any?,
        @NonNls threadName: String =  EventTracer.currentThreadName()
    ) : this(
        eventCategory,
        { eventName },
        { details.toString() },

        threadName
    )


    private var updater: BiConsumer<String?, Any?> ?= ChromeTracingEventTracer.getInstance().begin(eventCategory, eventName, details, this.threadName);

    override fun accept(
        @NonNls eventName: String?,
        @NonNls details: Any?
    )   {
        this.updater?.accept(eventName,details)
    }

    override fun close(){

        if (updater != null) {
            ChromeTracingEventTracer.getInstance().end(threadName)
            updater = null
        }
    }
}

