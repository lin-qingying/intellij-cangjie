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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.resolve.caches.getResolutionFacade
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.components.Service
import org.cangnova.cangjie.resolve.binding.getReferenceTargets


internal class CangJieCompletionDummyIdentifierProviderService: AbstractCompletionDummyIdentifierProviderService()  {
    override fun allTargetsAreFunctionsOrClasses(nameReferenceExpression: CjNameReferenceExpression): Boolean {
        val bindingContext = nameReferenceExpression.getResolutionFacade().analyze(nameReferenceExpression, BodyResolveMode.PARTIAL)
        val targets = nameReferenceExpression.getReferenceTargets(bindingContext)
        return targets.isNotEmpty() && targets.all { it is FunctionDescriptor || it is ClassDescriptor && it.kind == ClassKind.CLASS }
    }
}
