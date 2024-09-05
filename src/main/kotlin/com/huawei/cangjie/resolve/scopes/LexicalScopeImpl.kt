package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.utils.Printer

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
