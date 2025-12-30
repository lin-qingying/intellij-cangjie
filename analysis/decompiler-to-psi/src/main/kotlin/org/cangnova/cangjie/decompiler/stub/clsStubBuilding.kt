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

package org.cangnova.cangjie.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.DescriptorVisibilities
import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.metadata.model.wrapper.AnnotationWrapper
import org.cangnova.cangjie.metadata.model.wrapper.FunctionWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PropertyWrapper
import org.cangnova.cangjie.metadata.model.wrapper.VariableWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.CangJieUserTypeStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*

/**
 * Stub 构建工具函数集
 *
 * 该文件包含用于构建 PSI Stub 的核心工具函数。这些函数负责：
 * - 创建文件级 Stub（包声明、导入列表）
 * - 创建修饰符 Stub（可见性、模态）
 * - 创建类型名称 Stub（支持泛型和嵌套类型）
 * - 创建声明 Stub（函数、属性、变量）
 *
 * ## 设计原则
 *
 * 1. **轻量级**: Stub 只包含索引所需的最小信息
 * 2. **递归构建**: 支持嵌套结构（如嵌套类、限定类型名）
 * 3. **延迟加载**: 避免在 Stub 构建时加载完整的 PSI 树
 * 4. **缓存友好**: 使用 StringRef 减少内存占用
 *
 * @see ClsStubBuilderContext
 * @see CangJieFileStubImpl
 */

/**
 * 计算参数的显示名称
 *
 * ## 功能说明
 *
 * 将编译器内部的特殊参数名转换为用户可见的名称：
 * - 隐式 setter 参数 → `value`
 * - 匿名参数（`_1`, `_2`） → `_`
 * - 普通参数 → 保持原名
 *
 * ## 使用场景
 *
 * 1. **属性 setter**: 将 `IMPLICIT_SET_PARAMETER` 转换为 `value`
 *    ```kotlin
 *    var x: Int
 *        set(value) { ... } // 'value' 是隐式参数名
 *    ```
 *
 * 2. **Lambda 参数**: 匿名参数显示为 `_`
 *    ```kotlin
 *    list.map { _ -> 42 } // 忽略参数
 *    ```
 *
 * 3. **普通函数参数**: 直接使用声明的名称
 *
 * ## 实现细节
 *
 * 使用 [SpecialNames] 识别编译器生成的特殊名称：
 * - `<set-?>`: 隐式 setter 参数
 * - `_\d+`: 匿名参数模式
 *
 * @param name 元数据中的参数名
 * @return 用户可见的参数名
 *
 * @see SpecialNames.IMPLICIT_SET_PARAMETER
 * @see SpecialNames.isAnonymousParameterName
 * @see StandardNames.DEFAULT_VALUE_PARAMETER
 */
fun computeParameterName(name: Name): Name {
    return when {
        name == SpecialNames.IMPLICIT_SET_PARAMETER -> StandardNames.DEFAULT_VALUE_PARAMETER
        SpecialNames.isAnonymousParameterName(name) -> Name.identifier("_")
        else -> name
    }
}

/**
 * 创建不兼容 ABI 版本的文件 Stub
 *
 * ## 功能说明
 *
 * 当元数据文件的 ABI（Application Binary Interface）版本与当前编译器版本不兼容时，
 * 创建一个空的占位符 Stub，防止解析错误导致 IDE 崩溃。
 *
 * ## 使用场景
 *
 * 1. **版本不匹配**: 旧版本编译的 .cjo 文件与新版本 IDE 不兼容
 * 2. **格式错误**: 元数据文件损坏或格式无法识别
 * 3. **降级支持**: 允许在不完全支持的情况下仍然加载项目
 *
 * ## 实现细节
 *
 * 创建一个根包（[FqName.ROOT]）的空文件 Stub：
 * - 不包含任何声明
 * - 不会被索引或补全系统使用
 * - 允许文件在编辑器中打开并显示错误消息
 *
 * ## 反编译文本
 *
 * 配合 [createIncompatibleMetadataVersionDecompiledText] 使用，
 * 在编辑器中显示版本不兼容的错误提示。
 *
 * @return 空的文件 Stub
 *
 * @see CangJieMetadataStubBuilder.FileWithMetadata.Incompatible
 * @see createIncompatibleMetadataVersionDecompiledText
 */
