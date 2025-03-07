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

package cn.cangnova.cangjie.metadata.decompiler

import cn.cangnova.cangjie.contracts.description.CjEffectDeclaration
import cn.cangnova.cangjie.metadata.ProtoBuf

//
//class ClsContractBuilder(private val c: ClsStubBuilderContext, private val typeStubBuilder: TypeClsStubBuilder) :
//    ProtoBufContractDeserializer<CangJieTypeBean, Nothing?, ProtoBuf.Function>() {
//
//    fun loadContract(proto: ProtoBuf.Function): List<CjEffectDeclaration<CangJieTypeBean, Nothing?>>? {
//        return proto.contract.effectList.map { loadPossiblyConditionalEffect(it, proto) ?: return null }
//    }
//
//    override fun extractVariable(valueParameterIndex: Int, owner: ProtoBuf.Function): CjValueParameterReference<CangJieTypeBean, Nothing?> {
//        val type = if (valueParameterIndex < 0) {
//            owner.receiverType(c.typeTable)
//        } else owner.valueParameterList[valueParameterIndex].type(c.typeTable)
//        return if (type?.hasClassName() == true && c.nameResolver.getClassId(type.className) == StandardClassIds.Boolean) {
//            CjBooleanValueParameterReference(valueParameterIndex, name = IGNORE_REFERENCE_PARAMETER_NAME)
//        } else CjValueParameterReference(valueParameterIndex, name = IGNORE_REFERENCE_PARAMETER_NAME)
//    }
//
//    override fun extractType(proto: ProtoBuf.Expression): CangJieTypeBean? {
//        return typeStubBuilder.createCangJieTypeBean(proto.isInstanceType(c.typeTable))
//    }
//
//    override fun loadConstant(value: ProtoBuf.Expression.ConstantValue): CjConstantReference<CangJieTypeBean, Nothing?> {
//        return when (value) {
//            ProtoBuf.Expression.ConstantValue.TRUE -> CangJieContractConstantValues.TRUE
//            ProtoBuf.Expression.ConstantValue.FALSE -> CangJieContractConstantValues.FALSE
//            ProtoBuf.Expression.ConstantValue.NULL -> CangJieContractConstantValues.NULL
//        }
//    }
//
//    override fun getNotNull(): CjConstantReference<CangJieTypeBean, Nothing?> {
//        return CangJieContractConstantValues.NOT_NULL
//    }
//
//    override fun getWildcard(): CjConstantReference<CangJieTypeBean, Nothing?> {
//        return CangJieContractConstantValues.WILDCARD
//    }
//}
