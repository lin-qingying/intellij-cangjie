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

import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.lang.CangJieLanguage
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjFile.Companion.FILE_DECLARATION_TYPES
import cn.cangnova.cangjie.psi.CjNodeTypes
import cn.cangnova.cangjie.psi.stubs.CangJieFileStub
import cn.cangnova.cangjie.psi.stubs.elements.CjPlaceHolderStubElementType
import cn.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import cn.cangnova.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.*
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory

/**
 * 仓颉语言文件的基础接口。
 * 此接口作为PSI树中所有仓颉文件的标记接口。
 */
interface CangJieFile

/**
 * 仓颉文件的抽象基类，实现了通用功能。
 * 此类为IntelliJ PSI结构中的仓颉语言文件提供核心实现。
 *
 * @param viewProvider 此文件的文件视图提供者
 * @param isCompiled 此文件是否是编译文件（例如，来自库）或源文件
 */
abstract class CjCommonFile(viewProvider: FileViewProvider, val isCompiled: Boolean) :
    PsiFileBase(viewProvider, CangJieLanguage),
    PsiNamedElement,
    CjDeclarationContainer,
    CjElement,
    CangJieFile {
    /**
     * 返回此文件的文件类型。
     * @return 仓颉文件类型实例
     */
    override fun getFileType(): FileType = CangJieFileType.INSTANCE

    /**
     * 虚拟文件路径的缓存，避免重复计算。
     */
    @Volatile
    private var pathCached: String? = null

    /**
     * 缓存标志，指示此文件是否有顶级可调用元素。
     */
    @Volatile
    private var hasTopLevelCallables: Boolean? = null

    /**
     * 确定此文件是否包含任何顶级可调用声明。
     * 顶级可调用包括变量、命名函数和类型别名。
     *
     * @return 如果文件包含顶级可调用，则为true，否则为false
     */
    fun hasTopLevelCallables(): Boolean {
        hasTopLevelCallables?.let { return it }

        val result = declarations.any {
            (
                it is CjVariable ||
                    it is CjNamedFunction ||
                    it is CjTypeAlias
                ) /* && !it.hasExpectModifier()*/
        }

        hasTopLevelCallables = result
        return result
    }

    /**
     * 返回虚拟文件的路径，使用缓存以提高性能。
     * @return 虚拟文件的路径
     */
    val virtualFilePath
        get(): String {
            pathCached?.let { return it }

            return virtualFile.path.also {
                pathCached = it
            }
        }

    /**
     * 通过完全限定名查找导入别名。
     *
     * @param fqName 要搜索的完全限定名
     * @return 如果找到导入别名，则返回该别名，否则返回null
     */
    fun findAliasByFqName(fqName: FqName): CjImportAlias? {
        if (!hasImportAlias()) return null

        return importDirectivesItem.firstOrNull {
            it.alias != null && fqName == it.importedFqName
        }?.alias
    }
    
    /**
     * 接受此文件的访问者。访问者模式实现的一部分。
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的附加数据
     * @return 访问者访问的结果
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCjCommonFile(this)
    }
    
    /**
     * 接受此文件所有子元素的访问者。
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的附加数据
     */
    override fun <D> acceptChildren(visitor: CjVisitor<Void, D>, data: D) {
        CjPsiUtil.visitChildren(this, visitor, data)
    }
    
    /**
     * 接受PSI元素访问者。如果适用，委托给CjVisitor。
     *
     * @param visitor 要接受的访问者
     */
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            accept(visitor, null)
        } else {
            visitor.visitFile(this)
        }
    }

    /**
     * 返回此文件的父目录。
     * @return 父目录
     */
    override fun getParent(): PsiDirectory? {
        return super.getParent()
    }

    /**
     * 返回此文件中的所有导入列表。
     * @return 导入列表的列表
     */
    protected open val importLists: List<CjImportList>
        get() = findChildrenByTypeOrClass(CjStubElementTypes.IMPORT_LIST, CjImportList::class.java).asList()

    /**
     * 确定此文件是否有任何导入别名。
     * 使用缓存以提高性能。
     *
     * @return 如果文件有导入别名，则为true，否则为false
     */
    internal fun hasImportAlias(): Boolean {
        val hasImportAlias = hasImportAlias
        if (hasImportAlias != null) return hasImportAlias

        val newValue = importLists.any(CjImportList::computeHasImportAlias)
        this.hasImportAlias = newValue
        return newValue
    }

    /**
     * 返回此文件的字符串表示。
     * @return 字符串表示
     */
    override fun toString(): String {
        return "CangJie File: $name"
    }

    /**
     * 将此文件作为CjElement返回。
     * @return 此文件
     */
    override fun getPsiOrParent(): CjElement = this
    
    /**
     * 返回此文件中的所有导入指令项。
     * @return 导入指令项的列表
     */
    open val importDirectivesItem: List<CjImportDirectiveItem>
        get() = importLists.flatMap { it.importItems }
    
    /**
     * 返回此文件中的所有导入指令。
     * @return 导入指令的列表
     */
    open val importDirectives: List<CjImportDirective>
        get() = importLists.flatMap { it.imports }

    /**
     * 将此文件作为CjFile返回。
     * @return 转换为CjFile的此文件
     */
    override fun getContainingCjFile(): CjFile = this as CjFile

    /**
     * 通过别名名称查找导入指令项。
     *
     * @param name 要搜索的别名名称
     * @return 如果找到导入指令项，则返回该项，否则返回null
     */
    fun findImportByAlias(name: String): CjImportDirectiveItem? {
        if (!hasImportAlias()) return null

        return importDirectivesItem.firstOrNull { name == it.aliasName }
    }

    /**
     * 查找特定类型或类的子元素。
     * 如果可用，则使用存根以提高性能。
     *
     * @param elementType 要搜索的元素类型
     * @param elementClass 要搜索的元素类
     * @return 匹配元素的数组
     */
    private fun <T : CjElementImplStub<out StubElement<*>>> findChildrenByTypeOrClass(
        elementType: CjPlaceHolderStubElementType<T>,
        elementClass: Class<T>,
    ): Array<out T> {
        val stub = stub
        if (stub != null) {
            val arrayFactory: ArrayFactory<T> = elementType.arrayFactory
            return stub.getChildrenByType(elementType, arrayFactory)
        }
        return findChildrenByClass(elementClass)
    }

    /**
     * 通过遍历PSI树返回完全限定的包名。
     * @return 完全限定的包名
     */
    val packageFqNameByTree: FqName
        get() = packageDirectiveByTree?.fqName ?: FqName.ROOT

    /**
     * 通过遍历PSI树返回包指令。
     * @return 如果找到包指令，则返回该指令，否则返回null
     */
    private val packageDirectiveByTree: CjPackageDirective?
        get() {
            val ast = node.findChildByType(CjNodeTypes.PACKAGE_DIRECTIVE)
            return if (ast != null) ast.psi as CjPackageDirective else null
        }

    /**
     * 返回此文件的存根。
     * @return 如果可用，则返回存根，否则返回null
     * @throws IllegalStateException 如果存根类型不正确
     */
    override fun getStub(): CangJieFileStub? {
        if (virtualFile !is VirtualFileWithId) return null
        val stub = super.getStub()
        if (stub is CangJieFileStub?) {
            return stub
        }

        error("Illegal stub for CjFile: type=${this.javaClass}, stub=${stub!!.javaClass} name=$name")
    }

    /**
     * 返回此文件的包指令。
     * 如果可用，则使用存根以提高性能。
     *
     * @return 如果找到包指令，则返回该指令，否则返回null
     */
    val packageDirective: CjPackageDirective?
        get() {
            val stub = stub
            if (stub != null) {
                val packageDirectiveStub = stub.findChildStubByType(CjStubElementTypes.PACKAGE_DIRECTIVE)
                return packageDirectiveStub?.psi
            }
            return packageDirectiveByTree
        }

    /**
     * 缓存标志，指示此文件是否有导入别名。
     */
    @Volatile
    private var hasImportAlias: Boolean? = null

    /**
     * 返回此文件中的所有声明。
     * 如果可用，则使用存根以提高性能。
     *
     * @return 声明的列表
     */
    override val declarations: List<CjDeclaration>
        get() {
            val stub = stub
            return stub?.getChildrenByType(FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
                ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java).toMutableList().apply {
                    // 移除所有包指令
                    removeAll { it is CjPackageDirective }
                }
        }
    
    /**
     * 获取或设置此文件的完全限定包名。
     * 设置时，根据需要更新或创建包指令。
     */
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

    /**
     * 返回此文件中的第一个导入列表。
     * @return 如果可用，则返回第一个导入列表，否则返回null
     */
    val importList: CjImportList?
        get() = importLists.firstOrNull()
}

