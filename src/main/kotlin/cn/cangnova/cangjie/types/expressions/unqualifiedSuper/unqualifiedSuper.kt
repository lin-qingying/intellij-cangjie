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

package cn.cangnova.cangjie.types.expressions.unqualifiedSuper

import cn.cangnova.cangjie.descriptors.isClass
import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.CallableMemberDescriptor
import cn.cangnova.cangjie.descriptors.ClassKind
import cn.cangnova.cangjie.descriptors.Modality
import cn.cangnova.cangjie.incremental.components.NoLookupLocation
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.psi.CjCallExpression
import cn.cangnova.cangjie.psi.CjDotQualifiedExpression
import cn.cangnova.cangjie.psi.CjSimpleNameExpression
import cn.cangnova.cangjie.psi.CjSuperExpression
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker
import cn.cangnova.cangjie.types.util.TypeUtils
import cn.cangnova.cangjie.types.util.isAny
import cn.cangnova.cangjie.types.util.isInterface
import com.intellij.util.SmartList

fun resolveUnqualifiedSuperFromExpressionContext(
    superExpression: CjSuperExpression,
    supertypes: Collection<CangJieType>,
    anyType: CangJieType
): Pair<Collection<CangJieType>, Boolean> {
    val parentElement = superExpression.parent

    if (parentElement is CjDotQualifiedExpression) {
        when (val selectorExpression = parentElement.selectorExpression) {
            is CjCallExpression -> {
                // super.foo(...): foo can be a function or a property of a callable type
                val calleeExpression = selectorExpression.calleeExpression
                if (calleeExpression is CjSimpleNameExpression) {
                    val calleeName = calleeExpression.referencedNameAsName
                    return if (isCallingMethodOfAny(selectorExpression, calleeName)) {
                        resolveSupertypesForMethodOfAny(supertypes, calleeName, anyType)
                    } else {
                        resolveSupertypesByCalleeName(supertypes, calleeName) to false
                    }
                }
            }
            is CjSimpleNameExpression -> {
                // super.x: x can be a property only
                // NB there are no properties in kotlin.Any
                return resolveSupertypesByPropertyName(supertypes, selectorExpression.referencedNameAsName) to false
            }
        }
    }

    return emptyList<CangJieType>() to false
}
fun isPossiblyAmbiguousUnqualifiedSuper(superExpression: CjSuperExpression, supertypes: Collection<CangJieType>): Boolean =
    supertypes.size > 1 ||
            (supertypes.size == 1 && supertypes.single().isInterface() && isCallingMethodOfAnyWithSuper(superExpression))
private fun isCallingMethodOfAnyWithSuper(superExpression: CjSuperExpression): Boolean {
    val parent = superExpression.parent
    if (parent is CjDotQualifiedExpression) {
        val selectorExpression = parent.selectorExpression
        if (selectorExpression is CjCallExpression) {
            val calleeExpression = selectorExpression.calleeExpression
            if (calleeExpression is CjSimpleNameExpression) {
                val calleeName = calleeExpression.referencedNameAsName
                return isCallingMethodOfAny(selectorExpression, calleeName)
            }
        }
    }

    return false
}

private val ARITY_OF_METHODS_OF_ANY = hashMapOf("hashCode" to 0, "equals" to 1, "toString" to 0)

private fun isCallingMethodOfAny(callExpression: CjCallExpression, calleeName: Name): Boolean =
    ARITY_OF_METHODS_OF_ANY.getOrElse(calleeName.asString()) { -1 } == callExpression.valueArguments.size
