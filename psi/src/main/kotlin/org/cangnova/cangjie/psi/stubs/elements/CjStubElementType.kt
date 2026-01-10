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
package org.cangnova.cangjie.psi.stubs.elements

import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CjElementImplStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.util.ArrayFactory
import com.intellij.util.ReflectionUtil
import org.jetbrains.annotations.NonNls
import java.lang.reflect.Constructor

abstract class CjStubElementType<StubT : StubElement<*>, PsiT : CjElementImplStub<*>>(
    debugName: @NonNls String,
    psiClass: Class<PsiT>,
    stubClass: Class<*>,
) :
    IStubElementType<StubT, PsiT>(debugName, CangJieLanguage) {
    private val byNodeConstructor: Constructor<PsiT>
    private val byStubConstructor: Constructor<PsiT>
    private val emptyArray: Array<PsiT>
    val arrayFactory: ArrayFactory<PsiT>

    init {
        try {
            byNodeConstructor = psiClass.getConstructor(ASTNode::class.java)
            byStubConstructor = psiClass.getConstructor(stubClass)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(
                "Stub element type declaration for " + psiClass.simpleName + " is missing required constructors",
                e,
            )
        }
        emptyArray = java.lang.reflect.Array.newInstance(psiClass, 0) as Array<PsiT>
        arrayFactory = ArrayFactory { count: Int ->
            if (count == 0) {
                return@ArrayFactory emptyArray
            }
            java.lang.reflect.Array.newInstance(psiClass, count) as Array<PsiT>
        }
    }

    open fun createPsiFromAst(node: ASTNode): PsiT {
        return ReflectionUtil.createInstance(byNodeConstructor, node)
    }

    override fun createPsi(stub: StubT): PsiT {
        return ReflectionUtil.createInstance(byStubConstructor, stub)
    }

    override fun getExternalId(): String {
        return "cangjie.$this"
    }

    override fun indexStub(stub: StubT, sink: IndexSink) {
    }
}