fun createIncompatibleAbiVersionFileStub(): CangJieFileStubImpl = createFileStub(FqName.ROOT)

/**
 * 创建文件 Stub
 *
 * ## 功能说明
 *
 * 创建 PSI 文件 Stub 的根节点，并设置包声明和导入列表的占位符。
 *
 * ## Stub 结构
 *
 * ```
 * CangJieFileStub (packageFqName)
 *   ├─ CangJiePackageDirectiveStub
 *   │   └─ PackageName (递归的 DotQualifiedExpression)
 *   └─ CangJiePlaceHolderStub<CjImportList>
 * ```
 *
 * ## 工作流程
 *
 * 1. 创建 [CangJieFileStubImpl] 作为根节点
 * 2. 调用 [setupFileStub] 创建包声明和导入列表
 * 3. 返回完整的文件 Stub
 *
 * ## 与 PSI 的关系
 *
 * - 文件 Stub 是整个 Stub 树的根
 * - 所有顶层声明（类、函数、变量）都是其子节点
 * - 对应 [CjFile] 的 Stub 表示
 *
 * @param packageFqName 包的完全限定名
 * @return 文件 Stub
 *
 * @see setupFileStub
 * @see CangJieFileStubImpl.forFile
 */
fun createFileStub(packageFqName: FqName): CangJieFileStubImpl {
    val fileStub = CangJieFileStubImpl.forFile(packageFqName)
    setupFileStub(fileStub, packageFqName)
    return fileStub
}

/**
 * 设置文件 Stub 的包声明和导入列表
 *
 * ## 功能说明
 *
 * 在文件 Stub 下创建两个子 Stub：
 * 1. **包声明 Stub**: 表示 `package x.y.z`
 * 2. **导入列表 Stub**: 空的占位符（反编译文件没有显式导入）
 *
 * ## 包名 Stub 结构
 *
 * 对于包名 `com.example.app`，创建的 Stub 结构为：
 * ```
 * CangJiePackageDirectiveStub
 *   └─ DotQualifiedExpression ("com.example")
 *       ├─ DotQualifiedExpression ("com")
 *       │   └─ NameReferenceExpression ("com")
 *       └─ NameReferenceExpression ("example")
 *   └─ NameReferenceExpression ("app")
 * ```
 *
 * ## 导入列表
 *
 * 创建空的 [CjImportList] 占位符 Stub，因为：
 * - 反编译文件不需要显式导入（已通过 FullId 解析）
 * - PSI 结构要求文件必须有导入列表节点
 * - 占位符不占用额外内存
 *
 * @param fileStub 文件 Stub 根节点
 * @param packageFqName 包的完全限定名
 *
 * @see createStubForPackageName
 * @see CangJiePackageDirectiveStubImpl
 */
private fun setupFileStub(fileStub: CangJieFileStubImpl, packageFqName: FqName) {
    // 创建正确的 CangJiePackageDirectiveStub 实例，而不是使用占位符 Stub
    val packageDirectiveStub = CangJiePackageDirectiveStubImpl(fileStub)
    createStubForPackageName(packageDirectiveStub, packageFqName)
    CangJiePlaceHolderStubImpl<CjImportList>(fileStub, CjStubElementTypes.IMPORT_LIST)
}

