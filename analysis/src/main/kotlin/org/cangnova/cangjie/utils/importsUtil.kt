/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.utils

import cn.cangnova.cangjie.imports.CangJieImportPathComparator
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getLineCount
import org.cangnova.cangjie.psi.psiUtil.isMultiLine
import org.cangnova.cangjie.psi.psiUtil.nextLeaf
import org.cangnova.cangjie.resolve.DescriptorUtils
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiWhiteSpace
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.ImportPath
import org.cangnova.cangjie.psi.psiUtil.getLineCount
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.getReferenceTargets
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.types.CangJieType

/**
 * 获取分类器（类或类型别名）的所有构造器
 *
 * 仓颉语言中，类和类型别名都可以有构造器：
 * - 类构造器：类本身定义的构造器
 * - 类型别名构造器：类型别名展开后的类型的构造器
 *
 * @receiver ClassifierDescriptor 分类器描述符（类或类型别名）
 * @return Collection<ConstructorDescriptor> 所有构造器的集合
 */
fun ClassifierDescriptor.getConstructors(): Collection<ConstructorDescriptor> = when (this) {
    is ClassDescriptor -> constructors
    is TypeAliasDescriptor -> constructors
    else -> emptyList()
}

/**
 * 判断类型是否可以通过 import 语句引用
 *
 * 只有可导入的类型才能通过 import 语句引入到当前作用域。
 * 可导入的类型包括：
 * - 顶层声明的类型
 * - 嵌套在可导入类型中的公共类型
 *
 * @receiver CangJieType 要检查的类型
 * @return Boolean true 表示可以通过 import 引用，false 表示不可以
 */
fun CangJieType.canBeReferencedViaImport(): Boolean {
    val descriptor = constructor.declarationDescriptor
    return descriptor != null && descriptor.canBeReferencedViaImport()
}

/**
 * 获取声明的可导入全限定名
 *
 * 如果声明可以通过 import 语句引用，返回其全限定名；否则返回 null。
 * 对于某些特殊描述符（如构造器），返回其所属类的全限定名。
 *
 * @receiver DeclarationDescriptor 声明描述符
 * @return FqName? 可导入的全限定名，如果不可导入则返回 null
 */
val DeclarationDescriptor.importableFqName: FqName?
    get() {
        if (!canBeReferencedViaImport()) return null
        return getImportableDescriptor().fqNameSafe
    }


/**
 * 获取引用表达式的所有可导入目标
 *
 * 引用表达式可能引用多个声明（例如重载函数）。
 * 此函数返回所有这些声明的可导入形式。
 *
 * 特殊处理：
 * - 如果引用指向伴生对象，返回伴生对象本身
 * - 对于构造器引用，返回其所属类
 *
 * @receiver CjReferenceExpression 引用表达式
 * @param bindingContext 绑定上下文，包含引用的解析信息
 * @return Collection<DeclarationDescriptor> 所有可导入目标的集合（去重后）
 */
fun CjReferenceExpression.getImportableTargets(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
    val targets = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, this]?.let { listOf(it) }
        ?: getReferenceTargets(bindingContext)
    return targets.map { it.getImportableDescriptor() }.toSet()
}

/**
 * 判断导入路径是否已被导入（排除特定的全限定名）
 *
 * 此函数检查导入路径是否已在现有的导入列表中，并且不在排除列表中。
 * 主要用于去重和避免导入不必要的符号。
 *
 * @receiver ImportPath 要检查的导入路径
 * @param imports 已存在的导入路径列表
 * @param excludedFqNames 要排除的全限定名列表（这些名称即使已导入也视为未导入）
 * @return Boolean true 表示已导入且不在排除列表中，false 表示未导入或在排除列表中
 */
fun ImportPath.isImported(imports: Iterable<ImportPath>, excludedFqNames: Iterable<FqName>): Boolean {
    return isImported(imports) && (isAllUnder || this.fqName !in excludedFqNames)
}

/**
 * 判断导入路径是否在导入列表中
 *
 * @receiver ImportPath 要检查的导入路径
 * @param imports 已存在的导入路径列表
 * @return Boolean true 表示已导入，false 表示未导入
 */
