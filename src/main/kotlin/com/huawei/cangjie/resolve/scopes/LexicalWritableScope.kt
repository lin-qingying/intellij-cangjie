package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.utils.Printer

class LexicalWritableScope(
    parent: LexicalScope,
    override val ownerDescriptor: DeclarationDescriptor,
    override val isOwnerDescriptorAccessibleByLabel: Boolean,
    redeclarationChecker: LocalRedeclarationChecker,
    override val kind: LexicalScopeKind
) : LexicalScopeStorage(parent, redeclarationChecker) {
    override val implicitReceiver: ReceiverParameterDescriptor?
        get() = null
    override val contextReceiversGroup: List<ReceiverParameterDescriptor>
        get() = emptyList()
    private var canWrite: Boolean = true
    private var lastSnapshot: Snapshot? = null

    fun freeze() {
        canWrite = false
    }

    private inner class Snapshot(val descriptorLimit: Int) : LexicalScope by this {
        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
            addedDescriptors.subList(0, descriptorLimit)

        override fun getContributedClassifier(name: Name, location: LookupLocation) =
            variableOrClassDescriptorByName(name, descriptorLimit) as? ClassifierDescriptor

        // NB. This is important to have this explicit override, otherwise calls will be delegated to `this`-delegate,
        // which will use default implementation from `ResolutionScope`, which will call `getContributedClassifier` on
        // the `LexicalWritableScope` instead of calling it on this snapshot
        override fun getContributedClassifierIncludeDeprecated(
            name: Name,
            location: LookupLocation
        ): DescriptorWithDeprecation<ClassifierDescriptor>? {
            return (variableOrClassDescriptorByName(name, descriptorLimit) as? ClassifierDescriptor)
                ?.let { DescriptorWithDeprecation.createNonDeprecated(it) }
        }

        override fun getContributedVariables(name: Name, location: LookupLocation) =
            listOfNotNull(variableOrClassDescriptorByName(name, descriptorLimit) as? VariableDescriptor)

        override fun getContributedFunctions(name: Name, location: LookupLocation) =
            functionsByName(name, descriptorLimit)


        override fun toString(): String = "Snapshot($descriptorLimit) for $kind"

        override fun printStructure(p: Printer) {
            p.println("Snapshot with descriptorLimit = $descriptorLimit for scope:")
            this@LexicalWritableScope.printStructure(p)
        }
    }

    fun takeSnapshot(): LexicalScope {
        if (lastSnapshot == null || lastSnapshot!!.descriptorLimit != addedDescriptors.size) {
            lastSnapshot = Snapshot(addedDescriptors.size)
        }
        return lastSnapshot!!
    }

    fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {
        checkMayWrite()
        addVariableOrClassDescriptor(variableDescriptor)
    }

    fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor) {
        checkMayWrite()
        addFunctionDescriptorInternal(functionDescriptor)
    }

    fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor) {
        checkMayWrite()
        addVariableOrClassDescriptor(classifierDescriptor)
    }

    override fun toString(): String = kind.toString()

    override fun printStructure(p: Printer) {
        p.println(
            this::class.java.simpleName,
            ": ",
            kind,
            "; for descriptor: ",
            ownerDescriptor.name,
            " with implicitReceivers: ",
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

    private fun checkMayWrite() {
        if (!canWrite) {
            throw IllegalStateException("Cannot write into freezed scope:" + toString())
        }
    }

}