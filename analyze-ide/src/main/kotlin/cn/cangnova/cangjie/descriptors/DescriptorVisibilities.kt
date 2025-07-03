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
package cn.cangnova.cangjie.descriptors

import cn.cangnova.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.utils.ModuleVisibilityHelper

object DescriptorVisibilities {
    /**
     * This value should be used for receiverValue parameter of Visibility.isVisible
     * iff there is intention to determine if member is visible without receiver related checks being performed.
     */
    val ALWAYS_SUITABLE_RECEIVER: ReceiverValue = object : ReceiverValue {
        override fun getType(): CangJieType {
            throw IllegalStateException("This method should not be called")
        }

        override fun replaceType(newType: CangJieType): ReceiverValue {
            throw IllegalStateException("This method should not be called")
        }

        override fun getOriginal(): ReceiverValue {
            return this
        }
    }
    @JvmField
    val INHERITED: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.Inherited) {
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            throw IllegalStateException("Visibility is unknown yet") //This method shouldn't be invoked for INHERITED visibility
        }
    }

    /* Visibility for fake override invisible members (they are created for better error reporting) */
    @JvmField
    val INVISIBLE_FAKE: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.InvisibleFake) {
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            return false
        }
    }

    /**
     * private*****可见*******************可见********************可见**********************可见
     */
    //    当前文件可见
    @JvmField
    val PRIVATE: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.Private) {
        //        private boolean hasContainingSourceFile(@NotNull DeclarationDescriptor descriptor) {
        //            return DescriptorUtils.getContainingSourceFile(descriptor) != SourceFile.NO_SOURCE_FILE;
        //        }
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            if (DescriptorUtils.isTopLevelDeclaration(what) /* && hasContainingSourceFile(from)*/) {
                return inSameFile(what, from)
            }

            if (what is ConstructorDescriptor) {
                val classDescriptor: ClassifierDescriptorWithTypeParameters? = what.containingDeclaration
                if (useSpecialRulesForPrivateSealedConstructors
                    && DescriptorUtils.isSealedClass(classDescriptor)
                    && DescriptorUtils.isTopLevelDeclaration(classDescriptor)
                    && from is ConstructorDescriptor
                    && DescriptorUtils.isTopLevelDeclaration(from.containingDeclaration)
                    && inSameFile(what, from)
                ) {
                    return true
                }
            }

            var parent: DeclarationDescriptor? = what
            while (parent != null) {
                parent = parent.containingDeclaration
                if ((parent is ClassDescriptor) ||
                    parent is PackageFragmentDescriptor
                ) {
                    break
                }
            }
            if (parent == null) {
                return false
            }
            var fromParent: DeclarationDescriptor? = from
            while (fromParent != null) {
                if (parent === fromParent) {
                    return true
                }
                if (fromParent is PackageFragmentDescriptor) {
                    return parent is PackageFragmentDescriptor
                            && parent.fqName.equals(fromParent.fqName)
                            && DescriptorUtils.areInSameModule(fromParent, parent)
                }
                fromParent = fromParent.containingDeclaration
            }
            return false
        }
    }

    //所有可见
    @JvmField
    val PUBLIC: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.Public) {
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            return true
        }
    }
    @JvmField
    val DEFAULT_VISIBILITY: DescriptorVisibility = PUBLIC


    /***********************************************访问修饰符规则 */
    /**************文件****************包 & 子包*******************模块************************所有包 */
    /**private*****可见******************不可见********************不可见**********************不可见 */
    /**internal****可见*******************可见********************不可见**********************不可见 */
    /**private*****可见*******************可见*********************可见**********************不可见 */
    /**
     * This visibility is needed for the next case:
     * class A<in T>(t: T) {
     * private val t: T = t // visibility for t is PRIVATE_TO_THIS
     *
     *
     * fun test() {
     * val x: T = t // correct
     * val y: T = this.t // also correct
     * }
     * fun foo(a: A<String>) {
     * val x: String = a.t // incorrect, because a.t can be Any
     * }
     * }
    </String></in> */
    val PRIVATE_TO_THIS: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.PrivateToThis) {
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            return false
        }
    }
    @JvmField
    val LOCAL: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.Local) {
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            return false
        } //        @Override
        //        public bool isVisible(
        //                @Nullable ReceiverValue receiver,
        //                @NotNull DeclarationDescriptorWithVisibility what,
        //                @NotNull DeclarationDescriptor from,
        //                bool useSpecialRulesForPrivateSealedConstructors
        //        ) {
        //            throw new IllegalStateException("This method shouldn't be invoked for LOCAL visibility");
        //        }
    }

    // Currently used as default visibility of FunctionDescriptor
    // It's needed to prevent NPE when requesting non-nullable visibility of descriptor before `initialize` has been called
    val UNKNOWN: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.Unknown) {
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            return false
        } //        @Override
        //        public bool isVisible(
        //                @Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from,
        //                bool useSpecialRulesForPrivateSealedConstructors
        //        ) {
        //            return false;
        //        }
    }
    private val MODULE_VISIBILITY_HELPER: ModuleVisibilityHelper? = null

    // 文件  包以及子包可见
    @JvmField
    val INTERNAL: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.Internal) {
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            if (from is ModuleDescriptor) {
                return true
            }
            //            DescriptorUtils.getContainingModule(what);
            val fromModule = DescriptorUtils.getPackageDeclarationDescriptor(from)

            //            if (what instanceof  PackageViewDescriptor){
//                if (!fromModule.shouldSeeInternalsOf((PackageViewDescriptor)what)) return false;
//
//            }
            val whatModule = DescriptorUtils.getPackageDeclarationDescriptor(what)

            //            判断 fromModule 是不是 whatModule的子包或本包
//             fromModule 是否可见 whatModule
            if (whatModule is PackageFragmentDescriptor) {
                if (!fromModule.shouldSeeInternalsOf(whatModule)) return false
            } else {
                if (!fromModule.shouldSeeInternalsOf(whatModule)) return false
            }


            return MODULE_VISIBILITY_HELPER.isInFriendModule(what, from)
        }
    }

    //    模块内可见
    val PROTECTED: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.Protected) {
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            if (what.isSameModule(from)) {
                return true
            }

            return MODULE_VISIBILITY_HELPER.isInFriendModule(what, from)
        }
    }

    /**
     * This value should be used for receiverValue parameter of Visibility.isVisible
     * iff there is intention to determine if member is visible for any receiver.
     */
    private val IRRELEVANT_RECEIVER: ReceiverValue = object : ReceiverValue {
        override fun getType(): CangJieType {
            throw IllegalStateException("This method should not be called")
        }

        override fun replaceType(newType: CangJieType): ReceiverValue {
            throw IllegalStateException("This method should not be called")
        }

        override fun getOriginal(): ReceiverValue {
            return this
        }
    }
    private val visibilitiesMapping: MutableMap<Visibility?, DescriptorVisibility> =
        HashMap<Visibility?, DescriptorVisibility>()
    private val ORDERED_VISIBILITIES: MutableMap<DescriptorVisibility?, Int?>? = null

    init {
        recordVisibilityMapping(PRIVATE)
        recordVisibilityMapping(PRIVATE_TO_THIS)
        recordVisibilityMapping(PROTECTED)
        recordVisibilityMapping(INTERNAL)
        recordVisibilityMapping(PUBLIC)
        recordVisibilityMapping(LOCAL)
        recordVisibilityMapping(INHERITED)
        recordVisibilityMapping(INVISIBLE_FAKE)
        recordVisibilityMapping(UNKNOWN)
    }

    init {
        val iterator: MutableIterator<ModuleVisibilityHelper?> = ServiceLoader.load<ModuleVisibilityHelper?>(
            ModuleVisibilityHelper::class.java,
            ModuleVisibilityHelper::class.java.getClassLoader()
        ).iterator()
        MODULE_VISIBILITY_HELPER = if (iterator.hasNext()) iterator.next() else ModuleVisibilityHelper.EMPTY.INSTANCE
    }

    init {
        val visibilities: MutableMap<DescriptorVisibility?, Int?> =
            newHashMapWithExpectedSize<DescriptorVisibility?, Int?>(4)
        visibilities.put(PRIVATE_TO_THIS, 0)
        visibilities.put(PRIVATE, 0)
        visibilities.put(INTERNAL, 1)
        visibilities.put(PROTECTED, 1)
        visibilities.put(PUBLIC, 2)
        ORDERED_VISIBILITIES = Collections.unmodifiableMap<DescriptorVisibility?, Int?>(visibilities)
    }

    private fun recordVisibilityMapping(visibility: DescriptorVisibility) {
        visibilitiesMapping.put(visibility.delegate, visibility)
    }

    // Note that this method returns false if `from` declaration is `init` initializer
    // because initializer does not have source element
    fun inSameFile(what: DeclarationDescriptor, from: DeclarationDescriptor): Boolean {
        val fromContainingFile = DescriptorUtils.getContainingSourceFile(from)
        if (fromContainingFile !== SourceFile.NO_SOURCE_FILE) {
            return fromContainingFile == DescriptorUtils.getContainingSourceFile(what)
        }
        return false
    }

    fun formName(name: String): DescriptorVisibility {
        return when (name) {
            "public" -> PUBLIC
            "protected" -> PROTECTED
            "internal" -> INTERNAL
            "private" -> PRIVATE
            else -> throw IllegalArgumentException("unknown visibility name: " + name)
        }
    }

    //    public static boolean isVisibleIgnoringReceiver(
    //            @NotNull DeclarationDescriptorWithVisibility what,
    //            @NotNull DeclarationDescriptor from,
    //            boolean useSpecialRulesForPrivateSealedConstructors
    //    ) {
    //        return findInvisibleMember(ALWAYS_SUITABLE_RECEIVER, what, from, useSpecialRulesForPrivateSealedConstructors) == null;
    //    }
    //
    //    public static boolean isVisibleWithAnyReceiver(
    //            @NotNull DeclarationDescriptorWithVisibility what,
    //            @NotNull DeclarationDescriptor from,
    //            boolean useSpecialRulesForPrivateSealedConstructors
    //    ) {
    //        return findInvisibleMember(IRRELEVANT_RECEIVER, what, from, useSpecialRulesForPrivateSealedConstructors) == null;
    //    }
    @JvmStatic
    fun compare(first: DescriptorVisibility, second: DescriptorVisibility): Int? {
        val result = first.compareTo(second)
        if (result != null) {
            return result
        }
        val oppositeResult = second.compareTo(first)
        if (oppositeResult != null) {
            return -oppositeResult
        }
        return null
    }

    @JvmStatic
    fun isVisibleIgnoringReceiver(
        what: DeclarationDescriptor,
        from: DeclarationDescriptor,
        useSpecialRulesForPrivateSealedConstructors: Boolean
    ): Boolean {
        return findInvisibleMember(
            ALWAYS_SUITABLE_RECEIVER,
            what,
            from,
            useSpecialRulesForPrivateSealedConstructors
        ) == null
    }

    fun isVisibleWithAnyReceiver(
        what: DeclarationDescriptor,
        from: DeclarationDescriptor,
        useSpecialRulesForPrivateSealedConstructors: Boolean
    ): Boolean {
        return findInvisibleMember(IRRELEVANT_RECEIVER, what, from, useSpecialRulesForPrivateSealedConstructors) == null
    }

    fun isVisible(
        receiver: ReceiverValue?,
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        useSpecialRulesForPrivateSealedConstructors: Boolean
    ): Boolean {
        return findInvisibleMember(receiver, what, from, useSpecialRulesForPrivateSealedConstructors) == null
    }

    //    @Nullable
    //    public static DeclarationDescriptorWithVisibility findInvisibleMember(
    //            @Nullable ReceiverValue receiver,
    //            @NotNull DeclarationDescriptorWithVisibility what,
    //            @NotNull DeclarationDescriptor from,
    //            boolean useSpecialRulesForPrivateSealedConstructors
    //    ) {
    //        DeclarationDescriptorWithVisibility parent = (DeclarationDescriptorWithVisibility) what.getOriginal();
    //        while (parent != null && parent.getVisibility() != LOCAL) {
    //            if (!parent.getVisibility().isVisible(receiver, parent, from, useSpecialRulesForPrivateSealedConstructors)) {
    //                return parent;
    //            }
    //            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
    //        }
    //
    //        if (what instanceof TypeAliasConstructorDescriptor) {
    //            DeclarationDescriptorWithVisibility invisibleUnderlying =
    //                    findInvisibleMember(
    //                            receiver,
    //                            ((TypeAliasConstructorDescriptor) what).getUnderlyingConstructorDescriptor(),
    //                            from,
    //                            useSpecialRulesForPrivateSealedConstructors
    //                    );
    //            return invisibleUnderlying;
    //        }
    //
    //        return null;
    //    }
    fun findInvisibleMember(
        receiver: ReceiverValue?,
        what: DeclarationDescriptor,
        from: DeclarationDescriptor,
        useSpecialRulesForPrivateSealedConstructors: Boolean
    ): DeclarationDescriptor? {
        var parent: DeclarationDescriptor? = what.original
        while (parent != null && parent.visibility !== LOCAL) {
            if (!parent.visibility.isVisible(receiver, parent, from, useSpecialRulesForPrivateSealedConstructors)) {
                return parent
            }
            parent = DescriptorUtils.getParentOfType<DeclarationDescriptorWithVisibility?>(
                parent,
                DeclarationDescriptorWithVisibility::class.java
            )
        }

        if (what is TypeAliasConstructorDescriptor) {
            val invisibleUnderlying =
                findInvisibleMember(
                    receiver,
                    what.underlyingConstructorDescriptor,
                    from,
                    useSpecialRulesForPrivateSealedConstructors
                )
            return invisibleUnderlying
        }

        return null
    }

    @JvmStatic
    fun isPrivate(visibility: DescriptorVisibility): Boolean {
        return visibility === PRIVATE || visibility === PRIVATE_TO_THIS
    }

    fun toDescriptorVisibility(visibility: Visibility): DescriptorVisibility {
        val correspondingVisibility: DescriptorVisibility = visibilitiesMapping.get(visibility)!!
        requireNotNull(correspondingVisibility) { "Inapplicable visibility: " + visibility }
        return correspondingVisibility
    }
}