private fun ImportPath.isImported(imports: Iterable<ImportPath>): Boolean = imports.any { isImported(it) }

/**
 * 判断导入路径是否与已导入的路径匹配
 *
 * 匹配规则：
 * - 如果当前导入是通配符导入（`import foo.*`）或有别名，则必须完全相同
 * - 如果当前导入是具体导入（`import foo.Bar`），则检查全限定名是否被已导入的路径覆盖
 *
 * @receiver ImportPath 要检查的导入路径
 * @param alreadyImported 已存在的导入路径
 * @return Boolean true 表示已被覆盖，false 表示未被覆盖
 */
fun ImportPath.isImported(alreadyImported: ImportPath): Boolean {
    return if (isAllUnder || hasAlias()) this == alreadyImported else fqName.isImported(alreadyImported)
}

/**
 * 判断全限定名是否被导入路径覆盖
 *
 * 覆盖规则：
 * - 如果 [skipAliasedImports] 为 true，则忽略带别名的导入
 * - 如果导入路径是通配符导入（`import foo.*`），则检查全限定名的父包是否匹配
 * - 如果导入路径是具体导入（`import foo.Bar`），则检查全限定名是否完全匹配
 *
 * @receiver FqName 要检查的全限定名
 * @param importPath 导入路径
 * @param skipAliasedImports 是否跳过带别名的导入（默认为 true）
 * @return Boolean true 表示已被覆盖，false 表示未被覆盖
 */
fun FqName.isImported(importPath: ImportPath, skipAliasedImports: Boolean = true): Boolean {
    return when {
        skipAliasedImports && importPath.hasAlias() -> false
        importPath.isAllUnder && !isRoot -> importPath.fqName == this.parent()
        else -> importPath.fqName == this
    }
}


/**
 * 向仓颉文件添加 import 导入语句
 *
 * 此函数智能地向文件添加导入语句，处理多种情况：
 * - 如果导入已存在，返回已存在的导入指令
 * - 如果导入不存在，创建新的导入指令并按字母顺序插入
 * - 自动处理包声明和导入列表之间的空行
 * - 处理代码片段（CjCodeFragment）的特殊情况
 *
 * 导入格式：
 * - 普通导入：`import com.example.Foo`
 * - 通配符导入：`import com.example.*`
 * - 别名导入：`import com.example.Foo as Bar`
 * - 包分割导入：导入包的父包而不是完整路径
 *
 * @receiver CjFile 要添加导入的仓颉文件
 * @param fqName 要导入的全限定名
 * @param allUnder 是否为通配符导入（`.*`），默认为 false
 * @param alias 导入别名，null 表示不使用别名
 * @param project 项目实例，默认使用当前文件所在的项目
 * @param isPackageSplit 是否为包分割导入，true 表示导入父包，默认为 false
 * @return CjImportDirective 新添加的导入指令，或已存在的导入指令
 * @throws IllegalStateException 如果文件没有导入列表
 */
