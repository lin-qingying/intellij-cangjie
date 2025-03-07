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

package cn.cangnova.cangjie.resolve.deprecation

import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor

abstract class DescriptorBasedDeprecationInfo : DeprecationInfo() {
    override val propagatesToOverrides: Boolean
        get() = forcePropagationToOverrides

    /**
     * Marks deprecation as necessary to propagate to overrides
     * even if LanguageFeature.StopPropagatingDeprecationThroughOverrides is disabled or one of the overrides "undeprecated"
     * See DeprecationResolver.deprecationByOverridden for details.
     *
     * Currently, it's only expected to be true for deprecation from unsupported JDK members that might be removed in future versions:
     * we'd like to mark their overrides as unsafe as well.
     *
     * Also, there's an implicit contract that if `forcePropagationToOverrides`, then `propagatesToOverrides` should also be true
     */
    open val forcePropagationToOverrides: Boolean
        get() = false

    abstract val target: DeclarationDescriptor
}

val DEPRECATED_FUNCTION_KEY = object : CallableDescriptor.UserDataKey<DescriptorBasedDeprecationInfo> {}
