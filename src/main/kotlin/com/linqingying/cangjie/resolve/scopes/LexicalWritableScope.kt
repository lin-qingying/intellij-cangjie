package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.utils.Printer

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

//    override fun getContributedPackageFqName(name: Name, location: LookupLocation): List<FqName> = emptyList()

    private var canWrite: Boolean = true
    private var lastSnapshot: Snapshot? = null

    /**
     * 将当前对象置于冻结状态
     *
     * 冻结状态下，对象的canWrite属性被设置为false，表示不能再对该对象进行写操作
     * 这个方法通常用于在某些情况下需要禁止对对象的修改，比如在多线程环境下防止并发修改
     */
    fun freeze() {
        canWrite = false
    }

    /**
     * 内部类 [Snapshot] 用于捕获当前词法作用域的一个快照，并限制描述符的数量。
     * 它主要用于控制词法作用域的暴露程度，特别是在某些场景下需要限制可见的描述符数量。
     *
     * @param descriptorLimit 描述符的数量限制。
     */
    private inner class Snapshot(val descriptorLimit: Int) : LexicalScope by this {

        /**
         * 获取符合过滤条件的贡献描述符。
         *
         * @param kindFilter 描述符种类过滤器。
         * @param nameFilter 名称过滤器。
         * @return 符合条件的描述符列表。
         */
        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
            addedDescriptors.subList(0, descriptorLimit)

        /**
         * 获取指定名称的分类器描述符。
         *
         * @param name 分类器名称。
         * @param location 查找位置。
         * @return 符合条件的分类器描述符。
         */
        override fun getContributedClassifier(name: Name, location: LookupLocation) =
            variableOrClassDescriptorByName(name, descriptorLimit) as? ClassifierDescriptor

        /**
         * 获取包括已弃用的分类器描述符。
         * 这个方法的显式重写非常重要，否则调用将委托给 `this` 代理，这将使用 `ResolutionScope` 的默认实现，
         * 而不是在这个快照上调用 `getContributedClassifier`。
         *
         * @param name 分类器名称。
         * @param location 查找位置。
         * @return 包括已弃用的分类器描述符。
         */
        override fun getContributedClassifierIncludeDeprecated(
            name: Name,
            location: LookupLocation
        ): DescriptorWithDeprecation<ClassifierDescriptor>? {
            return (variableOrClassDescriptorByName(name, descriptorLimit) as? ClassifierDescriptor)
                ?.let { DescriptorWithDeprecation.createNonDeprecated(it) }
        }

        /**
         * 获取指定名称的变量描述符。
         *
         * @param name 变量名称。
         * @param location 查找位置。
         * @return 符合条件的变量描述符集合。
         */
        override fun getContributedVariables(
            name: Name,
            location: LookupLocation
        ): Collection<@JvmWildcard VariableDescriptor> =
            listOfNotNull(variableOrClassDescriptorByName(name, descriptorLimit) as? VariableDescriptor)

        /**
         * 获取指定名称的函数描述符。
         *
         * @param name 函数名称。
         * @param location 查找位置。
         * @return 符合条件的函数描述符。
         */
        override fun getContributedFunctions(name: Name, location: LookupLocation) =
            functionsByName(name, descriptorLimit)

        /**
         * 返回快照的字符串表示形式。
         *
         * @return 快照的字符串表示形式。
         */
        override fun toString(): String = "Snapshot($descriptorLimit) for $kind"

        /**
         * 打印快照的结构信息。
         *
         * @param p 打印器对象。
         */
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

    override fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {
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