/**
 * 递归创建包名 Stub
 *
 * ## 功能说明
 *
 * 将包的完全限定名（如 `com.example.app`）转换为嵌套的 PSI Stub 结构。
 *
 * ## 算法说明
 *
 * 使用**逆序递归**构建包名链：
 * 1. 从最后一个段开始（`app`）
 * 2. 为前缀创建 [CjDotQualifiedExpression]（`com.example`）
 * 3. 递归处理前缀，直到第一个段
 *
 * ## Stub 结构示例
 *
 * 输入: `com.example.app`
 *
 * 输出结构:
 * ```
 * packageDirectiveStub
 *   └─ DotQualifiedExpression
 *       ├─ receiver (DotQualifiedExpression)
 *       │   ├─ receiver (NameReferenceExpression "com")
 *       │   └─ selector (NameReferenceExpression "example")
 *       └─ selector (NameReferenceExpression "app")
 * ```
 *
 * ## 特殊情况
 *
 * - **根包** ([FqName.ROOT]): 不创建任何子 Stub
 * - **单段包名** (`app`): 只创建一个 [CangJieNameReferenceExpressionStubImpl]
 * - **多段包名**: 递归创建嵌套的 [CjDotQualifiedExpression]
 *
 * ## 性能考虑
 *
 * - 使用 [StringRef] 避免重复存储字符串
 * - 递归深度受包名长度限制（通常 < 10）
 * - 尾递归优化可能由 Kotlin 编译器应用
 *
 * @param packageDirectiveStub 包声明 Stub
 * @param packageFqName 包的完全限定名
 *
 * @see CangJiePackageDirectiveStubImpl
 * @see CjDotQualifiedExpression
 * @see CangJieNameReferenceExpressionStubImpl
 */
fun createStubForPackageName(packageDirectiveStub: StubElement<out PsiElement>, packageFqName: FqName) {
    val segments = packageFqName.pathSegments()
    val iterator = segments.listIterator(segments.size)

    fun recCreateStubForPackageName(current: StubElement<out PsiElement>) {
        when (iterator.previousIndex()) {
            -1 -> return
            0 -> {
                CangJieNameReferenceExpressionStubImpl(current, iterator.previous().ref())
                return
            }
            else -> {
                val lastSegment = iterator.previous()
                val receiver = CangJiePlaceHolderStubImpl<CjDotQualifiedExpression>(
                    current,
                    CjStubElementTypes.DOT_QUALIFIED_EXPRESSION
                )
                recCreateStubForPackageName(receiver)
                CangJieNameReferenceExpressionStubImpl(receiver, lastSegment.ref())
            }
        }
    }

    recCreateStubForPackageName(packageDirectiveStub)
}

/**
 * [Name] 转 [StringRef] 扩展函数
 *
 * ## 功能说明
 *
 * 将 [Name] 对象转换为 [StringRef]，用于 Stub 索引中的字符串存储。
 *
 * ## StringRef 优势
 *
 * 1. **内存共享**: 相同字符串在内存中只存储一份
 * 2. **快速比较**: 使用整数 ID 进行相等性检查
 * 3. **序列化友好**: 支持高效的缓存序列化
 *
 * ## 使用场景
 *
 * - 存储类名、函数名、参数名
 * - 构建包名 Stub
 * - 创建类型引用 Stub
 *
 * ## 注意事项
 *
 * - 返回值不会为 `null`（使用 `!!` 操作符）
 * - 假设所有 [Name] 都有有效的字符串表示
 *
 * @receiver Name 对象
 * @return 对应的 StringRef
 *
 * @see StringRef
 * @see Name.asString
 */
fun Name.ref(): StringRef = StringRef.fromString(this.asString())!!

/**
 * [FqName] 转 [StringRef] 扩展函数
 *
 * ## 功能说明
 *
 * 将完全限定名转换为 [StringRef]，用于文件 Stub 中的包名存储。
 *
 * ## 示例
 *
 * ```kotlin
 * val packageName = FqName("com.example.app")
 * val ref = packageName.ref() // StringRef("com.example.app")
 * ```
 *
 * @receiver FqName 对象
 * @return 对应的 StringRef
 *
 * @see StringRef
 * @see FqName.asString
 */
