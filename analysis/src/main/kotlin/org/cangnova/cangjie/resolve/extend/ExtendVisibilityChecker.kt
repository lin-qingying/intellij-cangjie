/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.extend

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.descriptors.PackageViewDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.resolve.extend.ExtendManager
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.resolve.scopes.HierarchicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.isPrimitiveType

/**
 * 扩展可见性检查器
 *
 * 提供统一的扩展可见性检查逻辑，被 TowerResolver、ExtendMemberAccessibilityChecker
 * 和遮蔽检查（checkExtendMemberShadowing）共同使用。
 *
 * ## 仓颉语言的扩展可访问性规则
 *
 * 扩展的可访问性规则决定了 extend 声明及其成员能否在其他包中被导入和使用。
 * 核心逻辑在编译器的 ExtendDecl::IsExportedDecl() 与 ImportManager::IsExtendAccessible/IsExtendMemberAccessible 中实现。
 *
 * ### 1. 扩展是否导出的判定（IsExportedDecl）
 *
 * #### 直接扩展（无继承接口）
 * - 在 `std.core` 包中：直接导出
 * - 与被扩展类型同包：导出性取决于被扩展类型与泛型上界的最小访问级别
 * - 跨包：永不导出，仅本包可见
 *
 * #### 接口扩展（有继承接口）
 * - 同包：随被扩展类型一起导出，不受接口访问级别影响
 * - 跨包：导出性取决于接口类型与泛型上界的最小访问级别
 *
 * #### 泛型上界检查
 * - 若存在泛型约束，所有上界类型必须可导出，否则整体不导出
 *
 * ### 2. 扩展在目标文件中的可访问性（IsExtendAccessible）
 *
 * - 同包或测试包特殊规则下直接可访问
 * - 跨包需满足：
 *   1. 所有泛型上界类型已导入
 *   2. 至少一个继承接口已导入（接口扩展）
 *   3. 被扩展类型可访问
 * - 最终可访问性还需扩展本身被导出
 *
 * ### 3. 扩展成员的可访问性（IsExtendMemberAccessible）
 *
 * - 同包直接可访问
 * - 跨包需满足：
 *   1. 扩展可访问（调用 IsExtendAccessible）
 *   2. 成员本身被导出（IsExportedDecl）
 *   3. 成员所属接口已导入（对接口扩展）
 *
 * ### 4. 编译器对扩展外部属性的设置（SetExtendExternalAttr）
 *
 * - 根据被扩展类型与泛型上界的访问级别，取最小值作为扩展的外部属性（访问级别）
 * - 跨包时还需考虑继承接口的访问级别，取被扩展类型与所有接口的最大值，再与泛型上界取最小值
 *
 * ### 5. 查找时的访问控制（FieldLookupExtend）
 *
 * - 在字段查找时，仅当扩展对目标文件可访问时，其成员才会被加入候选结果
 *
 * ## 本实现的规则
 *
 * 此实现完整实现了跨包访问时的可见性检查，核心规则如下：
 *
 * 1. **如果 extend 与观察点在同一个包** → 始终可访问
 *    - 对应编译器规则：同包情况下的直接可访问性
 *
 * 2. **如果 extend 在 std.core 包中** → 始终可访问
 *    - 对应编译器规则：std.core 包中的直接扩展也导出
 *
 * 3. **如果 extend 没有实现接口（直接扩展）且跨包** → 不可访问
 *    - 对应编译器规则：直接扩展跨包永不导出，仅本包可见
 *
 * 4. **检查泛型上界类型** → 所有泛型上界类型必须在当前作用域中可访问
 *    - 对应编译器规则：泛型约束检查
 *
 * 5. **检查被扩展类型** → 被扩展类型必须在当前作用域中可访问
 *    - 对应编译器规则：被扩展类型可访问性检查
 *
 * 6. **如果 extend 实现了接口（接口扩展）且跨包** → 必须在当前作用域中导入至少一个接口
 *    - 对应编译器规则：跨包接口扩展需要接口已导入
 *
 * ### 未实现的规则（待完善）
 *
 * - 测试包特殊规则
 *
 * ## 注意事项
 *
 * - 扩展成员（如 FuncDecl、PropDecl）的 IsExportedDecl 也会根据其所在扩展的规则进行覆盖判断
 * - 诊断提示（AddNote）会在扩展或其成员不可访问时给出具体的导入建议
 * - 对不可变类型的扩展有额外限制（如禁止赋值下标操作符、可变属性），但与可访问性规则相对独立
 *
 * ## 使用场景
 *
 * - **TowerResolver**：在名称解析时过滤不可访问的扩展
 * - **ExtendMemberAccessibilityChecker**：在使用扩展成员时检查访问权限
 * - **checkExtendMemberShadowing**：在遮蔽检查时排除不可见的扩展，避免误报
 *
 * @see org.cangnova.cangjie.resolve.calls.tower.TowerResolver
 * @see org.cangnova.cangjie.resolve.extend.ExtendMemberAccessibilityChecker
 */
