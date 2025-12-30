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
import org.cangnova.cangjie.cjo.NameResolveResult
import org.cangnova.cangjie.decompiler.COMPILED_DEFAULT_PARAMETER_VALUE
import org.cangnova.cangjie.metadata.model.fb.FbSemaTyInfo
import org.cangnova.cangjie.metadata.model.fb.FbTypeKind
import org.cangnova.cangjie.metadata.model.wrapper.TypeWrapper
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*

/**
 * 类型 Stub 构建器
 *
 * ## 架构作用
 *
 * TypeClsStubBuilder 是反编译系统的核心组件，负责将 Flatbuffers 元数据中的类型信息
 * 转换为 PSI Stub 树结构。它处理仓颉语言的所有类型系统特性，包括：
 *
 * - **基本类型**: Int32, Bool, Float64 等
 * - **用户类型**: class, interface, struct, enum, type alias
 * - **泛型类型**: 类型参数和类型参数列表
 * - **函数类型**: `(Int, String) -> Bool`
 * - **元组类型**: `(Int, String, Bool)`
 * - **数组类型**: `Array<T>`, `VArray<T>`
 * - **C 互操作类型**: `CPointer<T>`, `CString`
 *
 * ## 跨包引用解析
 *
 * 该构建器的关键功能是处理跨包类型引用。当类型来自其他包时：
 *
 * ```
 * // 类型: std.collection.ArrayList<Int>
 * 元数据中存储的 FullId: {packageIndex: 2, exportId: 15}
 *   ↓ (通过 CjoPackageService.resolveFullId)
 * 解析结果: {fqName: std.collection.ArrayList, needsQualification: true}
 *   ↓ (创建限定类型 Stub)
 * Stub 结构:
 *   CjUserType
 *     ├─ CjUserType (qualifier: std.collection)
 *     │   ├─ CjUserType (std)
 *     │   │   └─ NameRef("std")
 *     │   └─ NameRef("collection")
 *     ├─ NameRef("ArrayList")
 *     └─ TypeArgumentList
 *         └─ TypeProjection
 *             └─ BasicType("Int")
 * ```
 *
 * ## 类型解析流程
 *
 * ```
 * TypeWrapper (来自元数据)
 *   ↓
 * extractTypeInfo() - 识别类型种类
 *   ├─ 基本类型 → getPrimitiveTypeName()
 *   ├─ 用户类型 → resolveTypeFromDeclPtr() + CjoFullIdResolver
 *   └─ 泛型类型 → resolveTypeFromDeclPtr() + 递归处理类型参数
 *   ↓
 * NameResolveResult
 *   ├─ Success {fqName, name, needsQualification, packageFqName}
 *   └─ Failed
 *   ↓
 * 创建对应的 Stub
 *   ├─ needsQualification=true → createQualifiedUserTypeStub()
 *   └─ needsQualification=false → 简单 UserType
 * ```
 *
 * ## 错误降级处理
 *
 * 当类型解析失败时，使用降级策略保证反编译不中断：
 * - **本地类型** ([ClassId.isLocal]): 替换为 `AnyU`
 * - **解析失败** ([NameResolveResult.Failed]): 替换为 `Any`
 * - **未知类型**: 替换为 `Any`
 *
 * 这保证了即使在跨包依赖缺失的情况下，文件仍能被反编译和查看。
 *
 * ## 递归类型支持
 *
 * 类型可以嵌套引用自身或其他类型：
 * - **类型参数**: `class Box<T>` 中的 `T`
 * - **泛型类型**: `ArrayList<Box<Int>>`
 * - **函数类型**: `(Int) -> (String) -> Bool`
 *
 * 构建器通过递归调用自身处理嵌套类型。
 *
 * ## 使用场景
 *
 * 1. **变量/属性类型**: `val x: ArrayList<Int>`
 * 2. **函数参数/返回类型**: `func foo(x: String): Bool`
 * 3. **超类型**: `class Foo : Bar<Int>`
 * 4. **类型约束**: `where T: Comparable<T>`
 *
 * ## 性能优化
 *
 * - **延迟解析**: 只在需要时解析跨包引用
 * - **缓存结果**: CjoPackageService 缓存包信息
 * - **轻量级 Stub**: 只存储索引所需的最小信息
 *
 * @property parentStub 父 Stub 元素（通常是类型引用或参数）
 * @property context Stub 构建上下文（包含 CjoPackageService 等）
 *
 * @see ClsStubBuilderContext
 * @see TypeWrapper
 * @see NameResolveResult
 * @see CjoPackageService.resolveFullId
 */
class TypeClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val context: ClsStubBuilderContext
) {
    /**
     * 创建类型引用 Stub（入口方法）
     *
     * ## 功能说明
     *
     * 这是创建类型 Stub 的入口方法，为类型创建 [CjTypeReference] 包装器，
     * 然后委托给 [createTypeStub] 进行实际的类型 Stub 构建。
     *
     * ## PSI 结构
     *
     * 创建的结构为：
     * ```
     * CjTypeReference (占位符)
     *   └─ 具体类型 Stub (BasicType/UserType/FunctionType/...)
     * ```
     *
     * ## 使用场景
     *
     * - **变量声明**: `val x: Int`
     * - **函数参数**: `func foo(x: String)`
     * - **函数返回类型**: `func bar(): Bool`
     * - **超类型列表**: `class Foo : Bar`
     * - **类型约束**: `where T: Comparable`
     *
     * @param typeWrapper 类型包装器（来自元数据）
     *
     * @see CjTypeReference
     * @see createTypeStub
     */
    fun createTypeReferenceStub(typeWrapper: TypeWrapper) {
        val typeRefStub = CangJiePlaceHolderStubImpl<CjTypeReference>(
            parentStub,
            CjStubElementTypes.TYPE_REFERENCE
        )

        createTypeStub(typeRefStub, typeWrapper)
    }

    /**
     * 创建类型 Stub（核心分发方法）
     *
     * ## 功能说明
     *
     * 根据类型种类 ([FbTypeKind]) 分发到不同的 Stub 创建方法。
     * 这是类型 Stub 构建的核心分发器。
     *
     * ## 类型分类
     *
     * | 类型种类 | 处理方法 | 示例 |
     * |---------|---------|------|
     * | 基本类型 | [createBasicTypeStub] | Int32, Bool, Float64 |
     * | 用户类型 | [createUserTypeStub] | ArrayList, MyClass |
     * | 泛型类型 | [createGenericTypeStub] | T, U |
     * | 函数类型 | [createFunctionTypeStub] | (Int) -> Bool |
     * | 元组类型 | [createTupleTypeStub] | (Int, String) |
     * | 数组类型 | [createArrayTypeStub] | Array<T> |
     * | C 类型 | [createCPointerTypeStub] | CPointer<T> |
     * | 未知类型 | [createAnyTypeStub] | Any (降级) |
     *
     * ## 扩展支持
     *
     * 当添加新的类型种类时：
     * 1. 在 [FbTypeKind] 枚举中添加新值
     * 2. 在此方法的 when 表达式中添加分支
     * 3. 实现对应的 create*TypeStub 方法
     *
     * ## 错误处理
     *
     * 对于无法识别的类型种类，降级为 `Any` 类型以保证反编译不中断。
     *
     * @param parent 父 Stub（通常是 TypeReference）
     * @param typeWrapper 类型包装器
     *
     * @see FbTypeKind
     */
    private fun createTypeStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        val kind = typeWrapper.kind

        when (kind) {
            // 基本类型
            FbTypeKind.Unit, FbTypeKind.Nothing,
            FbTypeKind.Bool, FbTypeKind.Rune,
            FbTypeKind.Int8, FbTypeKind.Int16, FbTypeKind.Int32, FbTypeKind.Int64, FbTypeKind.IntNative,
            FbTypeKind.UInt8, FbTypeKind.UInt16, FbTypeKind.UInt32, FbTypeKind.UInt64, FbTypeKind.UIntNative,
            FbTypeKind.Float16, FbTypeKind.Float32, FbTypeKind.Float64 -> {
                createBasicTypeStub(parent, kind)
            }

            // 复合类型
            FbTypeKind.Class, FbTypeKind.Interface, FbTypeKind.Struct, FbTypeKind.Enum, FbTypeKind.Type -> {
                createUserTypeStub(parent, typeWrapper)
            }

            // 泛型类型
            FbTypeKind.Generic -> {
                createGenericTypeStub(parent, typeWrapper)
            }

            // 函数类型
            FbTypeKind.Func -> {
                createFunctionTypeStub(parent, typeWrapper)
            }

            // 元组类型
            FbTypeKind.Tuple -> {
                createTupleTypeStub(parent, typeWrapper)
            }

            // 数组类型
            FbTypeKind.Array, FbTypeKind.VArray -> {
                createArrayTypeStub(parent, typeWrapper)
            }

            // C 指针类型
            FbTypeKind.CPointer, FbTypeKind.CString -> {
                createCPointerTypeStub(parent, kind)
            }

            else -> {
                // 未知类型，创建一个 Any 类型作为回退
                createAnyTypeStub(parent)
            }
        }
    }

    /**
     * 创建基本类型 Stub
     *
     * ## 功能说明
     *
     * 为仓颉语言的基本类型创建 [CangJieBasicTypeStubImpl]。
     *
     * ## 支持的基本类型
     *
     * | 分类 | 类型 |
     * |------|------|
     * | 整数 | Int8, Int16, Int32, Int64, IntNative |
     * | 无符号整数 | UInt8, UInt16, UInt32, UInt64, UIntNative |
     * | 浮点数 | Float16, Float32, Float64 |
     * | 布尔 | Bool |
     * | 字符 | Rune |
     * | 特殊 | Unit, Nothing |
     *
     * ## 实现细节
     *
     * 通过 [getPrimitiveTypeName] 将 [FbTypeKind] 映射为类型名称字符串。
     * 如果映射失败（返回 `null`），则不创建任何 Stub。
     *
     * @param parent 父 Stub
     * @param kind 类型种类
     *
     * @see CangJieBasicTypeStubImpl
     * @see getPrimitiveTypeName
     */
    private fun createBasicTypeStub(parent: StubElement<out PsiElement>, kind: FbTypeKind) {
        val typeName = getPrimitiveTypeName(kind) ?: return
        CangJieBasicTypeStubImpl(parent, typeName)
    }

    /**
     * 创建用户类型 Stub（class, interface, struct, enum, type alias）
     *
     * 对于跨包引用的类型，会创建完整限定名的嵌套 UserType 结构。
     * 例如 std.collection.ArrayList 会创建：
     * ```
     * CjUserType (ArrayList)
     *   ├── CjUserType (qualifier: std.collection)
     *   │     ├── CjUserType (qualifier: std)
     *   │     │     └── CjNameReferenceExpression (std)
     *   │     └── CjNameReferenceExpression (collection)
     *   └── CjNameReferenceExpression (ArrayList)
     *   └── CjTypeArgumentList (if any)
     * ```
     */
    private fun createUserTypeStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        when (val result = extractTypeInfo(typeWrapper, context)) {
            is NameResolveResult.Success -> {
                if (result.needsQualification) {
                    // 跨包引用：创建带限定符的完整类型引用
                    createQualifiedUserTypeStub(parent, result.packageFqName, result.name, typeWrapper)
                } else {
                    // 当前包引用：创建简单类型引用
                    val userTypeStub = CangJieUserTypeStubImpl(parent)
                    CangJieNameReferenceExpressionStubImpl(userTypeStub, result.name.ref(), false)
                    // 处理类型参数
                    createTypeArgumentListStub(userTypeStub, typeWrapper)
                }
            }
            is NameResolveResult.Failed -> {
                createAnyTypeStub(parent)
            }
        }
    }

    /**
     * 创建带限定符的用户类型 Stub
     *
     * 递归创建嵌套的 UserType 结构来表示完整限定名。
     * 对于 std.collection.ArrayList，结构为：
     * ```
     * CjUserType (最外层)
     *   ├── CjUserType (qualifier: std.collection)
     *   │     ├── CjUserType (qualifier: std)
     *   │     │     └── CjNameReferenceExpression (std)
     *   │     └── CjNameReferenceExpression (collection)
     *   ├── CjNameReferenceExpression (ArrayList)
     *   └── CjTypeArgumentList (if any)
     * ```
     *
     * @param parent 父 Stub 元素
     * @param packageFqName 包的完全限定名
     * @param typeName 类型名称
     * @param typeWrapper 类型包装器（用于创建类型参数）
     */
    private fun createQualifiedUserTypeStub(
        parent: StubElement<out PsiElement>,
        packageFqName: FqName,
        typeName: Name,
        typeWrapper: TypeWrapper
    ) {
        // 创建最外层的 UserType（类型本身）
        val typeStub = CangJieUserTypeStubImpl(parent)

        // 创建包路径的限定符链（作为 qualifier）
        if (!packageFqName.isRoot) {
            val segments = packageFqName.pathSegments()
            if (segments.isNotEmpty()) {
                // 从最内层（第一个段）开始构建，然后逐层向外
                createNestedQualifier(typeStub, segments, segments.size - 1)
            }
        }

        // 创建类型名称引用
        CangJieNameReferenceExpressionStubImpl(typeStub, typeName.ref(), false)

        // 处理类型参数
        createTypeArgumentListStub(typeStub, typeWrapper)
    }

    /**
     * 递归创建嵌套的限定符 UserType
     *
     * 从 segments 的指定索引位置开始，递归创建嵌套的 UserType 结构。
     * 索引从后往前处理：
     * - 当 index = 0 时，创建最内层的 UserType（只有名称，没有 qualifier）
     * - 当 index > 0 时，先创建 qualifier（递归），再创建当前名称
     *
     * 例如对于 [std, collection]，index=1：
     * 1. 创建 UserType(collection)
     * 2. 在其中先递归创建 qualifier UserType(std)
     * 3. 再创建 NameRef(collection)
     *
     * @param parent 父 Stub（应该是一个 UserType）
     * @param segments 包路径段列表
     * @param index 当前处理的索引（从后往前）
     */
    private fun createNestedQualifier(
        parent: StubElement<out PsiElement>,
        segments: List<Name>,
        index: Int
    ) {
        if (index < 0) return

        // 创建当前级别的 UserType 作为 qualifier
        val qualifierStub = CangJieUserTypeStubImpl(parent)

        if (index > 0) {
            // 还有更内层的 qualifier，先递归创建
            createNestedQualifier(qualifierStub, segments, index - 1)
        }

        // 创建当前段的名称引用
        CangJieNameReferenceExpressionStubImpl(qualifierStub, segments[index].ref(), false)
    }

    /**
     * 创建泛型类型 Stub（类型参数引用）
     *
     * 泛型类型用于引用类型参数，例如在 `ArrayList<T>` 中，`T` 是泛型类型。
     * 使用 CjoFullIdResolver 处理跨包类型参数引用。
     */
    private fun createGenericTypeStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        when (val result = extractTypeInfo(typeWrapper, context)) {
            is NameResolveResult.Success -> {
                if (result.needsQualification) {
                    // 跨包引用：创建带限定符的完整类型引用
                    createQualifiedUserTypeStub(parent, result.packageFqName, result.name, typeWrapper)
                } else {
                    // 当前包引用：创建简单类型引用
                    val userTypeStub = CangJieUserTypeStubImpl(parent)
                    CangJieNameReferenceExpressionStubImpl(userTypeStub, result.name.ref(), false)
                    // 处理类型参数（泛型类型参数本身可能也有类型参数）
                    createTypeArgumentListStub(userTypeStub, typeWrapper)
                }
            }
            is NameResolveResult.Failed -> {
                createAnyTypeStub(parent)
            }
        }
    }

    /**
     * 创建函数类型 Stub
     *
     * ## 功能说明
     *
     * 创建函数类型的 Stub 表示，如 `(Int, String) -> Bool`。
     *
     * ## PSI 结构
     *
     * ```
     * CjFunctionType
     *   ├─ CjParameterList
     *   │   ├─ CjParameter
     *   │   │   └─ TypeReference (Int)
     *   │   └─ CjParameter
     *   │       └─ TypeReference (String)
     *   └─ TypeReference (Bool) - 返回类型
     * ```
     *
     * ## 元数据映射
     *
     * - **参数类型**: 存储在 [TypeWrapper.typeArgs] 中
     * - **返回类型**: 存储在 [FbFuncTyInfo.retType] 中
     *
     * ## 参数命名
     *
     * 函数类型的参数没有实际名称，统一使用 `_` 作为占位符。
     *
     * ## 递归处理
     *
     * 参数和返回类型通过递归调用 [TypeClsStubBuilder] 处理，
     * 支持嵌套的复杂类型如 `((Int) -> String) -> Bool`。
     *
     * @param parent 父 Stub
     * @param typeWrapper 函数类型包装器
     *
     * @see CjFunctionType
     * @see FbFuncTyInfo
     */
    private fun createFunctionTypeStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        val functionTypeStub = CangJiePlaceHolderStubImpl<CjFunctionType>(
            parent,
            CjStubElementTypes.FUNCTION_TYPE
        )

        val info = typeWrapper.info
        if (info is FbSemaTyInfo.FbFuncTyInfo) {
            // 创建参数类型列表
            val paramListStub = CangJiePlaceHolderStubImpl<CjParameterList>(
                functionTypeStub,
                CjStubElementTypes.VALUE_PARAMETER_LIST
            )

            // 参数类型在 typeArgs 中
            // 注意：匿名函数类型参数没有名称，与 CangJieParsing 中的解析保持一致
            val typeArgs = typeWrapper.typeArgs
            for (paramType in typeArgs) {
                val paramStub = CangJieParameterStubImpl(
                    paramListStub,
                    null,
                    null,  // 匿名参数没有名称
                    isMutable = false,
                    hasLetOrVar = false,
                    hasDefaultValue = false
                )
                TypeClsStubBuilder(paramStub, context).createTypeReferenceStub(paramType)
            }

            // 创建返回类型
            val retTypeWrapper = typeWrapper.typeTable[info.retType]
            val retTypeWrap = TypeWrapper(retTypeWrapper, typeWrapper.declTable, typeWrapper.typeTable)
            TypeClsStubBuilder(functionTypeStub, context).createTypeReferenceStub(retTypeWrap)
        }
    }

    /**
     * 创建元组类型 Stub
     *
     * ## 功能说明
     *
     * 创建元组类型的 Stub 表示，如 `(Int, String, Bool)`。
     *
     * ## PSI 结构
     *
     * ```
     * CjTupleType
     *   ├─ TypeReference (Int)
     *   ├─ TypeReference (String)
     *   └─ TypeReference (Bool)
     * ```
     *
     * ## 元数据映射
     *
     * 元组元素类型存储在 [TypeWrapper.typeArgs] 中，按顺序对应元组的各个位置。
     *
     * ## 特殊情况
     *
     * - **空元组** `()`: typeArgs 为空列表
     * - **单元素元组** `(Int,)`: typeArgs 只有一个元素
     * - **嵌套元组** `((Int, String), Bool)`: 递归处理
     *
     * ## 与函数类型的区别
     *
     * | 特性 | 元组类型 | 函数类型 |
     * |------|---------|---------|
     * | 语法 | `(Int, String)` | `(Int, String) -> Bool` |
     * | 用途 | 数据结构 | 可调用对象 |
     * | PSI | [CjTupleType] | [CjFunctionType] |
     *
     * @param parent 父 Stub
     * @param typeWrapper 元组类型包装器
     *
     * @see CjTupleType
     */
    private fun createTupleTypeStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        val tupleTypeStub = CangJiePlaceHolderStubImpl<CjTupleType>(
            parent,
            CjStubElementTypes.TUPLE_TYPE
        )

        // 元组元素类型在 typeArgs 中
        for (elementType in typeWrapper.typeArgs) {
            TypeClsStubBuilder(tupleTypeStub, context).createTypeReferenceStub(elementType)
        }
    }

    /**
     * 创建数组类型 Stub
     */
    private fun createArrayTypeStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        val typeName = if (typeWrapper.kind == FbTypeKind.VArray) "VArray" else "Array"
        val userTypeStub = CangJieUserTypeStubImpl(parent)
        CangJieNameReferenceExpressionStubImpl(userTypeStub, Name.identifier(typeName).ref(), false)

        // 处理数组元素类型
        createTypeArgumentListStub(userTypeStub, typeWrapper)
    }

    /**
     * 创建 C 指针类型 Stub
     */
    private fun createCPointerTypeStub(parent: StubElement<out PsiElement>, kind: FbTypeKind) {
        val typeName = if (kind == FbTypeKind.CPointer) "CPointer" else "CString"
        val userTypeStub = CangJieUserTypeStubImpl(parent)
        CangJieNameReferenceExpressionStubImpl(userTypeStub, Name.identifier(typeName).ref(), false)
    }

    /**
     * 创建 Any 类型 Stub 作为回退
     */
    private fun createAnyTypeStub(parent: StubElement<out PsiElement>) {
        val userTypeStub = CangJieUserTypeStubImpl(parent)
        CangJieNameReferenceExpressionStubImpl(userTypeStub, Name.identifier("Any").ref(), false)
    }

    /**
     * 创建类型参数列表 Stub
     */
    private fun createTypeArgumentListStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        val typeArgs = typeWrapper.typeArgs
        if (typeArgs.isEmpty()) return

        val typeArgListStub = CangJiePlaceHolderStubImpl<CjTypeArgumentList>(
            parent,
            CjStubElementTypes.TYPE_ARGUMENT_LIST
        )

        for (typeArg in typeArgs) {
            val typeProjectionStub = CangJieTypeProjectionStubImpl(typeArgListStub, 0)
            TypeClsStubBuilder(typeProjectionStub, context).createTypeReferenceStub(typeArg)
        }
    }

    companion object {
        /**
         * 从类型包装器中提取类型信息（核心解析方法）
         *
         * ## 功能说明
         *
         * 这是类型解析的核心方法，负责将 Flatbuffers 元数据中的类型信息
         * 转换为 [NameResolveResult]，包含类型的完全限定名和是否需要限定的信息。
         *
         * ## 解析策略
         *
         * 根据 [FbSemaTyInfo] 的类型采取不同策略：
         *
         * ### 1. 复合类型 ([FbCompositeTyInfo])
         *
         * 通过 [resolveTypeFromDeclPtr] 解析 `declPtr` (FullId)：
         * ```
         * class Foo { ... }  // declPtr = {packageIndex: 0, exportId: 5}
         * ```
         *
         * ### 2. 泛型类型 ([FbGenericTyInfo])
         *
         * 同样通过 [resolveTypeFromDeclPtr] 解析类型参数的声明：
         * ```
         * class Box<T> { ... }  // T 的 declPtr = {packageIndex: 0, exportId: 10}
         * ```
         *
         * ### 3. 基本类型
         *
         * 通过 [getPrimitiveTypeName] 直接映射类型名，无需跨包解析：
         * ```kotlin
         * FbTypeKind.Int32 → "Int32"
         * FbTypeKind.Bool → "Bool"
         * ```
         *
         * ## 跨包引用处理
         *
         * 当类型来自其他包时，[CjoPackageService.resolveFullId] 会：
         * 1. 根据 `packageIndex` 找到导入包
         * 2. 根据 `exportId` 在包的导出表中查找声明
         * 3. 返回包含完整路径的 [NameResolveResult.Success]
         *
         * ### 示例：跨包类型解析
         *
         * ```
         * // 当前包: com.example.app
         * // 类型引用: std.collection.ArrayList<Int>
         *
         * FullId: {packageIndex: 2, exportId: 15}
         *   ↓ (查找 importedPackages[2])
         * Package: std.collection
         *   ↓ (查找 exportTable[15])
         * Declaration: ArrayList
         *   ↓
         * Result: NameResolveResult.Success(
         *   fqName = std.collection.ArrayList,
         *   name = ArrayList,
         *   needsQualification = true,
         *   packageFqName = std.collection
         * )
         * ```
         *
         * ## 当前包引用
         *
         * 当类型在当前包中时，`needsQualification = false`：
         * ```
         * // 当前包: com.example.app
         * // 类型引用: MyClass (在同一包中)
         *
         * Result: NameResolveResult.Success(
         *   fqName = com.example.app.MyClass,
         *   name = MyClass,
         *   needsQualification = false,  // 不需要限定
         *   packageFqName = com.example.app
         * )
         * ```
         *
         * ## 解析失败处理
         *
         * 当解析失败时返回 [NameResolveResult.Failed]，调用者会：
         * 1. 创建 `Any` 类型作为降级
         * 2. 记录警告日志
         * 3. 继续反编译其他部分
         *
         * ## 性能考虑
         *
         * - **缓存**: CjoPackageService 缓存包信息，避免重复加载
         * - **延迟加载**: 只在需要时加载导入包的元数据
         * - **早期返回**: 基本类型直接映射，无需复杂解析
         *
         * ## 使用场景
         *
         * 1. **Stub 构建**: 在 [createUserTypeStub] 和 [createGenericTypeStub] 中调用
         * 2. **类型渲染**: 在 [buildDecompiledText] 中用于生成反编译文本
         * 3. **类型检查**: 在分析器中验证类型引用的有效性
         *
         * @param typeWrapper 类型包装器（包含类型种类和信息）
         * @param context Stub 构建上下文（包含 CjoPackageService）
         * @return 类型解析结果（成功或失败）
         *
         * @see NameResolveResult
         * @see CjoPackageService.resolveFullId
         * @see resolveTypeFromDeclPtr
         * @see getPrimitiveTypeName
         */
        fun extractTypeInfo(typeWrapper: TypeWrapper, context: ClsStubBuilderContext): NameResolveResult {
            val info = typeWrapper.info
            return when (info) {
                is FbSemaTyInfo.FbCompositeTyInfo -> {
                    resolveTypeFromDeclPtr(info.declPtr, context)
                }
                is FbSemaTyInfo.FbGenericTyInfo -> {
                    resolveTypeFromDeclPtr(info.declPtr, context)
                }
                else -> {
                    // 尝试从 kind 获取基本类型名称
                    getPrimitiveTypeName(typeWrapper.kind)?.let { typeName ->
                        // 基本类型不需要限定
                        val name = Name.identifier(typeName)
                        NameResolveResult.Success(
                            fqName = FqName.ROOT.child(name),
                            name = name,
                            needsQualification = false,
                            packageFqName = FqName.ROOT,
                            declIndex = -1
                        )
                    } ?: NameResolveResult.Failed
                }
            }
        }

        /**
         * 从 FullId (declPtr) 中解析类型信息
         *
         * 使用 CjoFullIdResolver 完整处理 FullId 的查找逻辑：
         * - 当前包：通过 index 从 declTable 查找
         * - 其他包：通过 index/exportId 从服务查找
         */
        private fun resolveTypeFromDeclPtr(
            declPtr: org.cangnova.cangjie.metadata.model.fb.FbFullId?,
            context: ClsStubBuilderContext
        ): NameResolveResult {
            if (declPtr == null) return NameResolveResult.Failed

            return context.components.packageService.resolveFullId(
                declPtr,
                context.components.packageWrapper
            )
        }

        /**
         * 向后兼容的方法：只返回名称
         */
        fun extractTypeName(typeWrapper: TypeWrapper, context: ClsStubBuilderContext): Name? {
            return when (val result = extractTypeInfo(typeWrapper, context)) {
                is NameResolveResult.Success -> result.name
                is NameResolveResult.Failed -> null
            }
        }

        /**
         * 获取基本类型名称
         */
        private fun getPrimitiveTypeName(kind: FbTypeKind): String? {
            return when (kind) {
                FbTypeKind.Unit -> "Unit"
                FbTypeKind.Nothing -> "Nothing"
                FbTypeKind.Bool -> "Bool"
                FbTypeKind.Rune -> "Rune"
                FbTypeKind.Int8 -> "Int8"
                FbTypeKind.Int16 -> "Int16"
                FbTypeKind.Int32 -> "Int32"
                FbTypeKind.Int64 -> "Int64"
                FbTypeKind.IntNative -> "IntNative"
                FbTypeKind.UInt8 -> "UInt8"
                FbTypeKind.UInt16 -> "UInt16"
                FbTypeKind.UInt32 -> "UInt32"
                FbTypeKind.UInt64 -> "UInt64"
                FbTypeKind.UIntNative -> "UIntNative"
                FbTypeKind.Float16 -> "Float16"
                FbTypeKind.Float32 -> "Float32"
                FbTypeKind.Float64 -> "Float64"
                else -> null
            }
        }
    }
}