fun FqName.ref(): StringRef = StringRef.fromString(this.asString())!!

/**
 * 创建修饰符列表 Stub
 *
 * ## 功能说明
 *
 * 根据给定的修饰符集合创建 [CangJieModifierListStubImpl]。
 *
 * ## 修饰符表示
 *
 * 修饰符使用位掩码存储，通过 [ModifierMaskUtils.computeMask] 计算：
 * - 每个修饰符对应一个二进制位
 * - 使用整数压缩存储，节省内存
 * - 支持快速的修饰符查询（位运算）
 *
 * ## 空修饰符处理
 *
 * 如果 [modifiers] 为空，返回 `null`：
 * - 避免创建不必要的 Stub 节点
 * - 减少内存占用
 * - 简化 PSI 树结构
 *
 * ## 使用场景
 *
 * 直接创建修饰符 Stub，适用于：
 * - 自定义修饰符组合
 * - 特殊声明（如注解）
 * - 临时修饰符创建
 *
 * ## 对比
 *
 * - [createModifierListStub]: 返回 null 或 Stub
 * - [createEmptyModifierListStub]: 总是返回空 Stub
 * - [createModifierListStubForDeclaration]: 基于可见性和模态创建
 *
 * @param parent 父 Stub 元素
 * @param modifiers 修饰符关键字集合
 * @return 修饰符列表 Stub，如果为空则返回 `null`
 *
 * @see CangJieModifierListStubImpl
 * @see ModifierMaskUtils
 * @see CjModifierKeywordToken
 */
fun createModifierListStub(
    parent: StubElement<out PsiElement>,
    modifiers: Collection<CjModifierKeywordToken>
): CangJieModifierListStubImpl? {
    if (modifiers.isEmpty()) {
        return null
    }
    return CangJieModifierListStubImpl(
        parent,
        ModifierMaskUtils.computeMask { it in modifiers },
        CjStubElementTypes.MODIFIER_LIST
    )
}

/**
 * 创建空修饰符列表 Stub
 *
 * ## 功能说明
 *
 * 创建一个不包含任何修饰符的修饰符列表 Stub。
 *
 * ## 与 null 的区别
 *
 * | 情况 | 返回值 | 场景 |
 * |------|--------|------|
 * | [createModifierListStub] 空集合 | `null` | 可选的修饰符列表 |
 * | [createEmptyModifierListStub] | 空 Stub | 必须的修饰符列表节点 |
 *
 * ## 使用场景
 *
 * 1. **PSI 结构要求**: 某些声明必须有修饰符列表节点（即使为空）
 * 2. **占位符**: 为后续修饰符添加预留位置
 * 3. **默认可见性**: 表示使用默认可见性的声明
 *
 * ## 实现细节
 *
 * 修饰符掩码为 0（所有位都是 false）：
 * ```kotlin
 * ModifierMaskUtils.computeMask { false } // 0000...0000
 * ```
 *
 * @param parent 父 Stub 元素
 * @return 空的修饰符列表 Stub
 *
 * @see CangJieModifierListStubImpl
 * @see createModifierListStub
 */
fun createEmptyModifierListStub(parent: CangJieStubBaseImpl<*>): CangJieModifierListStubImpl {
    return CangJieModifierListStubImpl(
        parent,
        ModifierMaskUtils.computeMask { false },
        CjStubElementTypes.MODIFIER_LIST
    )
}

