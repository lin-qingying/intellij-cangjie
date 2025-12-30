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

package org.cangnova.cangjie.references

import com.intellij.util.SmartList
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.resolve.binding.BindingContext


internal class CangJieSyntheticPropertyAccessorReference(
    expression: CjNameReferenceExpression,
    getter: Boolean
) : SyntheticPropertyAccessorReference(expression, getter), CjReference {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
//        val descriptors = expression.getReferenceTargets(context)

        val result = SmartList<FunctionDescriptor>()
//        for (descriptor in descriptors) {
//            if (descriptor is SyntheticJavaPropertyDescriptor) {
//                if (getter) {
//                    result.add(descriptor.getMethod)
//                } else {
//                    if (descriptor.setMethod == null) result.addIfNotNull(descriptor.getMethod)
//                    else result.addIfNotNull(descriptor.setMethod)
//                }
//            }
//        }
        return result
    }

}
