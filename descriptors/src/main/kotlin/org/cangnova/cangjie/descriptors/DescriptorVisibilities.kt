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
package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.isSameModule
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.util.ModuleVisibilityHelper
import org.cangnova.cangjie.utils.newHashMapWithExpectedSize
import java.util.*
import java.util.Collections.unmodifiableMap

object DescriptorVisibilities {
    /**
     * 该值应在需要忽略接收器相关检查的情况下用于Visibility.isVisible的receiverValue参数
     */
    val ALWAYS_SUITABLE_RECEIVER: ReceiverValue = object : ReceiverValue {


        override val type: CangJieType
            get() = throw IllegalStateException("This method should not be called")

        override fun replaceType(newType: CangJieType): ReceiverValue {
            throw IllegalStateException("This method should not be called")
        }

        override val original: ReceiverValue
            get() = this

    }

    @JvmField
    val INHERITED: DescriptorVisibility = object : DelegatedDescriptorVisibility(Visibilities.Inherited) {
        public override fun isVisible(
            receiver: ReceiverValue?,
            what: DeclarationDescriptor,
            from: DeclarationDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean
        ): Boolean {
            throw IllegalStateException("Visibility is unknown yet") // 此方法不应在继承可见性(INHERITED)的情况下被调用
        }
    }

    /* 用于虚假覆盖不可见成员的可见性（为更好的错误报告创建） */
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
     * private可见性仅在当前文件内有效
     */
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
                if (useSpecialRulesForPrivateSealedConstructors && DescriptorUtils.isSealedClass(classDescriptor) && DescriptorUtils.isTopLevelDeclaration(
                        classDescriptor
                    ) && from is ConstructorDescriptor && DescriptorUtils.isTopLevelDeclaration(from.containingDeclaration) && inSameFile(
                        what,
                        from
                    )
                ) {
                    return true
                }
            }

            var parent: DeclarationDescriptor? = what
            while (parent != null) {
                parent = parent.containingDeclaration
                if ((parent is ClassDescriptor) || parent is PackageFragmentDescriptor) {
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
                    return parent is PackageFragmentDescriptor && parent.fqName.equals(fromParent.fqName) && DescriptorUtils.areInSameModule(
                        fromParent,
                        parent
                    )
                }
                fromParent = fromParent.containingDeclaration
            }
            return false
        }
    }

    //对所有地方可见
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


    /**
     * 访问修饰符规则
     * 文件        包 & 子包        模块        所有包
     * private     可见            不可见      不可见      不可见
     * internal    可见            可见        不可见      不可见
     * protected   可见            可见        可见        不可见
     */



    /**
     * 此可见性用于以下场景：
     * class A<T> {
     *   init(t: T){}
     *   private let t: T = t // t的可见性为PRIVATE_TO_THIS
     *   
     *   func test() {
     *     let x: T = t       // 正确
     *     let y: T = this.t  // 也正确
     *   }
     *   func foo(a: A<String>) {
     *     let x: String = a.t // 错误，因为a.t可能是Any
     *   }
     * }
     */
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

    // 当前用作FunctionDescriptor的默认可见性
    // 防止在调用`initialize`前请求非空可见性时出现NPE
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
    private val MODULE_VISIBILITY_HELPER: ModuleVisibilityHelper

    init {
        val iterator = ServiceLoader.load<ModuleVisibilityHelper?>(
            ModuleVisibilityHelper::class.java, ModuleVisibilityHelper::class.java.getClassLoader()
        ).iterator()
        MODULE_VISIBILITY_HELPER = if (iterator.hasNext()) iterator.next() else ModuleVisibilityHelper.EMPTY

    }

    // 在文件、包及子包中可见
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

    // 仅在模块内可见
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
     * 该值应在需要判断成员是否对任意接收器可见的情况下用于Visibility.isVisible的receiverValue参数
     */
    private val IRRELEVANT_RECEIVER: ReceiverValue = object : ReceiverValue {


        override val type: CangJieType
            get() = throw IllegalStateException("This method should not be called")


        override fun replaceType(newType: CangJieType): ReceiverValue {
            throw IllegalStateException("This method should not be called")
        }

        override val original: ReceiverValue
            get() = this

    }
    private val visibilitiesMapping: MutableMap<Visibility?, DescriptorVisibility> =
        HashMap<Visibility?, DescriptorVisibility>()
    private val ORDERED_VISIBILITIES: MutableMap<DescriptorVisibility, Int>

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
        val visibilities = newHashMapWithExpectedSize<DescriptorVisibility?, Int?>(4)
        visibilities.put(PRIVATE_TO_THIS, 0)
        visibilities.put(PRIVATE, 0)
        visibilities.put(INTERNAL, 1)
        visibilities.put(PROTECTED, 1)
        visibilities.put(PUBLIC, 2)
        ORDERED_VISIBILITIES = unmodifiableMap(visibilities)
    }

    private fun recordVisibilityMapping(visibility: DescriptorVisibility) {
        visibilitiesMapping.put(visibility.delegate, visibility)
    }

    // 注意：如果`from`声明是`init`初始化器，则返回false
    // 因为初始化器没有源元素
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
        what: DeclarationDescriptor, from: DeclarationDescriptor, useSpecialRulesForPrivateSealedConstructors: Boolean
    ): Boolean {
        return findInvisibleMember(
            ALWAYS_SUITABLE_RECEIVER, what, from, useSpecialRulesForPrivateSealedConstructors
        ) == null
    }

    fun isVisibleWithAnyReceiver(
        what: DeclarationDescriptor, from: DeclarationDescriptor, useSpecialRulesForPrivateSealedConstructors: Boolean
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
            parent = DescriptorUtils.getParentOfType(
                parent, DeclarationDescriptorWithVisibility::class.java
            )
        }

        if (what is TypeAliasConstructorDescriptor) {
            val invisibleUnderlying = findInvisibleMember(
                receiver, what.underlyingConstructorDescriptor, from, useSpecialRulesForPrivateSealedConstructors
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
