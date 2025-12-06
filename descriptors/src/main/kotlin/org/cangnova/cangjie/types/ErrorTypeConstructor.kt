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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.ProjectDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.error.ErrorEntity
import org.cangnova.cangjie.types.error.ErrorTypeKind
import kotlin.jvm.internal.Intrinsics


class ErrorTypeConstructor(val kind: ErrorTypeKind, vararg val formatParams: String) : TypeConstructor {
    private val debugText = ErrorEntity.ERROR_TYPE.debugText.format(kind.debugMessage.format(*formatParams))

    fun getParam(i: Int): String = formatParams[i]

    override val supertypes: Collection<CangJieType>
        get() = emptyList()
    override val isDenotable: Boolean = false
    override val declarationDescriptor: ClassifierDescriptor?
        get() = ErrorUtils.errorClass

    
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
        Intrinsics.checkNotNullParameter(cangjieTypeRefiner, "cangjieTypeRefiner")
        return this
    }

    override val isFinal: Boolean
        get() = false

    override val parameters: List<TypeParameterDescriptor>
        get() = emptyList()

    //    override fun getBuiltIns(): CangJieBuiltIns = DefaultBuiltIns.Instance
    override fun toString(): String = debugText


    override val builtIns: CangJieBuiltIns
        get() = DefaultBuiltIns
}


object DefaultBuiltIns : CangJieBuiltIns(ProjectDescriptor.ERROR, LockBasedStorageManager("DefaultBuiltIns"))
