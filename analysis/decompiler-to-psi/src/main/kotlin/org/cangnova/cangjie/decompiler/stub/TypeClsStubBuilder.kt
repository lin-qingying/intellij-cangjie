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
import org.cangnova.cangjie.metadata.model.fb.FbSemaTyInfo
import org.cangnova.cangjie.metadata.model.fb.FbTypeKind
import org.cangnova.cangjie.metadata.model.wrapper.TypeWrapper
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
     */
    private fun createUserTypeStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        val typeName = extractTypeName(typeWrapper)
        if (typeName != null) {
            val userTypeStub = CangJieUserTypeStubImpl(parent)
            CangJieNameReferenceExpressionStubImpl(userTypeStub, typeName.ref(), false)

            // 处理类型参数
            createTypeArgumentListStub(userTypeStub, typeWrapper)
        } else {
            createAnyTypeStub(parent)
        }
    }

    /**
     * 创建泛型类型 Stub
     */
    private fun createGenericTypeStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        val info = typeWrapper.info
        if (info is FbSemaTyInfo.FbGenericTyInfo) {
            val declPtr = info.declPtr
            if (declPtr != null) {
                val typeName = Name.identifier(declPtr.decl)
                val userTypeStub = CangJieUserTypeStubImpl(parent)
                CangJieNameReferenceExpressionStubImpl(userTypeStub, typeName.ref(), false)
            } else {
                createAnyTypeStub(parent)
            }
        } else {
            createAnyTypeStub(parent)
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
                    hasValOrVar = false,
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
         * 从类型包装器中提取类型名称
         */
        fun extractTypeName(typeWrapper: TypeWrapper): Name? {
            val info = typeWrapper.info
            return when (info) {
                is FbSemaTyInfo.FbCompositeTyInfo -> {
                    info.declPtr?.decl?.let { Name.identifier(it) }
                }
                is FbSemaTyInfo.FbGenericTyInfo -> {
                    info.declPtr?.decl?.let { Name.identifier(it) }
                }
                else -> {
                    // 尝试从 kind 获取基本类型名称
                    getPrimitiveTypeName(typeWrapper.kind)?.let { Name.identifier(it) }
                }
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
