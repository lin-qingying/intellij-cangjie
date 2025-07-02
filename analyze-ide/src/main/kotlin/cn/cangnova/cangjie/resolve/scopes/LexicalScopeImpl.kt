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

package cn.cangnova.cangjie.resolve.scopes

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.utils.Printer

class LexicalScopeImpl @JvmOverloads constructor(
    parent: HierarchicalScope,
    override val ownerDescriptor: DeclarationDescriptor,
    override val isOwnerDescriptorAccessibleByLabel: Boolean,
    override val implicitReceiver: ReceiverParameterDescriptor?,
    override val contextReceiversGroup: List<ReceiverParameterDescriptor>,
    override val kind: LexicalScopeKind,
    redeclarationChecker: LocalRedeclarationChecker = LocalRedeclarationChecker.DO_NOTHING,
    initialize: LexicalScopeImpl.InitializeHandler.() -> Unit = {}
) : LexicalScope, LexicalScopeStorage(parent, redeclarationChecker) {

    init {
        InitializeHandler().initialize()
    }

//    override fun getContributedPackageFqName(name: Name, location: LookupLocation): List<FqName> {
//        TODO("Not yet implemented")
//    }

    override fun printStructure(p: Printer) {
        p.println(
            this::class.java.simpleName,
            ": ",
            kind,
            "; for descriptor: ",
            ownerDescriptor.name,
            " with implicitReceiver: ",
            implicitReceiver?.value ?: "NONE",
            " with contextReceiversGroup: ",
            if (contextReceiversGroup.isEmpty()) "NONE" else contextReceiversGroup.joinToString { it.value.toString() },
            " {"
        )
        p.pushIndent()

        p.print("parent = ")
        parent.printStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }



    override fun toString(): String = kind.toString()

    inner class InitializeHandler {

        fun addVariableDescriptor(variableDescriptor: VariableDescriptor): Unit =
            this@LexicalScopeImpl.addVariableOrClassDescriptor(variableDescriptor)

        fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor): Unit =
            this@LexicalScopeImpl.addFunctionDescriptorInternal(functionDescriptor)

        fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor): Unit =
            this@LexicalScopeImpl.addVariableOrClassDescriptor(classifierDescriptor)

    }
}
