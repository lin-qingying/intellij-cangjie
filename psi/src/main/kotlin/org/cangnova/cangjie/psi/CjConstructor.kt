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

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieConstructorStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException

abstract class CjConstructor<T : CjConstructor<T>> : CjDeclarationStub<CangJieConstructorStub<T>>, CjFunction {
    protected constructor(node: ASTNode) : super(node)
    protected constructor(stub: CangJieConstructorStub<T>, nodeType: CjConstructorElementType<T>) : super(
        stub,
        nodeType,
    )

    open fun getConstructorKeyword(): PsiElement? = findChildByType(CjTokens.INIT_KEYWORD)

    abstract fun getContainingTypeStatement(): CjTypeStatement

    override val isLocal = false
    override val bodyExpression: CjInitBlockExpression?get() {
        val stub = stub
        if (stub != null) {
            if (!stub.hasBody()) {
                return null
            }
            if (getContainingCjFile().isCompiled) {
                return null
            }
        }
        return findChildByClass(CjInitBlockExpression::class.java)
    }


    override val valueParameters: List<CjParameter>
        get() = valueParameterList?.parameters ?: emptyList()
    override val typeReference: CjTypeReference? = null
    override val valueParameterList: CjParameterList? get() = getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST)

    val delegationCall: CjConstructorDelegationCall? get() = bodyExpression?.getDelegationCall()
    fun getDelegationCallOrNull(): CjConstructorDelegationCall? = bodyExpression?.getDelegationCallOrNull()

    fun hasImplicitDelegationCall(): Boolean = delegationCall?.isImplicit == true

    fun replaceImplicitDelegationCallWithExplicit(isThis: Boolean): CjConstructorDelegationCall {
        return bodyExpression!!.replaceImplicitDelegationCallWithExplicit(isThis)
    }

    @Throws(IncorrectOperationException::class)
    override fun setTypeReference(typeRef: CjTypeReference?) =
        throw IncorrectOperationException("setTypeReference to constructor")

    override val colon get() = findChildByType<PsiElement>(CjTokens.COLON)

    override val equalsToken = null

    override fun hasBlockBody() = hasBody()

    fun isDelegatedCallToThis(): Boolean {
        stub?.let { return it.isDelegatedCallToThis() }
        return when (this) {
            is CjPrimaryConstructor -> false
            is CjSecondaryConstructor -> getDelegationCallOrNull()?.isCallToThis ?: true
            else -> throw IllegalStateException("Unknown constructor type: $this")
        }
    }

    override fun hasBody(): Boolean {
        stub?.let { return it.hasBody() }
        return bodyExpression != null
    }

    override val typeParameterList: CjTypeParameterList? = null
    override val typeConstraintList: CjTypeConstraintList? = null
    override val typeConstraints: List<CjTypeConstraint> = emptyList()
    override val typeParameters: List<CjTypeParameter> = emptyList()

    override fun hasDeclaredReturnType() = false

    override fun getName(): String? = getContainingTypeStatement().name

    override val fqName: FqName?
        get() = null

    override val nameAsSafeName: Name
        get() = CjPsiUtil.safeName(name)

    override val nameAsName: Name?
        get() = nameAsSafeName

    override fun getNameIdentifier() = null

    override fun getIdentifyingElement(): PsiElement? = getInitKeyword()

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement = throw IncorrectOperationException("setName to constructor")

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    open fun getInitKeyword(): PsiElement? = findChildByType(CjTokens.INIT_KEYWORD)

    fun hasConstructorKeyword(): Boolean = stub != null || getInitKeyword() != null

    override fun getTextOffset(): Int {
        return getInitKeyword()?.textOffset
            ?: valueParameterList?.textOffset
            ?: super.getTextOffset()
    }

    override fun getUseScope(): SearchScope {
        return getContainingTypeStatement().useScope
    }
}
