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

package cn.cangnova.cangjie.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.*
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory
import cn.cangnova.cangjie.CjNodeTypes
import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.lang.CangJieLanguage
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjFile.Companion.FILE_DECLARATION_TYPES
import cn.cangnova.cangjie.psi.stubs.CangJieFileStub
import cn.cangnova.cangjie.psi.stubs.elements.CjPlaceHolderStubElementType
import cn.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import cn.cangnova.cangjie.psi.stubs.elements.CjTokenSets

interface CangJieFile
abstract class CjCommonFile (viewProvider: FileViewProvider, val isCompiled: Boolean): PsiFileBase(viewProvider, CangJieLanguage),

    PsiNamedElement,

    CjDeclarationContainer,
    CjElement, CangJieFile{
    override fun getFileType(): FileType = CangJieFileType.INSTANCE

    @Volatile
    private var pathCached: String? = null




    @Volatile
    private var hasTopLevelCallables: Boolean? = null

    fun hasTopLevelCallables(): Boolean {
        hasTopLevelCallables?.let { return it }

        val result = declarations.any {
            (it is CjVariable ||
                    it is CjNamedFunction ||

                    it is CjTypeAlias)/* && !it.hasExpectModifier()*/
        }

        hasTopLevelCallables = result
        return result
    }

    val virtualFilePath
        get(): String {
            pathCached?.let { return it }

            return virtualFile.path.also {
                pathCached = it
            }
        }

    fun findAliasByFqName(fqName: FqName): CjImportAlias? {
        if (!hasImportAlias()) return null

        return importDirectivesItem.firstOrNull {

            it .alias != null && fqName == it.importedFqName
        }?.alias
    }
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCjCommonFile(this)
    }
    override fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D) {
        CjPsiUtil.visitChildren(this, visitor, data)
    }
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            accept(visitor, null)
        } else {
            visitor.visitFile(this)
        }
    }

    override fun getParent(): PsiDirectory? {
        return super.getParent()
    }

    protected open val importLists: List<CjImportList>
        get() = findChildrenByTypeOrClass(CjStubElementTypes.IMPORT_LIST, CjImportList::class.java).asList()

    private fun hasImportAlias(): Boolean {
        val hasImportAlias = hasImportAlias
        if (hasImportAlias != null) return hasImportAlias

        val newValue = importLists.any(CjImportList::computeHasImportAlias)
        this.hasImportAlias = newValue
        return newValue

    }

    override fun toString(): String {
        return "CangJie File: $name"
    }


    override fun getPsiOrParent(): CjElement = this
    open val importDirectivesItem: List<CjImportDirectiveItem>
        get() = importLists.flatMap { it.importItems }
    open val importDirectives: List<CjImportDirective>
        get() = importLists.flatMap { it.imports}


    override fun getContainingCjFile(): CjFile = this as CjFile

    fun findImportByAlias(name: String): CjImportDirectiveItem? {
        if (!hasImportAlias()) return null

        return importDirectivesItem.firstOrNull { name == it.aliasName }
    }

    private fun <T : CjElementImplStub<out StubElement<*>>> findChildrenByTypeOrClass(
        elementType: CjPlaceHolderStubElementType<T>,
        elementClass: Class<T>
    ): Array<out T> {
        val stub = stub
        if (stub != null) {
            val arrayFactory: ArrayFactory<T> = elementType.arrayFactory
            return stub.getChildrenByType(elementType, arrayFactory)
        }
        return findChildrenByClass(elementClass)
    }

    val packageFqNameByTree: FqName
        get() = packageDirectiveByTree?.fqName ?: FqName.ROOT


    private val packageDirectiveByTree: CjPackageDirective?
        get() {
            val ast = node.findChildByType(CjNodeTypes.PACKAGE_DIRECTIVE)
            return if (ast != null) ast.psi as CjPackageDirective else null
        }

    override fun getStub(): CangJieFileStub? {
        if (virtualFile !is VirtualFileWithId) return null
        val stub = super.getStub()
        if (stub is CangJieFileStub?) {
            return stub
        }

        error("Illegal stub for CjFile: type=${this.javaClass}, stub=${stub!!.javaClass} name=$name")
    }

    val packageDirective: CjPackageDirective?
        get() {
            val stub = stub
            if (stub != null) {
                val packageDirectiveStub = stub.findChildStubByType(CjStubElementTypes.PACKAGE_DIRECTIVE)
                return packageDirectiveStub?.psi
            }
            return packageDirectiveByTree
        }


    @Volatile
    private var hasImportAlias: Boolean? = null



    override val declarations: List<CjDeclaration>
        get() {
            val stub = stub
            return stub?.getChildrenByType(FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
                ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java).toMutableList().apply {
//                remove所有PACKAGE_DIRECTIVE
                    removeAll { it is CjPackageDirective }

                }
        }
    var packageFqName: FqName
        get() = stub?.getPackageFqName() ?: packageFqNameByTree
        set(value) {
            val packageDirective = packageDirective
            if (packageDirective != null) {
                packageDirective.fqName = value
            } else {
                val newPackageDirective = CjPsiFactory(project).createPackageDirectiveIfNeeded(value) ?: return
                addAfter(newPackageDirective, null)
            }
        }

    val importList: CjImportList?
        get() = importLists.firstOrNull()

    }
open class CjFile(viewProvider: FileViewProvider,   isCompiled: Boolean = false,val isCodeFragment: Boolean = false) :
    CjCommonFile(viewProvider, isCompiled) {


    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R =
        visitor.visitCjFile(this, data)

    companion object {
        val FILE_DECLARATION_TYPES = TokenSet.orSet(CjTokenSets.DECLARATION_TYPES, TokenSet.create(CjStubElementTypes.CJ_SCRIPT))

    }
}

private fun CjImportList.computeHasImportAlias(): Boolean {
    var child: PsiElement? = firstChild
    while (child != null) {
        if (child is CjImportDirectiveItem && child.alias != null) {
            return true
        }

        child = child.nextSibling
    }

    return false
}

