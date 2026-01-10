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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.stubs.CangJieContextReceiverStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import org.cangnova.cangjie.name.*

class CjContextReceiver : CjElementImplStub<CangJieContextReceiverStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieContextReceiverStub) : super(stub, CjStubElementTypes.CONTEXT_RECEIVER)

    fun targetLabel(): CjSimpleNameExpression? =
        findChildByType<CjContainerNode>(CjNodeTypes.LABEL_QUALIFIER)
            ?.findChildByType(CjNodeTypes.LABEL)

    fun labelName(): String? {
        stub?.let { return it.getLabel() }
        return targetLabel()?.referencedName
    }

    fun labelNameAsName(): Name? {
        stub?.let { stub -> return stub.getLabel()?.let { Name.identifier(it) } }
        return targetLabel()?.referencedNameAsName
    }

    fun typeReference(): CjTypeReference? = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    fun name(): String? = labelName() ?: typeReference()?.nameForReceiverLabel()
}
