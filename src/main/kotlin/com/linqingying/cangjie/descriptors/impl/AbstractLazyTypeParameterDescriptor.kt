/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.SupertypeLoopChecker
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.Variance

abstract class AbstractLazyTypeParameterDescriptor(

    storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    variance: Variance,
//    isReified: Boolean,
    index: Int,
    source: SourceElement,
    supertypeLoopChecker: SupertypeLoopChecker
) :
    AbstractTypeParameterDescriptor(
        storageManager, containingDeclaration, annotations, name, variance, /*isReified, */index, source,
        supertypeLoopChecker
    ) {

    override fun toString(): String {
        // Not using descriptor renderer to preserve laziness
        return String.format(
            "%s%s%s",
  "",
           "",
            name
        )
    }

}
