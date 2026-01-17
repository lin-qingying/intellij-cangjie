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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.EnumConstructorDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.components.InferenceSession

import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.resolve.scopes.LocalRedeclarationChecker
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.storage.StorageManager

class EnumDescriptorResolver(
    private val typeResolver: TypeResolver,
    private val builtIns: CangJieBuiltIns,
    private val storageManager: StorageManager

) {
    /**
     * 解析枚举构造器描述符
     *
     * 枚举构造器可以是：
     * 1. 简单构造器（无关联值）：如 `Red`, `Green`
     * 2. 函数构造器（有关联值）：如 `Success(T)`, `Error(String)`
     *
     * @param scope 词法作用域
     * @param enumDescriptor 所属枚举的描述符
     * @param entry 枚举构造器的 PSI 元素
     * @param trace 绑定跟踪器
     * @param languageVersionSettings 语言版本设置
     * @param inferenceSession 类型推断会话
     * @return 枚举构造器描述符
     */
    fun resolveEnumConstructorConstructorDescriptor(
        scope: LexicalScope,
        enumDescriptor: EnumDescriptor,
        entry: CjEnumConstructor,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        inferenceSession: InferenceSession?,
    ): EnumConstructorDescriptorImpl {

        // 创建参数作用域
        val parameterScope = LexicalWritableScope(
            scope,
            enumDescriptor,
            false,
            LocalRedeclarationChecker.DO_NOTHING,
            LexicalScopeKind.CONSTRUCTOR_HEADER
        )

        // 解析构造器参数类型
        val parameterTypes = entry.typeReferences.map {
            typeResolver.resolveType(parameterScope, it, trace, true)
        }

        // 创建枚举构造器描述符
        val constructorName = entry.name?.let { Name.identifier(it) } ?: Name.special("<anonymous>")
        val sourceElement = entry.toSourceElement()

        val constructorDescriptor = EnumConstructorDescriptorImpl(
            name = constructorName,
            containingDeclaration = enumDescriptor,
            original = null,
            annotations = Annotations.EMPTY, // TODO: 解析注解
            source = sourceElement
        )

        // 创建值参数描述符列表
        val valueParameters = parameterTypes.mapIndexed { index, paramType ->
            ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                containingDeclaration = constructorDescriptor,
                original = null,
                index = index,
                annotations = Annotations.EMPTY,
                name = Name.identifier("param$index"), // 枚举构造器参数通常是匿名的
                isNamed = false,
                outType = paramType,
                declaresDefaultValue = false,
                source = sourceElement
            )
        }

        // 初始化构造器描述符
        // 枚举构造器继承枚举的类型参数（如 Option<T> 的 Some(T) 继承 T）
        constructorDescriptor.initialize(
            dispatchReceiverParameter = null,
            typeParameters = enumDescriptor.declaredTypeParameters,
            unsubstitutedValueParameters = valueParameters,
            unsubstitutedReturnType = enumDescriptor.defaultType,
            modality = Modality.FINAL,

        )

        return constructorDescriptor
    }


}
