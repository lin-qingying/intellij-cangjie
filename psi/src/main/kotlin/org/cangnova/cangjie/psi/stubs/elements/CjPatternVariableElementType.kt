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

import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.CangJieVariableStub
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import org.cangnova.cangjie.psi.stubs.impl.CangJieVariableStubImpl
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import java.io.IOException

/**
 * 根据单一模式返回所有绑定模式
 */
fun CjCasePatternElement?.getAllBindings(): List<CjBindingPattern> {
    this ?: return emptyList()
    return when (this) {
        is CjBindingPattern -> listOf(this)
        is CjEnumPattern -> this.patterns.flatMap { it.getAllBindings() }
        is CjTuplePattern -> this.patterns.flatMap { it.getAllBindings() }
        else -> emptyList()
    }
}

/**
 * 获取所有具有描述符的模式声明（绑定模式和类型模式）
 *
 * 与 [getAllBindings] 不同，此函数同时返回 [CjBindingPattern] 和 [CjTypePattern]，
 * 因为这两种模式都会创建变量描述符。
 *
 * @return 所有具有描述符的模式声明列表
 */
fun CjCasePatternElement?.getAllPatternDeclarations(): List<CjNamedPattern> {
    this ?: return emptyList()
    return when (this) {
        is CjBindingPattern -> listOf(this)
        is CjTypePattern -> listOf(this)
        is CjEnumPattern -> this.patterns.flatMap { it.getAllPatternDeclarations() }
        is CjTuplePattern -> this.patterns.flatMap { it.getAllPatternDeclarations() }
        else -> emptyList()
    }
}

/**
 * 从 PSI 模式确定 PatternKind
 */
fun CjCasePatternElement?.toPatternKind(): PatternKind {
    return when (this) {
        null -> PatternKind.BINDING // 没有模式时默认为绑定模式
        is CjBindingPattern -> PatternKind.BINDING
        is CjTuplePattern -> PatternKind.TUPLE
        is CjEnumPattern -> PatternKind.ENUM
        is CjWildcardPattern -> PatternKind.WILDCARD
        else -> PatternKind.BINDING // 其他情况默认为绑定模式
    }
}

class CjPatternVariableElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieVariableStub, CjPatternVariable>(
        debugName,
        CjPatternVariable::class.java,
        CangJieVariableStub::class.java,
    ) {

    override fun createStub(psi: CjPatternVariable, parentStub: StubElement<*>?): CangJieVariableStub {
        val patternKind = psi.pattern.toPatternKind()

        return CangJieVariableStubImpl(
            parentStub,
            patternKind,
            psi.isVar,
            psi.isTopLevel,
            psi.hasInitializer(),
            psi.typeReference != null,
            null, // origin
        )
    }

    companion object {
        @Throws(IOException::class)
        fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieVariableStub {
            val patternKindOrdinal = dataStream.readInt()
            val patternKind = PatternKind.fromOrdinal(patternKindOrdinal)
            val isVar = dataStream.readBoolean()
            val isTopLevel = dataStream.readBoolean()
            val hasInitializer = dataStream.readBoolean()
            val hasReturnTypeRef = dataStream.readBoolean()

            return CangJieVariableStubImpl(
                parentStub,
                patternKind,
                isVar,
                isTopLevel,
                hasInitializer,
                hasReturnTypeRef,
                deserialize(dataStream),
            )
        }

        fun serialize(stub: CangJieVariableStub, dataStream: StubOutputStream) {
            dataStream.writeInt(stub.getPatternKind().ordinal)
            dataStream.writeBoolean(stub.isVar())
            dataStream.writeBoolean(stub.isTopLevel())
            dataStream.writeBoolean(stub.hasInitializer())
            dataStream.writeBoolean(stub.hasReturnTypeRef())

            if (stub is CangJieVariableStubImpl) {
                serialize(stub.origin, dataStream)
            }
        }
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieVariableStub, dataStream: StubOutputStream) {
        Companion.serialize(stub, dataStream)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieVariableStub {
        return Companion.deserialize(dataStream, parentStub)
    }

    override fun indexStub(stub: CangJieVariableStub, sink: IndexSink) {
        getInstance().indexPatternVariable(stub, sink)
    }
}