/**
 * 根据可见性和模态创建修饰符列表 Stub
 *
 * ## 功能说明
 *
 * 为类、函数、属性等声明创建标准的修饰符列表，基于：
 * 1. **可见性** ([DescriptorVisibility])
 * 2. **模态** ([Modality])
 * 3. **额外修饰符** (如 `static`, `const`, `suspend` 等)
 *
 * ## 可见性映射
 *
 * | Descriptor Visibility | CangJie Keyword |
 * |-----------------------|-----------------|
 * | PUBLIC | `public` |
 * | PRIVATE | `private` |
 * | PROTECTED | `protected` |
 * | INTERNAL | `internal` |
 *
 * ## 模态映射
 *
 * | Modality | CangJie Keyword |
 * |----------|-----------------|
 * | ABSTRACT | `abstract` |
 * | OPEN | `open` |
 * | SEALED | `sealed` |
 * | FINAL | (无修饰符，默认) |
 *
 * ## 修饰符组合示例
 *
 * ```kotlin
 * // public open class Foo
 * createModifierListStubForDeclaration(
 *     parent,
 *     DescriptorVisibilities.PUBLIC,
 *     Modality.OPEN
 * )
 *
 * // private suspend fun bar()
 * createModifierListStubForDeclaration(
 *     parent,
 *     DescriptorVisibilities.PRIVATE,
 *     null,
 *     listOf(CjTokens.SUSPEND_KEYWORD)
 * )
 * ```
 *
 * ## 空修饰符处理
 *
 * 如果所有修饰符都不存在，仍然返回空的修饰符列表 Stub（而不是 `null`），
 * 因为声明必须有修饰符列表节点。
 *
 * ## 性能优化
 *
 * - 使用可变列表逐步添加修饰符
 * - 使用位掩码压缩存储
 * - 避免创建中间集合
 *
 * @param parent 父 Stub 元素
 * @param visibility 可见性描述符
 * @param modality 模态（可为 `null` 表示 FINAL）
 * @param additionalModifiers 额外的修饰符列表
 * @return 修饰符列表 Stub
 *
 * @see DescriptorVisibility
 * @see Modality
 * @see CjModifierKeywordToken
 */
fun createModifierListStubForDeclaration(
    parent: StubElement<out PsiElement>,
    visibility: DescriptorVisibility,
    modality: Modality?,
    additionalModifiers: List<CjModifierKeywordToken> = emptyList()
): CangJieModifierListStubImpl {
    val modifiers = mutableListOf<CjModifierKeywordToken>()

    // 添加可见性修饰符
    when (visibility) {
        DescriptorVisibilities.PUBLIC -> modifiers.add(CjTokens.PUBLIC_KEYWORD)
        DescriptorVisibilities.PRIVATE -> modifiers.add(CjTokens.PRIVATE_KEYWORD)
        DescriptorVisibilities.PROTECTED -> modifiers.add(CjTokens.PROTECTED_KEYWORD)
        DescriptorVisibilities.INTERNAL -> modifiers.add(CjTokens.INTERNAL_KEYWORD)
    }

    // 添加模态修饰符
    when (modality) {
        Modality.ABSTRACT -> modifiers.add(CjTokens.ABSTRACT_KEYWORD)
        Modality.OPEN -> modifiers.add(CjTokens.OPEN_KEYWORD)
        Modality.SEALED -> modifiers.add(CjTokens.SEALED_KEYWORD)
        else -> {}
    }

    modifiers.addAll(additionalModifiers)

    return createModifierListStub(parent, modifiers)
        ?: CangJieModifierListStubImpl(
            parent,
            ModifierMaskUtils.computeMask { false },
            CjStubElementTypes.MODIFIER_LIST
        )
}

/**
 * 创建注解列表 Stub
 *
 * ## 功能说明
 *
 * 为声明创建注解列表 Stub，保持与源码 PSI 结构的一致性。
 * 注解是声明的直接子元素，位于修饰符列表之前。
 *
 * ## PSI 结构
 *
 * ```
 * CjClass (或其他声明)
 *   ├─ CangJiePlaceHolderStub<CjAnnotations>  <-- 注解在修饰符列表之前
 *   │   ├─ CangJieAnnotationStub ("Deprecated")
 *   │   └─ CangJieAnnotationStub ("Frozen")
 *   ├─ CangJieModifierListStub
 *   └─ ... 其他子元素
 * ```
 *
 * ## 使用场景
 *
 * 在创建类、函数、属性等声明的 Stub 时调用，确保反编译代码的注解信息
 * 可以在 IDE 中正确显示和索引。
 *
 * **重要**: 必须在创建修饰符列表之前调用此函数，以保持正确的 PSI 结构。
 *
 * @param parent 父 Stub 元素（声明 Stub）
 * @param annotations 注解包装器列表
 *
 * @see CangJieAnnotationStubImpl
 * @see AnnotationWrapper
 */