object ExtendVisibilityChecker {

    private val LOG = Logger.getInstance(ExtendVisibilityChecker::class.java)



    /**
     * 检查扩展在当前作用域中是否可访问
     *
     * 根据仓颉语言扩展可访问性规则：
     * 1. **同包可见**：extend 与观察点在同一个包时，始终可见
     * 2. **std.core 包特殊处理**：std.core 包中的扩展始终导出
     * 3. **跨包直接扩展不可见**：没有实现接口的 extend，跨包永不导出
     * 4. **泛型上界检查**：所有泛型上界类型必须可访问
     * 5. **被扩展类型检查**：被扩展类型必须可访问
     * 6. **跨包接口扩展**：需要至少一个接口在当前作用域中已导入
     *
     * @param extendDescriptor 扩展描述符
     * @param scope 当前词法作用域（观察点）
     * @return 如果扩展可访问返回 true，否则返回 false
     */
    fun isExtendAccessible(
        extendDescriptor: ExtendDescriptor,
        scope: LexicalScope
    ): Boolean {
        // 获取 extend 声明所在的包
        val extendPackage = extendDescriptor.containingDeclaration.fqNameSafe

        // 获取观察点（scope）所在的包
        val observerPackage = getPackageFromScope(scope)

        if (LOG.isDebugEnabled) {
            LOG.debug(
                "isExtendAccessible: extendId='${extendDescriptor.extendId}', " +
                        "extendPackage=$extendPackage, observerPackage=$observerPackage"
            )
        }

        // 规则 1: 同包始终可见
        if (extendPackage == observerPackage) {
            if (LOG.isDebugEnabled) {
                LOG.debug("isExtendAccessible: '${extendDescriptor.extendId}' - same package, returning true")
            }
            return true
        }

        // 跨包情况
        val interfaces = extendDescriptor.superTypes

        if (LOG.isDebugEnabled) {
            LOG.debug(
                "isExtendAccessible: '${extendDescriptor.extendId}' - cross-package, " +
                        "interfaces.size=${interfaces.size}"
            )
        }

        // 规则 2: std.core 包中的扩展始终导出（直接扩展也可见）
        if (extendPackage == StandardNames.FqNames.core) {
            if (LOG.isDebugEnabled) {
                LOG.debug("isExtendAccessible: '${extendDescriptor.extendId}' - in std.core, returning true")
            }
            return true
        }

        // 规则 3: 直接扩展（无接口）跨包永不导出
        if (interfaces.isEmpty()) {
            if (LOG.isDebugEnabled) {
                LOG.debug("isExtendAccessible: '${extendDescriptor.extendId}' - no interfaces, cross-package, returning false")
            }
            return false
        }

        // 规则 4: 检查所有泛型上界类型是否可访问
        if (!areAllTypeParameterBoundsAccessible(extendDescriptor, scope)) {
            if (LOG.isDebugEnabled) {
                LOG.debug("isExtendAccessible: '${extendDescriptor.extendId}' - type parameter bounds not accessible, returning false")
            }
            return false
        }

        // 规则 5: 检查被扩展类型是否可访问
        if (!isExtendedTypeAccessible(extendDescriptor, scope)) {
            if (LOG.isDebugEnabled) {
                LOG.debug("isExtendAccessible: '${extendDescriptor.extendId}' - extended type not accessible, returning false")
            }
            return false
        }

        // 规则 6: 接口扩展，检查是否有至少一个接口在当前作用域中可见
        val result = interfaces.any { interfaceType ->
            val interfaceDescriptor = interfaceType.constructor.declarationDescriptor ?: return@any false
            val accessible = isClassifierAccessibleInScope(interfaceDescriptor, scope)
            if (LOG.isDebugEnabled) {
                LOG.debug(
                    "isExtendAccessible: '${extendDescriptor.extendId}' - " +
                            "checking interface ${interfaceDescriptor.fqNameSafe}, accessible=$accessible"
                )
            }
            accessible
        }

        if (LOG.isDebugEnabled) {
            LOG.debug("isExtendAccessible: '${extendDescriptor.extendId}' - final result=$result")
        }
        return result
    }

    /**
     * 从词法作用域中获取所在包的 FqName
     *
     * 通过遍历 ownerDescriptor 的 containingDeclaration 链，找到 PackageFragmentDescriptor。
     *
     * @param scope 词法作用域
     * @return 包的完全限定名
     */
    private fun getPackageFromScope(scope: LexicalScope): FqName {
        var descriptor: DeclarationDescriptor? = scope.ownerDescriptor
        while (descriptor != null) {
            when (descriptor) {
                is PackageFragmentDescriptor -> return descriptor.fqName
                is PackageViewDescriptor -> return descriptor.fqName
            }
            descriptor = descriptor.containingDeclaration
        }
        return FqName.ROOT
    }

