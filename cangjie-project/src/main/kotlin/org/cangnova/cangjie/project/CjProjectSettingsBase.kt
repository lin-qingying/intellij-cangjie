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

package org.cangnova.cangjie.project

import com.intellij.openapi.components.BaseState
import com.intellij.util.messages.Topic
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class AffectsHighlighting

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class AffectsMetadata

abstract class CjProjectSettingsBase<T : CjProjectSettingsBase<T>> : BaseState() {
    abstract fun copy(): T
}


    val CANGJIE_SETTINGS_TOPIC: Topic<CjSettingsListener> = Topic.create(
        "cangjie settings changes",
        CjSettingsListener::class.java,
        Topic.BroadcastDirection.TO_PARENT
    )

abstract class SettingsChangedEventBase<T : CjProjectSettingsBase<T>>(val oldState: T, val newState: T) {
    private val highlightingAffectingProps: List<KProperty1<T, *>> =
        oldState.javaClass.kotlin.memberProperties.filter { it.findAnnotation<AffectsHighlighting>() != null }

    val affectsHighlighting: Boolean
        get() = highlightingAffectingProps.any(::isChanged)
    val affectsMetadata: Boolean
        get() = metadataAffectingProps.any(::isChanged)
    private val metadataAffectingProps: List<KProperty1<T, *>> =
        oldState.javaClass.kotlin.memberProperties.filter { it.findAnnotation<AffectsMetadata>() != null }

    fun isChanged(prop: KProperty1<T, *>): Boolean = prop.get(oldState) != prop.get(newState)

}

interface CjSettingsListener {
    fun <T : CjProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>)
}