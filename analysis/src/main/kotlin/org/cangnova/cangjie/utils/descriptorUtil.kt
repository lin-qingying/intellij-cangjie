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

import com.intellij.util.SmartList
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DescriptorToSourceUtils
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.psiUtil.unpackFunctionLiteral
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.classValueTypeDescriptor
import org.cangnova.cangjie.resolve.getSuperClassNotAny
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.DeferredType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.contains
fun ClassDescriptor.getAllSuperclassesWithoutAny() =
    generateSequence(
        getSuperClassNotAny(),
        ClassDescriptor::getSuperClassNotAny
    ).toCollection(SmartList<ClassDescriptor>())
val VariableDescriptor.isUnderscoreNamed
    get() = !name.isSpecial && name.identifier == "_"

fun CallableMemberDescriptor.firstOverridden(
    useOriginal: Boolean = false,
    predicate: (CallableMemberDescriptor) -> Boolean
): CallableMemberDescriptor? {
    var result: CallableMemberDescriptor? = null
    return DFS.dfs(listOf(this),
        { current ->
            val descriptor = if (useOriginal) current?.original else current
            (descriptor?.overriddenDescriptors ?: emptyList()) as MutableIterable<CallableMemberDescriptor>
        },
        object : DFS.AbstractNodeHandler<CallableMemberDescriptor, CallableMemberDescriptor?>() {
            override fun beforeChildren(current: CallableMemberDescriptor) = result == null
            override fun afterChildren(current: CallableMemberDescriptor) {
                if (result == null && predicate(current)) {
                    result = current
                }
            }

            override fun result(): CallableMemberDescriptor? = result
        }
    )
}
inline fun <reified T : CjDeclaration> reportOnDeclarationAs(
    trace: BindingTrace,
    descriptor: DeclarationDescriptor,
    what: (T) -> Diagnostic
) {
    DescriptorToSourceUtils.descriptorToDeclaration(descriptor)?.let { psiElement ->
        (psiElement as? T)?.let {
            trace.report(what(it))
        }
            ?: throw AssertionError("Declaration for $descriptor is expected to be ${T::class.simpleName}, actual declaration: $psiElement")
    } ?: throw AssertionError("No declaration for $descriptor")
}

fun FunctionDescriptor.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false

    val returnType = returnType ?: return false
    if (returnType is DeferredType && returnType.isComputing) return false

    if (returnType.constructor != typeParameter.typeConstructor) return false

    fun CangJieType.isBadType() = contains { it.constructor == typeParameter.typeConstructor }

    return !(valueParameters.any { it.type.isBadType() } || extensionReceiverParameter?.type?.isBadType() == true)
}

val DeclarationDescriptor.isExtension: Boolean
    get() = TODO("是否扩展")


fun TypeConstructor.supertypesWithAny(): Collection<CangJieType> {
    val supertypes = supertypes
    val noSuperClass = supertypes.map { it.constructor.declarationDescriptor as? ClassDescriptor }.all {
        it == null || it.kind == ClassKind.INTERFACE
    }
    return if (noSuperClass) supertypes + builtIns.anyType else supertypes
}
/** If a literal of this class can be used as a value, returns the type of this value */
val ClassDescriptor.classValueType: CangJieType?
    get() = classValueTypeDescriptor?.defaultType

fun CjCallExpression.getLastLambdaExpression(): CjLambdaExpression? {
    if (lambdaArguments.isNotEmpty()) return null
    return valueArguments.lastOrNull()?.getArgumentExpression()?.unpackFunctionLiteral()
}