fun createAnnotationsStub(
    parent: StubElement<out PsiElement>,
    annotations: List<AnnotationWrapper>
) {
    val annotationsStub = CangJiePlaceHolderStubImpl<CjAnnotations>(
        parent,
        CjStubElementTypes.ANNOTATIONS
    )

    for (annotation in annotations) {
        val annotationStub = CangJieAnnotationStubImpl(
            annotationsStub,
            annotation.name.ref(),
            hasValueArguments = false  // TODO: 从元数据中提取参数信息
        )

        // 创建 CONSTRUCTOR_CALLEE 子节点，与 PSI 解析器结构保持一致
        // PSI 结构: ANNOTATION -> CONSTRUCTOR_CALLEE -> TYPE_REFERENCE -> USER_TYPE -> REFERENCE_EXPRESSION
        val constructorCalleeStub = CangJiePlaceHolderStubImpl<CjConstructorCalleeExpression>(
            annotationStub,
            CjStubElementTypes.CONSTRUCTOR_CALLEE
        )

        val typeRefStub = CangJiePlaceHolderStubImpl<CjTypeReference>(
            constructorCalleeStub,
            CjStubElementTypes.TYPE_REFERENCE
        )

        val userTypeStub = CangJieUserTypeStubImpl(typeRefStub)
        CangJieNameReferenceExpressionStubImpl(userTypeStub, annotation.name.ref(), true)
    }
}

/**
 * 创建类型名称 Stub
 *
 * ## 功能说明
 *
 * 根据 [ClassId] 递归创建类型名称的 Stub 结构，支持：
 * 1. **简单类型**: `Int`, `String`
 * 2. **限定类型**: `std.collection.ArrayList`
 * 3. **嵌套类型**: `Outer.Inner`
 * 4. **泛型类型**: 通过 [bindTypeArguments] 回调绑定类型参数
 *
 * ## Stub 结构
 *
 * 对于类型 `std.collection.ArrayList<Int>`：
 * ```
 * CangJieUserTypeStub
 *   ├─ CangJieUserTypeStub ("std.collection")
 *   │   ├─ CangJieUserTypeStub ("std")
 *   │   │   └─ NameReferenceExpression ("std", isClassifier=false)
 *   │   └─ NameReferenceExpression ("collection", isClassifier=false)
 *   ├─ NameReferenceExpression ("ArrayList", isClassifier=true)
 *   └─ TypeArgumentList
 *       └─ TypeProjection
 *           └─ UserType ("Int")
 * ```
 *
 * ## 本地类替换
 *
 * 如果 [ClassId.isLocal] 为 `true`（匿名类、lambda 类），则：
 * - 使用 `AnyU` 类型替代（防止无法解析）
 * - 不绑定类型参数
 * - 保证反编译文本的可读性
 *
 * ## 类型参数绑定
 *
 * [bindTypeArguments] 回调在每个类型层级被调用：
 * - `level = 0`: 最内层类型（如 `ArrayList`）
 * - `level = 1`: 外层类型（如 `collection`）
 * - `level = 2`: 最外层（如 `std`）
 *
 * 调用者负责创建类型参数 Stub：
 * ```kotlin
 * createStubForTypeName(classId, parent) { userTypeStub, level ->
 *     if (level == 0 && typeArguments.isNotEmpty()) {
 *         // 创建 TypeArgumentList Stub
 *     }
 * }
 * ```
 *
 * ## 递归构建
 *
 * 使用**逆序递归**构建类型链：
 * 1. 从包名开始（非分类器）
 * 2. 逐步添加限定符
 * 3. 最后添加类名（分类器）
 *
 * ## 性能考虑
 *
 * - 使用 [StringRef] 缓存类型名
 * - 递归深度通常 < 5
 * - 避免在 Stub 构建时解析类型依赖
 *
 * @param typeClassId 类型的 ClassId
 * @param parent 父 Stub 元素
 * @param bindTypeArguments 类型参数绑定回调 (userTypeStub, level) -> Unit
 * @return 类型名称 Stub
 *
 * @see CangJieUserTypeStub
 * @see ClassId
 * @see StandardNames.FqNames.anyUFqName
 */