private fun resolveSupertypesForMethodOfAny(
    supertypes: Collection<CangJieType>,
    calleeName: Name,
    anyType: CangJieType
): Pair<Collection<CangJieType>, Boolean> {
    val (typesWithConcreteOverride, isEqualsMigration) = resolveSupertypesByMembers(supertypes, allowNonConcreteInterfaceMembers = false) {
        getFunctionMembers(it, calleeName)
    }
    return typesWithConcreteOverride.ifEmpty { listOf(anyType) } to isEqualsMigration
}
private fun resolveSupertypesByCalleeName(supertypes: Collection<CangJieType>, calleeName: Name): Collection<CangJieType> =
    resolveSupertypesByMembers(supertypes, allowNonConcreteInterfaceMembers = true) {
        getFunctionMembers(it, calleeName) +
                getPropertyMembers(it, calleeName)
    }.first

private fun resolveSupertypesByPropertyName(supertypes: Collection<CangJieType>, propertyName: Name): Collection<CangJieType> =
    resolveSupertypesByMembers(supertypes, allowNonConcreteInterfaceMembers = true) {
        getPropertyMembers(it, propertyName)
    }.first

private inline fun resolveSupertypesByMembers(
    supertypes: Collection<CangJieType>,
    allowNonConcreteInterfaceMembers: Boolean,
    getMembers: (CangJieType) -> Collection<CallableMemberDescriptor>
): Pair<Collection<CangJieType>, Boolean> {
    val typesWithConcreteMembers = SmartList<CangJieType>()
    val typesWithNonConcreteMembers = SmartList<CangJieType>()

    for (supertype in supertypes) {
        val members = getMembers(supertype)
        if (members.isNotEmpty()) {
                if (members.any { isConcreteMember(supertype, it) })
                typesWithConcreteMembers.add(supertype)
            else if (members.any { it.dispatchReceiverParameter?.type?.isAny() == false })
                typesWithNonConcreteMembers.add(supertype)
        }
    }

    typesWithConcreteMembers.removeAll { typeWithConcreteMember ->
        typesWithNonConcreteMembers.any { typeWithNonConcreteMember ->
            CangJieTypeChecker.DEFAULT.isSubtypeOf(typeWithNonConcreteMember, typeWithConcreteMember)
        }
    }

    return when {
        typesWithConcreteMembers.isNotEmpty() ->
            typesWithConcreteMembers to false
        allowNonConcreteInterfaceMembers ->
            typesWithNonConcreteMembers to false
        else ->
            typesWithNonConcreteMembers.filter {
                // We aren't interested in objects or enum classes here
                // (objects can't be inherited, enum classes cannot have specific equals/hashCode)
                TypeUtils.getClassDescriptor(it)?.kind?.isClass == true
            } to true
    }
}


private val LOOKUP_LOCATION = NoLookupLocation.MATCH_GET_SUPER_MEMBERS

private fun getFunctionMembers(type: CangJieType, name: Name): Collection<CallableMemberDescriptor> =
    type.memberScope.getContributedFunctions(name, LOOKUP_LOCATION)

private fun getPropertyMembers(type: CangJieType, name: Name): Collection<CallableMemberDescriptor> =
    type.memberScope.getContributedVariables(name, LOOKUP_LOCATION).filterIsInstanceTo(SmartList())
private fun isConcreteMember(supertype: CangJieType, memberDescriptor: CallableMemberDescriptor): Boolean {
    // "Concrete member" is a function or a property that is not abstract,
    // and is not an implicit fake override for a method of Any on an interface.

    if (memberDescriptor.modality == Modality.ABSTRACT)
        return false

    val classDescriptorForSupertype = TypeUtils.getClassDescriptor(supertype)
    val memberKind = memberDescriptor.kind
    if (classDescriptorForSupertype?.kind == ClassKind.INTERFACE && memberKind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        // We have a fake override on interface. It should have a dispatch receiver, which should not be Any.
        val dispatchReceiverType = memberDescriptor.dispatchReceiverParameter?.type ?: return false
        val dispatchReceiverClass = TypeUtils.getClassDescriptor(dispatchReceiverType) ?: return false
        return !CangJieBuiltIns.isAny(dispatchReceiverClass)
    }

    return true
}
