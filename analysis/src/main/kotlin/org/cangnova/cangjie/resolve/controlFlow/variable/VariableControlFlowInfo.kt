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

package org.cangnova.cangjie.resolve.controlFlow.variable

import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInfo
import org.cangnova.cangjie.resolve.controlFlow.ReadOnlyControlFlowInfo
import org.cangnova.cangjie.utils.ImmutableHashMap
import org.cangnova.cangjie.utils.ImmutableMap
import javaslang.Tuple2

typealias VariableUsageReadOnlyControlInfo = ReadOnlyControlFlowInfo<VariableDescriptor, VariableUseState>
typealias VariableUsageControlFlowInfo<S, D> = ControlFlowInfo<S, VariableDescriptor, D>

interface VariableInitReadOnlyControlFlowInfo :
    ReadOnlyControlFlowInfo<VariableDescriptor, VariableControlFlowState> {
    fun checkDefiniteInitializationInMatch(merge: VariableInitReadOnlyControlFlowInfo): Boolean
}

class VariableInitControlFlowInfo(map: ImmutableMap<VariableDescriptor, VariableControlFlowState> = ImmutableHashMap.empty()) :
    VariableUsageControlFlowInfo<VariableInitControlFlowInfo, VariableControlFlowState>(map),
    VariableInitReadOnlyControlFlowInfo {
    override fun copy(newMap: ImmutableMap<VariableDescriptor, VariableControlFlowState>) =
        VariableInitControlFlowInfo(newMap)

    // this = output of EXHAUSTIVE_WHEN_ELSE instruction
    // merge = input of MergeInstruction
    // returns true if definite initialization in when happens here
    override fun checkDefiniteInitializationInMatch(merge: VariableInitReadOnlyControlFlowInfo): Boolean {
        for ((key, value) in iterator()) {
            if (value.initState == InitState.INITIALIZED_EXHAUSTIVELY &&
                merge.getOrNull(key)?.initState == InitState.INITIALIZED
            ) {
                return true
            }
        }
        return false
    }
}

operator fun <T1, T2> Tuple2<T1, T2>.component2(): T2 {
    return _2()
}

operator fun <T1, T2> Tuple2<T1, T2>.component1(): T1 {
    return _1()
}


class UsageVariableControlFlowInfo(map: ImmutableMap<VariableDescriptor, VariableUseState> = ImmutableHashMap.empty()) :
    VariableUsageControlFlowInfo<UsageVariableControlFlowInfo, VariableUseState>(map),
    VariableUsageReadOnlyControlInfo {
    override fun copy(newMap: ImmutableMap<VariableDescriptor, VariableUseState>) =
        UsageVariableControlFlowInfo(newMap)
}

enum class InitState(private val s: String) {
    // Definitely initialized
    INITIALIZED("I"),

    // Fake initializer in else branch of "exhaustive when without else", see MagicKind.EXHAUSTIVE_WHEN_ELSE
    INITIALIZED_EXHAUSTIVELY("IE"),

    // Initialized in some branches, not initialized in other branches
    UNKNOWN("I?"),

    // Definitely not initialized
    NOT_INITIALIZED("");

    fun merge(other: InitState): InitState {
        // X merge X = X
        // X merge IE = IE merge X = X
        // else X merge Y = I?
        if (this == other || other == INITIALIZED_EXHAUSTIVELY) return this
        if (this == INITIALIZED_EXHAUSTIVELY) return other
        return UNKNOWN
    }

    override fun toString() = s
}

class VariableControlFlowState private constructor(val initState: InitState, val isDeclared: Boolean) {

    fun definitelyInitialized(): Boolean = initState == InitState.INITIALIZED

    fun mayBeInitialized(): Boolean = initState != InitState.NOT_INITIALIZED

    override fun toString(): String {
        if (initState == InitState.NOT_INITIALIZED && !isDeclared) return "-"
        return "$initState${if (isDeclared) "D" else ""}"
    }

    companion object {

        private val VS_IT = VariableControlFlowState(InitState.INITIALIZED, true)
        private val VS_IF = VariableControlFlowState(InitState.INITIALIZED, false)
        private val VS_ET = VariableControlFlowState(InitState.INITIALIZED_EXHAUSTIVELY, true)
        private val VS_EF = VariableControlFlowState(InitState.INITIALIZED_EXHAUSTIVELY, false)
        private val VS_UT = VariableControlFlowState(InitState.UNKNOWN, true)
        private val VS_UF = VariableControlFlowState(InitState.UNKNOWN, false)
        private val VS_NT = VariableControlFlowState(InitState.NOT_INITIALIZED, true)
        private val VS_NF = VariableControlFlowState(InitState.NOT_INITIALIZED, false)

        fun create(initState: InitState, isDeclared: Boolean): VariableControlFlowState =
            when (initState) {
                InitState.INITIALIZED -> if (isDeclared) VS_IT else VS_IF
                InitState.INITIALIZED_EXHAUSTIVELY -> if (isDeclared) VS_ET else VS_EF
                InitState.UNKNOWN -> if (isDeclared) VS_UT else VS_UF
                InitState.NOT_INITIALIZED -> if (isDeclared) VS_NT else VS_NF
            }

        fun createInitializedExhaustively(isDeclared: Boolean): VariableControlFlowState =
            create(InitState.INITIALIZED_EXHAUSTIVELY, isDeclared)

        fun create(isInitialized: Boolean, isDeclared: Boolean = false): VariableControlFlowState =
            create(if (isInitialized) InitState.INITIALIZED else InitState.NOT_INITIALIZED, isDeclared)

        fun create(isDeclaredHere: Boolean, mergedEdgesData: VariableControlFlowState?): VariableControlFlowState =
            create(true, isDeclaredHere || mergedEdgesData != null && mergedEdgesData.isDeclared)
    }
}

enum class VariableUseState(private val priority: Int) {
    READ(3),
    WRITTEN_AFTER_READ(2),
    ONLY_WRITTEN_NEVER_READ(1),
    UNUSED(0);

    fun merge(variableUseState: VariableUseState?): VariableUseState {
        if (variableUseState == null || priority > variableUseState.priority) return this
        return variableUseState
    }

    companion object {

        @JvmStatic
        fun isUsed(variableUseState: VariableUseState?): Boolean =
            variableUseState != null && variableUseState != UNUSED
    }
}