fun createStubForTypeName(
    typeClassId: ClassId,
    parent: StubElement<out PsiElement>,
    bindTypeArguments: (CangJieUserTypeStub, Int) -> Unit = { _, _ -> }
): CangJieUserTypeStub {
    val substituteWithAny = typeClassId.isLocal

    val fqName = if (substituteWithAny) StandardNames.FqNames.anyUFqName
    else typeClassId.asSingleFqName().toUnsafe()

    val segments = fqName.pathSegments().asReversed()
    assert(segments.isNotEmpty())
    val classesNestedLevel = segments.size - if (substituteWithAny) 1 else typeClassId.packageFqName.pathSegments().size

    fun recCreateStubForType(current: StubElement<out PsiElement>, level: Int): CangJieUserTypeStub {
        val lastSegment = segments[level]
        val userTypeStub = CangJieUserTypeStubImpl(current)
        if (level + 1 < segments.size) {
            recCreateStubForType(userTypeStub, level + 1)
        }
        CangJieNameReferenceExpressionStubImpl(userTypeStub, lastSegment.ref(), level < classesNestedLevel)
        if (!substituteWithAny) {
            bindTypeArguments(userTypeStub, level)
        }
        return userTypeStub
    }

    return recCreateStubForType(parent, level = 0)
}

/**
 * 创建声明 Stubs（函数和属性）
 *
 * ## 功能说明
 *
 * 为类或对象的成员（函数和属性）批量创建 Stub。
 *
 * ## 创建顺序
 *
 * 1. **属性优先**: 先创建所有属性 Stub
 * 2. **函数其次**: 再创建所有函数 Stub
 *
 * 这个顺序与源码的书写习惯一致，但不影响功能。
 *
 * ## 函数类型识别
 *
 * 根据 [FunctionWrapper] 的属性自动选择合适的 Stub Builder：
 * - `isMainEntry = true` → [MainFunctionClsStubBuilder]
 * - `isMacro = true` → [MacroClsStubBuilder]
 * - 默认 → [FunctionClsStubBuilder]
 *
 * ## 元数据容器
 *
 * [MetadataContainer] 提供构建上下文：
 * - **Package**: 顶层声明
 * - **Class**: 类成员
 *
 * ## 使用场景
 *
 * - 类成员创建: 在 [ClassClsStubBuilder] 中调用
 * - 对象成员创建: 在 object 声明处理中调用
 * - 接口成员创建: 在 interface 声明处理中调用
 *
 * ## 与包级声明的区别
 *
 * | 功能 | [createDeclarationsStubs] | [createPackageDeclarationsStubs] |
 * |------|--------------------------|----------------------------------|
 * | 属性/变量 | [PropertyWrapper] | [VariableWrapper] |
 * | 容器类型 | Class/Object | Package |
 * | 典型调用 | 类体内 | 文件顶层 |
 *
 * @param parentStub 父 Stub（通常是类体 Stub）
 * @param outerContext Stub 构建上下文
 * @param metadataContainer 元数据容器（类或包）
 * @param functionWrappers 函数包装器列表
 * @param propertyWrappers 属性包装器列表
 *
 * @see PropertyClsStubBuilder
 * @see FunctionClsStubBuilder
 * @see MainFunctionClsStubBuilder
 * @see MacroClsStubBuilder
 */
