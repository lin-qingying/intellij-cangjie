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

package com.linqingying.cangjie.highlighter.renderersUtil

import com.google.common.html.HtmlEscapers
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.diagnostics.rendering.RenderingContext
import com.linqingying.cangjie.diagnostics.rendering.SmartTypeRenderer
import com.linqingying.cangjie.diagnostics.rendering.asRenderer
import com.linqingying.cangjie.highlighter.CangJieHighlightingBundle
import com.linqingying.cangjie.renderer.ClassifierNamePolicy
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.renderer.RenderingFormat
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.calls.components.hasDefaultValue
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.calls.util.hasTypeMismatchErrorOnParameter
import com.linqingying.cangjie.resolve.calls.util.hasUnmappedArguments
import com.linqingying.cangjie.types.ErrorUtils


fun renderResolvedCall(resolvedCall: ResolvedCall<*>, context: RenderingContext): String {
    val typeRenderer = SmartTypeRenderer(HTML_FOR_UNINFERRED_TYPE_PARAMS)
    val descriptorRenderer = HTML_FOR_UNINFERRED_TYPE_PARAMS.asRenderer()
    val stringBuilder = StringBuilder("")
    val indent = "&nbsp;&nbsp;"

    fun append(any: Any): StringBuilder = stringBuilder.append(any)

    fun renderParameter(parameter: ValueParameterDescriptor): String {
        val varargElementType = parameter.varargElementType
        val parameterType = varargElementType ?: parameter.type
        val renderedParameter = (if (varargElementType != null) "<b>vararg</b> " else "") +
                typeRenderer.render(parameterType, context) +
                if (parameter.hasDefaultValue()) " = ..." else ""
        return if (resolvedCall.hasTypeMismatchErrorOnParameter(parameter))
            renderError(renderedParameter)
        else
            renderedParameter
    }

    fun appendTypeParametersSubstitution() {
        val parametersToArgumentsMap = resolvedCall.typeArguments
        fun TypeParameterDescriptor.isInferred(): Boolean {
            val typeArgument = parametersToArgumentsMap[this] ?: return false
            return !ErrorUtils.isUninferredTypeVariable(typeArgument)
        }

        val typeParameters = resolvedCall.candidateDescriptor.typeParameters
        val (inferredTypeParameters, notInferredTypeParameters) = typeParameters.partition(TypeParameterDescriptor::isInferred)

        append("<br/>$indent<i>${CangJieHighlightingBundle.message("type.parameters.where")}</i> ")
        if (notInferredTypeParameters.isNotEmpty()) {
            append(notInferredTypeParameters.joinToString { typeParameter -> renderError(typeParameter.name) })
            append("<i> ${CangJieHighlightingBundle.message("cannot.be.inferred")}</i>")
            if (inferredTypeParameters.isNotEmpty()) {
                append("; ")
            }
        }

        val typeParameterToTypeArgumentMap = resolvedCall.typeArguments
        if (inferredTypeParameters.isNotEmpty()) {
            append(inferredTypeParameters.joinToString { typeParameter ->
                "${typeParameter.name} = ${typeRenderer.render(typeParameterToTypeArgumentMap[typeParameter]!!, context)}"
            })
        }
    }

    val resultingDescriptor = resolvedCall.resultingDescriptor
    val receiverParameter = resultingDescriptor.extensionReceiverParameter
    if (receiverParameter != null) {
        append(typeRenderer.render(receiverParameter.type, context)).append(".")
    }
    append(HtmlEscapers.htmlEscaper().escape(resultingDescriptor.name.asString())).append("(")
    append(resultingDescriptor.valueParameters.joinToString(transform = ::renderParameter))
    append(if (resolvedCall.hasUnmappedArguments()) renderError(")") else ")")

    if (resolvedCall.candidateDescriptor.typeParameters.isNotEmpty()) {
        appendTypeParametersSubstitution()
        append(CangJieHighlightingBundle.message("i.for.i.br.0", indent))
        // candidate descriptor is not in context of the rest of the message
        append(descriptorRenderer.render(resolvedCall.candidateDescriptor, RenderingContext.of(resolvedCall.candidateDescriptor)))
    } else {
        append(" <i>${CangJieHighlightingBundle.message("defined.in")}</i> ")
        val containingDeclaration = resultingDescriptor.containingDeclaration
        val fqName = DescriptorUtils.getFqName(containingDeclaration)
        append(if (fqName.isRoot) CangJieHighlightingBundle.message("root.package") else fqName.asString())
    }
    return stringBuilder.toString()
}
private const val RED_TEMPLATE = "<font color=red><b>%s</b></font>"
private const val STRONG_TEMPLATE = "<b>%s</b>"

fun renderStrong(o: Any): String = STRONG_TEMPLATE.format(o)

fun renderError(o: Any): String = RED_TEMPLATE.format(o)

fun renderStrong(o: Any, error: Boolean): String = (if (error) RED_TEMPLATE else STRONG_TEMPLATE).format(o)

private val HTML_FOR_UNINFERRED_TYPE_PARAMS: DescriptorRenderer = DescriptorRenderer.withOptions {
    uninferredTypeParameterAsName = true
    modifiers = emptySet()
    classifierNamePolicy = ClassifierNamePolicy.SHORT
    textFormat = RenderingFormat.HTML
}
