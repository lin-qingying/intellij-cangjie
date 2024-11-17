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

package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.linqingying.cangjie.descriptors.impl.LazySubstitutingClassDescriptor
import com.linqingying.cangjie.diagnostics.Errors.NOT_ENUM_MATCH
import com.linqingying.cangjie.diagnostics.Errors.NOT_ENUM_PARAMETER_CONSTRUCTOR
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjBindingPattern
import com.linqingying.cangjie.psi.CjConstantPattern
import com.linqingying.cangjie.psi.CjEnum
import com.linqingying.cangjie.psi.CjVisitor
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.linqingying.cangjie.resolve.BindingContext.REFERENCE_TARGET
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.calls.CallExpressionResolver
import com.linqingying.cangjie.resolve.scopes.findClassifier
import com.linqingying.cangjie.types.util.isEnum