fun createDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    metadataContainer: MetadataContainer,
    functionWrappers: List<FunctionWrapper>,
    propertyWrappers: List<PropertyWrapper>,
) {
    for (propertyWrapper in propertyWrappers) {
        PropertyClsStubBuilder(parentStub, outerContext, metadataContainer, propertyWrapper).build()
    }
    for (functionWrapper in functionWrappers) {
        // 根据函数类型创建相应的 stub builder
        val builder = when {
            functionWrapper.isMainEntry -> MainFunctionClsStubBuilder(
                parentStub,
                outerContext,
                metadataContainer,
                functionWrapper
            )
            functionWrapper.isMacro -> MacroClsStubBuilder(
                parentStub,
                outerContext,
                metadataContainer,
                functionWrapper
            )
            else -> FunctionClsStubBuilder(
                parentStub,
                outerContext,
                metadataContainer,
                functionWrapper
            )
        }
        builder.build()
    }
}

/**
 * 创建包声明 Stubs
 *
 * ## 功能说明
 *
 * 为包的顶层声明（函数和变量）批量创建 Stub。
 *
 * ## 与类成员的区别
 *
 * | 特性 | 包级声明 | 类成员 |
 * |------|---------|--------|
 * | 变量类型 | [VariableWrapper] | [PropertyWrapper] |
 * | 是否有 getter/setter | 否 | 是 |
 * | 是否有可见性 | 是 | 是 |
 * | 容器 | [MetadataContainer.Package] | [MetadataContainer.Class] |
 *
 * ## 函数类型识别
 *
 * 与 [createDeclarationsStubs] 相同，支持：
 * - 普通函数
 * - Main 函数
 * - 宏函数
 *
 * ## 创建顺序
 *
 * 1. **函数优先**: 先创建所有函数 Stub
 * 2. **变量其次**: 再创建所有变量 Stub
 *
 * 这个顺序与包级文件的典型组织方式一致。
 *
 * ## 使用场景
 *
 * - 文件 Stub 构建: 在 [CangJieMetadataStubBuilder.buildCompatibleFileStub] 中调用
 * - 包级声明索引
 * - 顶层函数和变量查找
 *
 * ## 元数据容器
 *
 * 必须是 [MetadataContainer.Package] 类型，包含：
 * - 包的完全限定名
 * - 类型表（用于类型解析）
 *
 * @param parentStub 父 Stub（通常是文件 Stub）
 * @param outerContext Stub 构建上下文
 * @param metadataContainer 包元数据容器
 * @param functionWrappers 函数包装器列表
 * @param variableWrappers 变量包装器列表
 *
 * @see VariableClsStubBuilder
 * @see FunctionClsStubBuilder
 * @see MainFunctionClsStubBuilder
 * @see MacroClsStubBuilder
 * @see CangJieMetadataStubBuilder.buildCompatibleFileStub
 */
fun createPackageDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    metadataContainer: MetadataContainer.Package,
    functionWrappers: List<FunctionWrapper>,
    variableWrappers: List<VariableWrapper>
) {
    for (functionWrapper in functionWrappers) {
        // 根据函数类型创建相应的 stub builder
        val builder = when {
            functionWrapper.isMainEntry -> MainFunctionClsStubBuilder(
                parentStub,
                outerContext,
                metadataContainer,
                functionWrapper
            )
            functionWrapper.isMacro -> MacroClsStubBuilder(
                parentStub,
                outerContext,
                metadataContainer,
                functionWrapper
            )
            else -> FunctionClsStubBuilder(
                parentStub,
                outerContext,
                metadataContainer,
                functionWrapper
            )
        }
        builder.build()
    }
    for (variableWrapper in variableWrappers) {
        VariableClsStubBuilder(parentStub, outerContext, metadataContainer, variableWrapper).build()
    }
}