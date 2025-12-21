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
import org.cangnova.cangjie.metadata.model.fb.FbSemaTyInfo
import org.cangnova.cangjie.metadata.model.fb.FbTypeKind
import org.cangnova.cangjie.metadata.model.wrapper.TypeWrapper
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*

const val COMPILED_DEFAULT_PARAMETER_VALUE = "COMPILED_CODE"

/**
 * 类型 Stub 构建器，用于从 Flatbuffers 元数据构建类型相关的 Stub
 */
class TypeClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val context: ClsStubBuilderContext
) {
    /**
     * 创建类型引用 Stub
     */
    fun createTypeReferenceStub(typeWrapper: TypeWrapper) {
        val typeRefStub = CangJiePlaceHolderStubImpl<CjTypeReference>(
            parentStub,
            CjStubElementTypes.TYPE_REFERENCE
        )

        createTypeStub(typeRefStub, typeWrapper)
    }

    /**
     * 创建类型 Stub
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
            val typeArgs = typeWrapper.typeArgs
            for (paramType in typeArgs) {
                val paramStub = CangJieParameterStubImpl(
                    paramListStub,
                    null,
                    Name.identifier("_").ref(),
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
         * 从类型包装器中提取类型信息
         *
         * 使用 CjoFullIdResolver 处理完整的 FullId 查找逻辑，包括跨包引用。
         * 返回解析结果而不仅仅是名称，以便调用者知道是否需要限定。
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
