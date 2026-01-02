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

package org.cangnova.cangjie.resolve

import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.DiagnosticFactory1
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.reportOnDeclaration
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.BodiesResolveContext
import org.cangnova.cangjie.resolve.qualified.isEnum
import org.cangnova.cangjie.resolve.scopes.MemberScope

@DefaultImplementation(impl = ConflictingOverloadsDispatcher.Default::class)
interface ConflictingOverloadsDispatcher {
    fun getDiagnostic(
        languageVersionSettings: LanguageVersionSettings,
        declaration: DeclarationDescriptor,
        redeclarations: Collection<DeclarationDescriptor>
    ): DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>>?

    object Default : ConflictingOverloadsDispatcher {
        override fun getDiagnostic(
            languageVersionSettings: LanguageVersionSettings,
            declaration: DeclarationDescriptor,
            redeclarations: Collection<DeclarationDescriptor>
        ): DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>>? {
            return when (declaration) {
                is PropertyDescriptor, is ClassifierDescriptor -> REDECLARATION
                is FunctionDescriptor -> CONFLICTING_OVERLOADS
                else -> null
            }
        }
    }
}

class OverloadResolver(
    private val trace: BindingTrace,
    private val overloadFilter: OverloadFilter,
    private val overloadChecker: OverloadChecker,
    private val errorDispatcher: ConflictingOverloadsDispatcher,
    private val languageVersionSettings: LanguageVersionSettings,
//    mainFunctionDetectorFactory: MainFunctionDetector.Factory
) {


    /**
     * 在嵌套类和类型别名中查找构造函数
     * 此函数旨在收集嵌套类的构造函数，以便进行重载名称检查
     * 它跳过单例对象和匿名对象的构造函数，因为这些构造函数在代码中不可调用
     * 还注释掉了与类型别名相关的代码，可能是出于简化逻辑或当前上下文不需要处理类型别名的考虑
     *
     * @param c 解析上下文，包含声明的类和类型别名的信息
     * @return 返回一个MultiMap，其中包含类描述符作为键，以及相应的构造函数描述符集合作为值
     */
    private fun findConstructorsInNestedClassesAndTypeAliases(c: BodiesResolveContext): MultiMap<ClassDescriptor, FunctionDescriptor> {
        // 创建一个MultiMap，用于存储外层类描述符与内层类构造函数描述符的映射
        val constructorsByOuterClass = MultiMap.create<ClassDescriptor, FunctionDescriptor>()

        // 遍历解析上下文中声明的所有类
        for (cclass in c.declaredClasses.values) {
            // 跳过枚举类或匿名对象的构造函数，因为它们在代码中不可调用，不应参与重载名称检查
            // 枚举构造器通过 EnumConstructorDescriptor 表示，不是普通构造函数
            if (cclass.isEnum || cclass.name.isSpecial) {
                continue
            }
            // 获取当前类所在的包含声明
            val containingDeclaration = cclass.containingDeclaration
            // 如果包含声明是一个类描述符，则将当前类的构造函数添加到结果MultiMap中
            if (containingDeclaration is ClassDescriptor) {
                constructorsByOuterClass.putValues(containingDeclaration, cclass.constructors)
            } else if (!(containingDeclaration is FunctionDescriptor ||
                        containingDeclaration is PropertyDescriptor ||
                        containingDeclaration is PackageFragmentDescriptor)
            ) {
                // 如果包含声明不是预期的类型之一，则抛出异常
                throw IllegalStateException("Illegal class container: $containingDeclaration")
            }
        }

        // 以下代码段被注释掉，旨在处理类型别名的构造函数，但目前未启用
//        for (typeAlias in c.typeAliases.values) {
//            val containingDeclaration = typeAlias.containingDeclaration
//            if (containingDeclaration is ClassDescriptor) {
//                constructorsByOuterClass.putValues(containingDeclaration, typeAlias.constructors)
//            }
//        }

        // 返回收集到的构造函数映射
        return constructorsByOuterClass
    }


    /**
     * 检查重载函数和构造函数的正确性
     * 此函数旨在确保在嵌套类和类型别名中声明的构造函数以及包级别的函数没有重载问题
     * 它通过分析已声明的类和构造函数来实现这一点
     *
     * @param c 解析上下文，包含了需要检查的所有必要信息
     */
    fun checkOverloads(c: BodiesResolveContext) {
        // 查找嵌套类和类型别名中的构造函数
        val inClasses = findConstructorsInNestedClassesAndTypeAliases(c)

        // 遍历所有已声明的类，检查每个类中的重载构造函数
        for (value in c.declaredClasses.values) {
           if(value is ClassDescriptor)
               checkOverloadsInClass(value, inClasses.get(value))
        }
        // 检查包级别的重载函数
        checkOverloadsInPackages(c)
    }

    /**
     * 获取与给定描述符同名的模块包成员
     *
     * 此函数旨在检索与给定描述符[descriptor]同名的所有模块包成员它首先确定描述符所属的包，
     * 然后在该包的范围内查找所有同名的成员，并根据模块上下文进行过滤，最后返回经过重载过滤器
     * [overloadFilter]处理的成员集合
     *
     * @param descriptor 描述符，表示需要查找同名成员的元素
     * @param overloadFilter 重载过滤器，用于过滤同名成员，以便处理重载情况
     * @param getMembersByName 一个高阶函数，用于从指定作用域中获取指定名称的成员描述符集合
     * @return 返回一个包含与给定描述符同名的模块包成员的集合
     * @throws AssertionError 如果描述符不是顶级包成员，或者描述符种类不符合预期
     */
    private inline fun getModulePackageMembersWithSameName(
        descriptor: DeclarationDescriptor,
        overloadFilter: OverloadFilter,
        getMembersByName: (MemberScope, Name) -> Collection<DeclarationDescriptorNonRoot>
    ): Collection<DeclarationDescriptorNonRoot> {
        // 初始化包含包变量，用于后续确定描述符所属的包
        var containingPackage = descriptor.containingDeclaration

        // 确保描述符是一个包片段描述符，否则抛出异常
        if (containingPackage !is PackageFragmentDescriptor) {
            throw AssertionError("$descriptor is not a top-level package member")
        }

        // 获取描述符所属的模块，如果不存在，则根据描述符类型返回相应的默认值
        val containingModule = DescriptorUtils.getContainingModuleOrNull(descriptor) ?: return when (descriptor) {
            is CallableMemberDescriptor -> listOf(descriptor)
            is ClassDescriptor -> descriptor.constructors
            else -> throw AssertionError("Unexpected descriptor kind: $descriptor")
        }

        // 获取包含包的作用域
        val containingPackageScope = containingModule.getPackage(
            containingPackage.fqName
        ).memberScope
        // 在包作用域内查找所有同名成员，并过滤出属于当前模块的成员
        val possibleOverloads =
            getMembersByName(containingPackageScope, descriptor.name).filter {
                // NB memberScope for PackageViewDescriptor includes module dependencies
                DescriptorUtils.getContainingModule(it) == containingModule
            }

        // 使用重载过滤器处理同名成员，并返回结果
        return overloadFilter.filterPackageMemberOverloads(possibleOverloads)
    }

    /**
     * 收集具有相同名称的模块包成员
     *
     * 此函数旨在收集属于特定模块的包成员中与给定描述符具有相同名称的所有成员
     * 它避免重复收集全限定名相同的成员，并且只关注包含在包片段描述符或惰性扩展类描述符中的描述符
     *
     * @param packageMembersByName 一个映射，将全限定名与一组具有相同名称的声明描述符关联起来
     * @param interestingDescriptors 一组描述符，用作收集过程的起点
     * @param overloadFilter 用于过滤重载成员的过滤器
     * @param getMembersByName 一个函数，用于从成员作用域中获取具有特定名称的声明描述符集合
     */
    private inline fun collectModulePackageMembersWithSameName(
        packageMembersByName: MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot>,
        interestingDescriptors: Collection<DeclarationDescriptor>,
        overloadFilter: OverloadFilter,
        getMembersByName: (MemberScope, Name) -> Collection<DeclarationDescriptorNonRoot>
    ) {
        // 用于跟踪已经观察到的全限定名，以避免重复处理
        val observedFQNs = hashSetOf<FqNameUnsafe>()
        for (descriptor in interestingDescriptors) {
            if (descriptor.containingDeclaration !is PackageFragmentDescriptor) continue


            val descriptorFQN = DescriptorUtils.getFqName(descriptor)
            // 如果当前描述符的全限定名已经观察到，则跳过，避免重复处理
            if (observedFQNs.contains(descriptorFQN)) continue
            observedFQNs.add(descriptorFQN)

            // 获取与当前描述符名称相同的模块包成员，并将其添加到映射中
            val packageMembersWithSameName =
                getModulePackageMembersWithSameName(descriptor, overloadFilter, getMembersByName)
            packageMembersByName.putValues(descriptorFQN, packageMembersWithSameName)
        }
    }

    /**
     * 根据完全限定名对模块包成员进行分组
     *
     * 此函数的目的是将模块中具有相同名称的包成员（如函数、类、变量等）根据它们的完全限定名进行分组
     * 这有助于在解决重载、检查声明冲突等问题时，能够更方便地对这些成员进行处理
     *
     * @param c 解析上下文，包含了需要处理的模块信息
     * @param overloadFilter 重载过滤器，用于处理具有相同名称的声明
     * @return 返回一个多映射，其中键是完全限定名，值是对应的声明描述符列表
     */
    private fun groupModulePackageMembersByFqName(
        c: BodiesResolveContext,
        overloadFilter: OverloadFilter
    ): MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot> {
        // 创建一个多映射，用于存储按完全限定名分组的包成员
        val packageMembersByName = MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot>()

        // 收集模块中具有相同名称的函数、类和类型别名，并根据重载过滤器进行处理
        collectModulePackageMembersWithSameName(
            packageMembersByName,
            (c.functions.values as Collection<DeclarationDescriptor>) + c.declaredClasses.values + c.typeAliases.values,
            overloadFilter
        ) { scope, name ->
            // 根据名称获取作用域中的函数和分类器（类或类型别名）
            val functions = scope.getContributedFunctions(name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            val classifier = scope.getContributedClassifier(name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            // 根据分类器的类型，决定返回的声明列表
            when (classifier) {
                is ClassDescriptor ->

                        functions + classifier.constructors

                is TypeAliasDescriptor ->
                    functions + classifier.constructors

                else ->
                    functions
            }
        }


        // 收集模块中具有相同名称的变量，并根据重载过滤器进行处理
        collectModulePackageMembersWithSameName(
            packageMembersByName,
            c.variables.values.flatten(),
            overloadFilter
        ) { scope, name ->
            // 根据名称获取作用域中的变量和分类器
            val variables = scope.getContributedVariables(name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            val classifier = scope.getContributedClassifier(name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            // 将变量和分类器组合成声明列表
            variables + listOfNotNull(classifier)
        }

        // 返回按完全限定名分组的包成员多映射
        return packageMembersByName
    }

    /**
     * 在给定的上下文中检查包中的重载成员
     * 此函数通过将模块包成员按完全限定名分组，并对每个分组调用检查重载的方法
     * 以确保在同一个包中没有重载冲突
     *
     * @param c 一个包含解析信息的上下文对象，用于获取和解析模块包成员的信息
     */
    private fun checkOverloadsInPackages(c: BodiesResolveContext) {
        // 将模块包成员按完全限定名分组，以便后续处理
        val membersByName = groupModulePackageMembersByFqName(c, overloadFilter)

        // 遍历每个分组，检查其中的重载成员
        for (e in membersByName.entrySet()) {
            checkOverloadsInPackage(e.value)
        }
    }

    /**
     * 判断声明描述符是否为私有
     *
     * 此方法扩展了DeclarationDescriptor的功能，用于检查一个声明描述符是否被标记为私有
     * 它首先检查当前描述符是否具有可见性属性，然后进一步检查其可见性是否为私有
     *
     * @return Boolean 如果描述符是私有的，则返回true；否则返回false
     */
    private fun DeclarationDescriptor.isPrivate() =
        this is DeclarationDescriptorWithVisibility &&
                DescriptorVisibilities.isPrivate(this.visibility)

    /**
     * 获取可能的重新声明组集合。
     * 该函数旨在对给定的一组成员描述符进行分析，找出其中可能存在的重新声明组。
     * 重新声明组是由那些可能在相同作用域内被重新声明的成员组成的集合。
     *
     * @param members 非根声明描述符的集合，代表一组成员。
     * @return 返回一个集合，其中每个元素都是可能的重新声明组，每个组都是声明描述符的集合。
     */
    private fun getPossibleRedeclarationGroups(members: Collection<DeclarationDescriptorNonRoot>): Collection<Collection<DeclarationDescriptorNonRoot>> {
        // 初始化结果集合，用于存储可能的重新声明组。
        val result = arrayListOf<Collection<DeclarationDescriptorNonRoot>>()

        // 过滤出非私有成员，因为私有成员的重新声明不会引起问题。
        val nonPrivates = members.filter { !it.isPrivate() }

        // 根据成员所在的源文件对成员进行分组，以便后续处理。
        val bySourceFile = members.groupBy { DescriptorUtils.getContainingSourceFile(it) }

        // 标记是否有一个包含非私有成员的组，初始值为false。
        var hasGroupIncludingNonPrivateMembers = false
        // 遍历每个源文件对应的成员组。
        for (membersInFile in bySourceFile.values) {
            // 文件成员组在重新声明检查中是有趣的，如果至少有一个文件成员是私有的。
            if (membersInFile.any { it.isPrivate() }) {
                // 表示已找到至少包含一个私有成员的组。
                hasGroupIncludingNonPrivateMembers = true
                // 构建当前文件成员组与非私有成员的并集，并添加到结果中。
                val group = LinkedHashSet<DeclarationDescriptorNonRoot>(nonPrivates) + membersInFile
                result.add(group)
            }
        }

        // 如果没有找到包含非私有成员的组，且非私有成员数量大于1，则将非私有成员作为一组添加到结果中。
        if (!hasGroupIncludingNonPrivateMembers && nonPrivates.size > 1) {
            result.add(nonPrivates)
        }

        // 返回构建的可能的重新声明组集合。
        return result
    }


    /**
     * 判断一个声明描述符是否是合成的
     * 合成的成员是指由编译器生成的成员，而不是由源代码直接定义的
     * 这在处理某些语言特性时很有用，比如属性的getter和setter，它们是基于属性声明合成的
     *
     * @return 如果声明描述符是一个合成的成员描述符，则返回true，否则返回false
     */
    private fun DeclarationDescriptor.isSynthesized() =
        this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED

    /**
     * 在给定的声明描述符集合中查找重新声明的元素
     * 重新声明是指在同一个作用域内，具有相同名称和相同参数的多个声明
     * 这个函数的目的是识别并返回这些可能引起冲突的声明
     *
     * @param members 一个包含声明描述符的集合，表示在一个作用域内定义的所有成员
     * @return 返回一个集合，包含被认为是重新声明的成员
     */
    private fun findRedeclarations(members: Collection<DeclarationDescriptorNonRoot>): Collection<DeclarationDescriptorNonRoot> {
        // 使用 LinkedHashSet 来存储重新声明的元素，以保持插入顺序
        val redeclarations = linkedSetOf<DeclarationDescriptorNonRoot>()

        // 遍历所有成员，检查是否有重新声明的情况
        for (member1 in members) {
            // 如果当前成员是合成的，则跳过，因为合成成员不考虑为重新声明
            if (member1.isSynthesized()) continue

            // 再次遍历所有成员，与当前成员进行比较
            for (member2 in members) {
                // 如果两个成员相同，则跳过，避免自我比较
                if (member1 == member2) continue

                // 以下是一些特定情况的检查，如果满足这些情况，则认为不是重新声明
                // 例如：不同文件中的顶级 main 函数、不同平台的定义等
                // 这些特定情况的检查被注释掉了，可能是由于它们在当前上下文中不适用或尚未实现

                // 如果两个成员不是重载关系，则将 member1 添加到重新声明的集合中
                // 这里的重载检查是通过 overloadChecker 进行的，如果两个成员不能重载，则认为是重新声明
                if (!overloadChecker.isOverloadable(member1, member2)) {
                    redeclarations.add(member1)
                }
            }
        }

        // 返回所有找到的重新声明的成员集合
        return redeclarations
    }


    /**
     * 检查包中是否存在重载的函数或属性
     * 此函数旨在分析给定的声明描述符集合，识别并报告其中的重载声明
     * 重载声明指的是在同一作用域中具有相同名称但参数不同的函数或属性
     *
     * @param members 一个包含声明描述符的集合，通常代表一个包中的所有成员
     */
    fun checkOverloadsInPackage(members: Collection<DeclarationDescriptorNonRoot>) {
        // 如果成员数量少于两个，则无需检查重载
        if (members.size == 1) return

        // 创建一个映射，用于存储每个声明的重载声明集合
        val redeclarationsMap = LinkedHashMap<DeclarationDescriptorNonRoot, MutableSet<DeclarationDescriptorNonRoot>>()

        // 遍历可能的重声明组，对每个组进行处理
        for (redeclarationGroup in getPossibleRedeclarationGroups(members)) {
            // 找到当前组中的所有重声明
            val redeclarations = findRedeclarations(redeclarationGroup)
            // 将当前重声明添加到映射中，如果不存在，则初始化其重声明集合
            redeclarations.forEach {
                redeclarationsMap.getOrPut(it) { LinkedHashSet() }.addAll(redeclarations)
            }
        }

        // 创建一个集合，用于记录已经报告过的声明，避免重复报告
        val reported = HashSet<DeclarationDescriptorNonRoot>()

        // 遍历重声明映射，报告每个声明的重载情况
        for ((member, conflicting) in redeclarationsMap) {
            // 如果当前声明未被报告过，则报告其重声明情况，并将所有相关声明添加到已报告集合中
            if (!reported.contains(member)) {
                reported.addAll(conflicting)
                reportRedeclarations(conflicting)
            }
        }
    }

    /**
     * 报告重新声明的成员
     * 此函数遍历一组非根声明描述符，根据成员类型报告重新声明的错误
     * 它忽略枚举条目的构造函数，并对其他类型的成员进行错误报告
     *
     * @param redeclarations 一组非根声明描述符，表示可能的重新声明
     */
    private fun reportRedeclarations(redeclarations: Collection<DeclarationDescriptorNonRoot>) {
        // 如果没有重新声明，则直接返回
        if (redeclarations.isEmpty()) return

        // 遍历每个成员描述符以报告重新声明
        for (memberDescriptor in redeclarations) {
            // 根据成员描述符的类型执行不同的操作
            when (memberDescriptor) {


                is PropertyDescriptor,
                is VariableDescriptor,
                is ClassifierDescriptor,
                is FunctionDescriptor ->
                    // 报告这些成员的重新声明错误
                    reportOnDeclaration(trace, memberDescriptor) { REDECLARATION.on(it, redeclarations) }

                // 以下代码被注释掉，用于处理函数描述符的冲突重载错误
                // is FunctionDescriptor ->
                //     reportOnDeclaration(trace, memberDescriptor) {  CONFLICTING_OVERLOADS.on(it, redeclarations) }
            }
        }

        // 以下代码被注释掉，用于获取并报告更复杂的诊断信息
        // for (memberDescriptor in redeclarations) {
        //     val diagnostic = errorDispatcher.getDiagnostic(languageVersionSettings, memberDescriptor, redeclarations) ?: continue
        //     reportOnDeclaration(trace, memberDescriptor) {
        //         diagnostic.on(it, redeclarations)
        //     }
        // }
    }


    /**
     * 检查类中的方法重载
     * 此函数的目的是确保在类中重载的方法或构造函数在数量和类型上是有效的
     * 它通过收集所有同名的方法和构造函数，然后对每组重载进行有效性检查
     *
     * @param classDescriptor 描述类的接口，包含类的结构信息，如方法、属性和构造函数
     * @param nestedClassConstructors 嵌套类的构造函数集合，用于检查嵌套类的构造函数重载
     */
    private fun checkOverloadsInClass(
        classDescriptor:  DescriptorWithResolutionScopes,
        nestedClassConstructors: Collection<FunctionDescriptor>
    ) {
        // 创建一个多值映射，用于存储每个方法名对应的所有可调用成员描述符
        val callableMembersByName = MultiMap.create<Name, CallableMemberDescriptor>()

        // 遍历类中声明的所有可调用成员，将它们按名称分组
        for (callableMember in classDescriptor.declaredCallableMembers) {
            callableMembersByName.putValue(callableMember.name, callableMember)
        }
if(classDescriptor is ClassDescriptor){
    // 遍历类的所有构造函数，将它们按名称分组
    for (constructor in classDescriptor.endConstructors) {
        callableMembersByName.putValue(constructor.name, constructor)
    }
}


        // 遍历嵌套类的构造函数，将它们按嵌套类的名称分组
        for (nestedConstructor in nestedClassConstructors) {
            val name = nestedConstructor.containingDeclaration.name
            callableMembersByName.putValue(name, nestedConstructor)
        }

        // 遍历每个名称对应的可调用成员集合，检查重载的有效性
        for (e in callableMembersByName.entrySet()) {
            checkOverloadsInClass(e.value)
        }
    }

    /**
     * 检查类中的成员函数或属性是否具有重载
     * 此函数的目的是为了避免在类中出现不必要的重载，从而提高代码的清晰度和维护性
     *
     * @param members 成员函数或属性的集合通过这个集合来检查是否存在重载的情况
     */
    private fun checkOverloadsInClass(members: Collection<CallableMemberDescriptor>) {
        // 如果集合中只有一个成员，则无需检查重载，直接返回
        if (members.size == 1) return

        // 查找可能的重载声明，并报告它们
        // 这里假设findRedeclarations函数能够识别和返回重载的成员描述符
        reportRedeclarations(findRedeclarations(members))
    }
}
