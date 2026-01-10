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

package org.cangnova.cangjie.psi.debugtext

import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjElementImplStub
import org.cangnova.cangjie.psi.CjPackageDirective
import org.cangnova.cangjie.psi.CjVisitor

// invoke this instead of getText() when you need debug text to identify some place in PSI without storing the element itself
// this is need to avoid unnecessary file parses
// this defaults to get text if the element is not stubbed
fun CjElement.getDebugText(): String {
    if (this !is CjElementImplStub<*> || this.stub == null) {
        return text
    }
    if (this is CjPackageDirective) {
        val fqName = fqName
        if (fqName.isRoot) {
            return ""
        }
        return "package " + fqName.asString()
    }
    return accept(DebugTextBuildingVisitor, Unit).toString()
}

private object DebugTextBuildingVisitor : CjVisitor<String, Unit>()
