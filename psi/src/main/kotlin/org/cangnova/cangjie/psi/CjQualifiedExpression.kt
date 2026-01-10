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

import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.psi.psiUtil.siblings
import org.cangnova.cangjie.utils.firstIsInstanceOrNull
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import java.util.*
import javax.swing.Icon

interface CjQualifiedExpression : CjExpression {
    val receiverExpression: CjExpression
        get() {
            val expression = getExpression(false) ?: throw AssertionError("No receiver found: ${getElementTextWithContext()}")
            return expression
        }

    val selectorExpression: CjExpression?
        get() = getExpression(true)

    val operationTokenNode: ASTNode
        get() = node.findChildByType(CjTokens.OPERATIONS) ?: error(
            "No operation node for ${node.elementType}. Children: ${Arrays.toString(children)}",
        )

    val operationSign: CjSingleValueToken
        get() = operationTokenNode.elementType as CjSingleValueToken

    private fun getExpression(afterOperation: Boolean): CjExpression? {
        return operationTokenNode.psi?.siblings(afterOperation, false)?.firstIsInstanceOrNull<CjExpression>()
    }
}

class CjSynthesisQualifiedExpression(
    override val receiverExpression: CjExpression,
    override val selectorExpression: CjExpression?,

) : CjQualifiedExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        TODO("Not yet implemented")
    }

    override fun accept(visitor: PsiElementVisitor) {
        TODO("Not yet implemented")
    }

    override fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D) {
        TODO("Not yet implemented")
    }

    override fun acceptChildren(visitor: PsiElementVisitor) {
        TODO("Not yet implemented")
    }

    override fun getReference(): PsiReference? {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        TODO("Not yet implemented")
    }

    override fun getIcon(flags: Int): Icon {
        TODO("Not yet implemented")
    }

    override fun getProject(): Project {
        TODO("Not yet implemented")
    }

    override fun getLanguage(): Language {
        TODO("Not yet implemented")
    }

    override fun getManager(): PsiManager {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Array<PsiElement> {
        TODO("Not yet implemented")
    }

    override fun getParent(): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getFirstChild(): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getLastChild(): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getNextSibling(): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getPrevSibling(): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getContainingFile(): PsiFile {
        TODO("Not yet implemented")
    }

    override fun getTextRange(): TextRange {
        TODO("Not yet implemented")
    }

    override fun getStartOffsetInParent(): Int {
        TODO("Not yet implemented")
    }

    override fun getTextLength(): Int {
        TODO("Not yet implemented")
    }

    override fun findElementAt(offset: Int): PsiElement? {
        TODO("Not yet implemented")
    }

    override fun findReferenceAt(offset: Int): PsiReference? {
        TODO("Not yet implemented")
    }

    override fun getTextOffset(): Int {
        TODO("Not yet implemented")
    }

    override fun getText(): String {
        TODO("Not yet implemented")
    }

    override fun textToCharArray(): CharArray {
        TODO("Not yet implemented")
    }

    override fun getNavigationElement(): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getOriginalElement(): PsiElement {
        TODO("Not yet implemented")
    }

    override fun textMatches(text: CharSequence): Boolean {
        TODO("Not yet implemented")
    }

    override fun textMatches(element: PsiElement): Boolean {
        TODO("Not yet implemented")
    }

    override fun textContains(c: Char): Boolean {
        TODO("Not yet implemented")
    }

    override fun copy(): PsiElement {
        TODO("Not yet implemented")
    }

    override fun add(element: PsiElement): PsiElement {
        TODO("Not yet implemented")
    }

    override fun addBefore(element: PsiElement, anchor: PsiElement?): PsiElement {
        TODO("Not yet implemented")
    }

    override fun addAfter(element: PsiElement, anchor: PsiElement?): PsiElement {
        TODO("Not yet implemented")
    }

    override fun checkAdd(element: PsiElement) {
        TODO("Not yet implemented")
    }

    override fun addRange(first: PsiElement?, last: PsiElement?): PsiElement {
        TODO("Not yet implemented")
    }

    override fun addRangeBefore(first: PsiElement, last: PsiElement, anchor: PsiElement?): PsiElement {
        TODO("Not yet implemented")
    }

    override fun addRangeAfter(first: PsiElement?, last: PsiElement?, anchor: PsiElement?): PsiElement {
        TODO("Not yet implemented")
    }

    override fun delete() {
        TODO("Not yet implemented")
    }

    override fun checkDelete() {
        TODO("Not yet implemented")
    }

    override fun deleteChildRange(first: PsiElement?, last: PsiElement?) {
        TODO("Not yet implemented")
    }

    override fun replace(newElement: PsiElement): PsiElement {
        TODO("Not yet implemented")
    }

    override fun isValid(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isWritable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getReferences(): Array<PsiReference> {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getCopyableUserData(key: Key<T>): T? {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> putCopyableUserData(key: Key<T>, value: T?) {
        TODO("Not yet implemented")
    }

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun getContext(): PsiElement? {
        TODO("Not yet implemented")
    }

    override fun isPhysical(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getResolveScope(): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getUseScope(): SearchScope {
        TODO("Not yet implemented")
    }

    override fun getNode(): ASTNode {
        TODO("Not yet implemented")
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getName(): String? {
        TODO("Not yet implemented")
    }

    override fun getPresentation(): ItemPresentation? {
        TODO("Not yet implemented")
    }

    override fun getPsiOrParent(): CjElement {
        TODO("Not yet implemented")
    }

    override fun getContainingCjFile(): CjFile {
        TODO("Not yet implemented")
    }
}