fun CjFile.addImport(
    fqName: FqName,
    allUnder: Boolean = false,
    alias: Name? = null,
    project: Project = this.project,
    isPackageSplit: Boolean = false
): CjImportDirective {
    val importPath = ImportPath(if (isPackageSplit) fqName.parent() else fqName, allUnder, alias)

    val psiFactory = CjPsiFactory(project)
    if (this is CjCodeFragment) {
        // 代码片段的特殊处理：直接添加导入文本
        val newDirective = psiFactory.createImportDirective(importPath)
        addImportsFromString(newDirective.text)
        return newDirective
    }

    val importList = importList
    if (importList != null) {
        val newDirective = psiFactory.createImportDirective(importPath)
        val imports = importList.imports
        return if (imports.isEmpty()) {
            // 如果没有现有导入，处理与包声明之间的空行
            val packageDirective = packageDirective?.takeIf { it.packageKeyword != null }
            packageDirective?.let {
                val elemAfterPkg = packageDirective.nextSibling
                val linesAfterPkg = elemAfterPkg.getLineCount() - 1
                val missingLines = 2 - linesAfterPkg
                // 确保包声明和导入之间至少有两个换行
                if (missingLines > 0) addAfter(psiFactory.createNewLine(missingLines), it)
            }

            (importList.add(newDirective) as CjImportDirective).also {
                if (packageDirective == null) {
                    // 如果没有包声明，在导入后添加两个换行
                    val whiteSpace = importList.nextLeaf(true)
                    if (whiteSpace is PsiWhiteSpace) {
                        val newLineBreak = if (whiteSpace.isMultiLine()) {
                            psiFactory.createWhiteSpace("\n" + whiteSpace.text)
                        } else {
                            psiFactory.createWhiteSpace("\n\n" + whiteSpace.text)
                        }

                        whiteSpace.replace(newLineBreak)
                    } else {
                        addAfter(psiFactory.createNewLine(2), importList)
                    }
                }
            }
        } else {

            // 如果已有导入，按字母顺序插入新导入
            val importPathComparator = CangJieImportPathComparator.create(this)
            val insertAfter = imports.lastOrNull {
                val directivePath = it.firstImportPath
                directivePath != null && importPathComparator.compare(directivePath, importPath) <= 0
            }

            // 如果导入已存在，直接返回已存在的导入
            if (insertAfter is CjImportDirective && newDirective.firstImportPath == insertAfter.firstImportPath) return insertAfter

            (importList.addAfter(newDirective, insertAfter) as CjImportDirective ).also {
                importList.addBefore(psiFactory.createNewLine(1), it)
            }
        }
    } else {
        error("Trying to insert import $fqName into a file $name of type ${this::class.java} with no import list.")
    }
}


/**
 * 判断声明是否可以通过 import 语句引用
 *
 * 仓颉语言的导入规则：
 * - 顶层声明（类、函数、变量等）可以导入
 * - 嵌套在类中的类、类型别名可以导入
 * - 构造器可以通过导入其所属类来使用
 * - 包视图（PackageViewDescriptor）可以导入
 * - 特殊名称（编译器生成的名称）不可导入
 *
 * 导入层级检查：
 * - 如果声明嵌套在类中，父类必须可导入
 * - 递归检查直到顶层声明
 *
 * @receiver DeclarationDescriptor 声明描述符
 * @return Boolean true 表示可以通过 import 引用，false 表示不可以
 */
fun DeclarationDescriptor.canBeReferencedViaImport(): Boolean {
    if (this is PackageViewDescriptor ||
        DescriptorUtils.isTopLevelDeclaration(this) /*||
        this is CallableDescriptor && DescriptorUtils.isStaticDeclaration(this)*/
    ) {
        return !name.isSpecial
    }

    // 检查嵌套声明的可导入性
    // 适用于 TypeAliasDescriptor 和 ClassDescriptor
    val parentClassifier = containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: return false
    if (!parentClassifier.canBeReferencedViaImport()) return false

    return when (this) {
        is ConstructorDescriptor -> true // 构造器可以通过导入其所属类来使用
        is ClassDescriptor, is TypeAliasDescriptor -> true // 嵌套类和类型别名可以导入
        else -> parentClassifier is ClassDescriptor
    }
}

/**
 * 获取声明的可导入形式
 *
 * 某些声明不能直接导入，需要转换为其可导入的形式：
 * - 类型别名派生的描述符 → 原始类型别名
 * - 构造器 → 其所属的类
 * - 其他声明 → 原样返回
 *
 * 注意：仓颉语言没有属性访问器的概念，因此不需要处理 PropertyAccessorDescriptor。
 *
 * @receiver DeclarationDescriptor 声明描述符
 * @return DeclarationDescriptor 可导入的描述符形式
 */
fun DeclarationDescriptor.getImportableDescriptor(): DeclarationDescriptor =
    when (this) {
        is DescriptorDerivedFromTypeAlias -> typeAliasDescriptor
        is ConstructorDescriptor -> containingDeclaration
        // 注释：仓颉没有属性访问器的概念，不需要处理 PropertyAccessorDescriptor
        // is PropertyAccessorDescriptor -> correspondingProperty
        else -> this
    }