/**
 * 仓颉文件的具体实现。
 * 此类表示仓颉语言中的文件。
 *
 * @param viewProvider 此文件的文件视图提供者
 * @param isCompiled 此文件是否是编译文件或源文件
 * @param isCodeFragment 此文件是否表示代码片段（例如，在草稿文件中）
 */
open class CjFile(viewProvider: FileViewProvider, isCompiled: Boolean = false, val isCodeFragment: Boolean = false) :
    CjCommonFile(viewProvider, isCompiled) {

    /**
     * 接受此文件的访问者。访问者模式实现的一部分。
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的附加数据
     * @return 访问者访问的结果
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R =
        visitor.visitCjFile(this, data)

    companion object {
        /**
         * 定义可以出现在文件级别的声明类型的标记集。
         * 将标准声明类型与仓颉脚本声明结合起来。
         */
        val FILE_DECLARATION_TYPES = TokenSet.orSet(CjTokenSets.DECLARATION_TYPES, TokenSet.create(CjStubElementTypes.CJ_SCRIPT))
    }
}

/**
 * 辅助函数，用于确定导入列表是否包含任何导入别名。
 * 遍历导入列表的子元素，查找具有别名的任何导入指令项。
 *
 * @return 如果导入列表包含任何导入别名，则为true，否则为false
 */
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
