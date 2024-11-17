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

package com.linqingying.cangjie.resolve.calls.util

import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.intellij.psi.StubBasedPsiElement

/**
 * val lambda = fun(x: Int, _: String, `_`: Double) = 1
 *
 * This property is true only for second value parameter in the example above
 */
val CjNamedDeclaration.isSingleUnderscore: Boolean
    get() {
        // We don't want to call 'getNameIdentifier' on stubs to prevent text building
        // But it's fine because one-underscore names are prohibited for non-local declarations (only lambda parameters, local vars are allowed)
        if (this is StubBasedPsiElement<*> && this.stub != null) return false
        return nameIdentifier?.text == "_"
    }