    /**
     * 检查扩展定义在当前作用域中是否可访问
     *
     * 这是一个便捷方法，从 ExtensionDef 中提取 ExtendDescriptor 并进行检查。
     *
     * @param extensionDef 扩展定义
     * @param scope 当前词法作用域
     * @return 如果扩展可访问返回 true，如果无法获取 descriptor 则返回 false
     */
    fun isExtendAccessible(
        extensionDef: ExtendManager.ExtensionDef,
        scope: LexicalScope
    ): Boolean {
        val extendDescriptor = extensionDef.descriptor as? ExtendDescriptor
        if (extendDescriptor == null) {
            if (LOG.isDebugEnabled) {
                LOG.debug("isExtendAccessible: '${extensionDef.id}' - descriptor is not ExtendDescriptor, returning false")
            }
            // 无法获取 ExtendDescriptor 时，保守地返回 false（不可访问）
            return false
        }
        return isExtendAccessible(extendDescriptor, scope)
    }

    /**
     * 检查指定分类器（类/接口）在作用域中是否可访问
     *
     * 遍历完整的作用域链（包括 LexicalScope 和 ImportingScope），检查目标分类器是否
     * 通过 import 导入或在作用域中可见。验证找到的分类器的 FqName 是否与期望的分类器匹配。
     *
     * 这解决了默认导入（如 std.core.*）可能包含同名分类器的问题。
     * 例如，如果 std.core 中有一个名为 "Printable" 的类型，而用户定义的
     * untitled89.a.Printable 接口未被导入，我们不应该仅因为名称匹配就认为可见。
     *
     * @param expectedClassifier 期望的分类器描述符
     * @param scope 当前词法作用域
     * @return 如果正确的分类器可访问返回 true
     */
    fun isClassifierAccessibleInScope(
        expectedClassifier: ClassifierDescriptor,
        scope: LexicalScope
    ): Boolean {
        if(expectedClassifier.defaultType.isPrimitiveType()) return true
        val expectedFqName = expectedClassifier.fqNameSafe

        var currentScope: HierarchicalScope? = scope
        while (currentScope != null) {
            val classifier = currentScope.getContributedClassifier(
                expectedClassifier.name,
                NoLookupLocation.FROM_IDE
            )
            if (classifier != null && classifier.fqNameSafe == expectedFqName) {
                return true
            }
            currentScope = currentScope.parent
        }
        return false
    }

    /**
     * 兼容性别名，保留原方法名供外部调用
     */
    fun isInterfaceAccessibleInScope(
        expectedInterface: ClassifierDescriptor,
        scope: LexicalScope
    ): Boolean = isClassifierAccessibleInScope(expectedInterface, scope)

    /**
     * 检查扩展的所有泛型上界类型是否在作用域中可访问
     *
     * 遍历扩展声明的所有类型参数，检查每个类型参数的所有上界类型是否都可访问。
     *
     * @param extendDescriptor 扩展描述符
     * @param scope 当前词法作用域
     * @return 如果所有上界类型都可访问返回 true
     */
    private fun areAllTypeParameterBoundsAccessible(
        extendDescriptor: ExtendDescriptor,
        scope: LexicalScope
    ): Boolean {
        for (typeParam in extendDescriptor.declaredTypeParameters) {
            for (upperBound in typeParam.upperBounds) {
                if (!isTypeAccessibleInScope(upperBound, scope)) {
                    if (LOG.isDebugEnabled) {
                        LOG.debug(
                            "areAllTypeParameterBoundsAccessible: '${extendDescriptor.extendId}' - " +
                                    "upper bound ${upperBound} not accessible"
                        )
                    }
                    return false
                }
            }
        }
        return true
    }

    /**
     * 检查被扩展类型是否在作用域中可访问
     *
     * @param extendDescriptor 扩展描述符
     * @param scope 当前词法作用域
     * @return 如果被扩展类型可访问返回 true
     */
    private fun isExtendedTypeAccessible(
        extendDescriptor: ExtendDescriptor,
        scope: LexicalScope
    ): Boolean {
        return isTypeAccessibleInScope(extendDescriptor.extendType, scope)
    }

    /**
     * 检查类型是否在作用域中可访问
     *
     * 从类型中提取分类器描述符，并检查其可访问性。
     * 如果无法获取分类器描述符（例如原始类型），保守地认为可访问。
     *
     * @param type 要检查的类型
     * @param scope 当前词法作用域
     * @return 如果类型可访问返回 true
     */
    private fun isTypeAccessibleInScope(
        type: CangJieType,
        scope: LexicalScope
    ): Boolean {
        val typeDescriptor = type.constructor.declarationDescriptor as? ClassifierDescriptor
            ?: return true // 无法获取分类器描述符时（如原始类型），保守地认为可访问

        return isClassifierAccessibleInScope(typeDescriptor, scope)
    }
}