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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.ide.imports.getConstructors
import cn.cangnova.cangjie.psi.CjCallExpression
import cn.cangnova.cangjie.psi.CjNameReferenceExpression
import cn.cangnova.cangjie.psi.CjSimpleNameExpression
import cn.cangnova.cangjie.psi.getTypeArguments
import cn.cangnova.cangjie.resolve.calls.CallResolver
import cn.cangnova.cangjie.resolve.calls.util.CallMaker
import cn.cangnova.cangjie.types.expressions.ExpressionTypingContext
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import cn.cangnova.cangjie.types.util.createVArrayType
import cn.cangnova.cangjie.utils.exceptions.CangJieTypeInfo

class VArrayResolver(
    val typeResolver: TypeResolver,
    val builtIns: CangJieBuiltIns,
    val callResolver: CallResolver,

    ) {
    fun resolve(expression: CjCallExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        val type = (expression.calleeExpression as? CjNameReferenceExpression)?.let { resolve(it, context) }?.type
            ?: return noTypeInfo(context.dataFlowInfo)

        val descriptor = type.constructor.declarationDescriptor ?: return noTypeInfo(context.dataFlowInfo)

//        验证构造函数
        val constructor = descriptor.getConstructors()
        val call = CallMaker.makeCall(null, null, expression)


        val resolutionResults =
            callResolver.resolveCall(context, expression, call, constructor)



        return createTypeInfo(type)

    }

    fun resolve(expression: CjSimpleNameExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        if (expression !is CjNameReferenceExpression) return noTypeInfo(context.dataFlowInfo)

        assert(expression.referencedName == "VArray")

        val argumentTypePsi =
            expression.getTypeArguments().firstOrNull()?.typeReference ?: return noTypeInfo(context.dataFlowInfo)
        val argumentType = typeResolver.resolveType(context.scope, argumentTypePsi, context.trace, false)


        val size = expression.typeArgumentList?.varrayLiteral?.text?.toInt() ?: return noTypeInfo(context.dataFlowInfo)

        val varrayType = createVArrayType(
            builtIns, argumentType, size
        )
        return createTypeInfo(varrayType)
    }
}
