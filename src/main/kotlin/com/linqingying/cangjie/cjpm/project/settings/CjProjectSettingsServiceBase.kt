package com.linqingying.cangjie.cjpm.project.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties


abstract class CjProjectSettingsServiceBase<T : CjProjectSettingsServiceBase.CjProjectSettingsBase<T>>(
    val project: Project,
    state: T
) : SimplePersistentStateComponent<T>(state) {
    companion object {
        val CANGJIE_SETTINGS_TOPIC: Topic<CjSettingsListener> = Topic.create(
            "cangjie settings changes",
            CjSettingsListener::class.java,
            Topic.BroadcastDirection.TO_PARENT
        )
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.PROPERTY)
    protected annotation class AffectsHighlighting
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.PROPERTY)
    protected annotation class AffectsCjpmMetadata

    fun modify(action: (T) -> Unit) {
        val oldState = state.copy()
        val newState = state.also(action)
        val event = createSettingsChangedEvent(oldState, newState)
        notifySettingsChanged(event)
    }

    protected abstract fun createSettingsChangedEvent(oldEvent: T, newEvent: T): SettingsChangedEventBase<T>
    protected open fun notifySettingsChanged(event: SettingsChangedEventBase<T>) {
        project.messageBus.syncPublisher(CANGJIE_SETTINGS_TOPIC).settingsChanged(event)

        if (event.affectsHighlighting) {
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

    abstract class CjProjectSettingsBase<T : CjProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    interface CjSettingsListener {
        fun <T : CjProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>)
    }

    abstract class SettingsChangedEventBase<T : CjProjectSettingsBase<T>>(val oldState: T, val newState: T) {
        private val highlightingAffectingProps: List<KProperty1<T, *>> =
            oldState.javaClass.kotlin.memberProperties.filter { it.findAnnotation<AffectsHighlighting>() != null }
        private val cjpmMetadataAffectingProps: List<KProperty1<T, *>> =
            oldState.javaClass.kotlin.memberProperties.filter { it.findAnnotation<AffectsCjpmMetadata>() != null }

        val affectsHighlighting: Boolean
            get() = highlightingAffectingProps.any(::isChanged)
        val affectsCjpmMetadata: Boolean
            get() = cjpmMetadataAffectingProps.any(::isChanged)

        fun isChanged(prop: KProperty1<T, *>): Boolean = prop.get(oldState) != prop.get(newState)

    }
}
