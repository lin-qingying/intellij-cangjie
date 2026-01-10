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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.contracts.model

import org.cangnova.cangjie.builtins.CangJieBuiltIns
//import org.cangnova.cangjie.contracts.model.visitors.Reducer
import org.cangnova.cangjie.resolve.calls.inference.components.EmptySubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.TypeSubstitutorForConstraints

/**
 * An abstraction of effect-generating nature of some computation.
 *
 * One can think of Functor as of adjoint to function declaration, responsible
 * for generating effects. It's [invokeWithArguments] method roughly corresponds
 * to call of corresponding function, but instead of taking values and returning
 * values, it takes effects and returns effects.
 */
//interface Functor {
//    fun invokeWithArguments(arguments: List<Computation>, typeSubstitution: ESTypeSubstitution, reducer: Reducer): List<ESEffect>
//}

class ESTypeSubstitution(
    val substitutor: TypeSubstitutorForConstraints,
    val builtIns: CangJieBuiltIns
) {
    companion object {
        fun empty(builtIns: CangJieBuiltIns): ESTypeSubstitution {
            return ESTypeSubstitution(EmptySubstitutor, builtIns)
        }
    }
}
