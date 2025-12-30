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

package org.cangnova.cangjie.resolve.calls.components.candidate

import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.model.CangJieCallComponents
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCallAtom
import org.cangnova.cangjie.resolve.calls.model.ResolvedAtom
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import org.cangnova.cangjie.types.TypeSubstitutor

/**
 * baseSystem contains all information from arguments, i.e. it is union of all system of arguments
 * Also by convention we suppose that baseSystem has no contradiction
 */
open class SimpleResolutionCandidate(
    override val callComponents: CangJieCallComponents,
    override val resolutionCallbacks: CangJieResolutionCallbacks,
    override val scopeTower: ImplicitScopeTower,
    override val baseSystem: ConstraintStorage,
    override val resolvedCall: MutableResolvedCallAtom,
    override val knownTypeParametersResultingSubstitutor: TypeSubstitutor? = null,
) : ResolutionCandidate() {
    override val variableCandidateIfInvoke: ResolutionCandidate?
        get() = callComponents.statelessCallbacks.getVariableCandidateIfInvoke(resolvedCall.atom)

    override fun getSubResolvedAtoms(): List<ResolvedAtom> = subResolvedAtoms

    override fun addResolvedCjPrimitive(resolvedAtom: ResolvedAtom) {
        subResolvedAtoms.add(resolvedAtom)
    }

    private var subResolvedAtoms: MutableList<ResolvedAtom> = arrayListOf()
}
