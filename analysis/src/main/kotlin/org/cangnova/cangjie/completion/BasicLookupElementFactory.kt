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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.completion.handlers.BaseDeclarationInsertHandler
import org.cangnova.cangjie.completion.handlers.DeclarationLookupObjectImpl
import org.cangnova.cangjie.completion.handlers.InsertHandlerProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.resolve.DescriptorUtils
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.icon.CangJieDescriptorIconProvider
import org.cangnova.cangjie.resolve.deprecation.unwrapIfFakeOverride
import org.cangnova.cangjie.resolve.source.MacroExpandedSourceElement
import org.cangnova.cangjie.utils.isExtension
import javax.swing.Icon

/**
 * 基础 LookupElement 工厂
 *
 * 作用：
 * 将编译器 Descriptor 转换为 IntelliJ Completion 使用的 LookupElement。
 *
 * 在 IntelliJ 自动补全系统中：
 *
 * Descriptor (语义符号)
 *        ↓
 * LookupElement (IDE补全项)
 *
 * 该类负责：
 * - 根据 Descriptor 类型构造补全项
 * - 渲染补全展示文本
 * - 设置补全插入行为 (InsertHandler)
 * - 设置图标
 * - 设置尾部文本（参数、类型、包名等）
 */
class BasicLookupElementFactory(
    private val project: Project,
    val insertHandlerProvider: InsertHandlerProvider
) {

    companion object {

        /**
         * 简短类型渲染器
         *
         * 用于补全列表中显示函数参数和返回类型
         *
         * 特点：
         * - 使用短名称
         * - 启用增强类型
         * - 不显示函数类型参数名（减少噪音）
         */
        val SHORT_NAMES_RENDERER = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions {
            enhancedTypes = true
            parameterNamesInFunctionalTypes = false
        }

        /**
         * 获取 Descriptor 对应的图标
         *
         * IntelliJ 的图标由 DescriptorIconProvider 提供
         *
         * 参数：
         * lookupObject : Lookup对象
         * descriptor   : 符号描述符
         * flags        : 图标标志位
         */
        private fun getIcon(
            lookupObject: DescriptorBasedDeclarationLookupObject,
            descriptor: DeclarationDescriptor,
            flags: Int
        ): Icon? {

            // ReceiverParameterDescriptor 没有实际 PSI 声明
            val declaration = when (descriptor) {
                is ReceiverParameterDescriptor -> null
                else -> lookupObject.psiElement
            }

            return CangJieDescriptorIconProvider.getIcon(descriptor, declaration, flags)
        }
    }

    /**
     * 创建补全项（主入口）
     *
     * unwrapIfFakeOverride：
     * 解决 Kotlin/仓颉中的 fake override 问题
     */
    fun createLookupElement(
        descriptor: DeclarationDescriptor,
        qualifyNestedClasses: Boolean = false,
        includeClassTypeArguments: Boolean = true,
        parametersAndTypeGrayed: Boolean = false
    ): LookupElement {

        return createLookupElementUnwrappedDescriptor(
            descriptor.unwrapIfFakeOverride(),
            qualifyNestedClasses,
            includeClassTypeArguments,
            parametersAndTypeGrayed
        )
    }

    /**
     * 为包创建补全项
     *
     * 示例：
     *   std.io
     *   std.math
     */
    fun createLookupElementForPackage(name: FqName): LookupElement {

        var element = LookupElementBuilder.create(
            PackageLookupObject(name),
            name.shortName().asString()
        )

        // 插入处理器
        element = element.withInsertHandler(BaseDeclarationInsertHandler())

        // 显示完整包名
        if (!name.parent().isRoot) {
            element = element.appendTailText(" (${name.asString()})", true)
        }

        return element.withIconFromLookupObject()
    }

    /**
     * 创建 Descriptor 对应的 LookupElement
     *
     * 根据 Descriptor 类型生成不同的补全展示。
     */
    private fun createLookupElementUnwrappedDescriptor(
        descriptor: DeclarationDescriptor,
        qualifyNestedClasses: Boolean,
        includeClassTypeArguments: Boolean,
        parametersAndTypeGrayed: Boolean
    ): LookupElement {

        // 包补全
        if (descriptor is PackageViewDescriptor) {
            return createLookupElementForPackage(descriptor.fqName)
        }

        if (descriptor is PackageFragmentDescriptor) {
            return createLookupElementForPackage(descriptor.fqName)
        }

        val lookupObject: DescriptorBasedDeclarationLookupObject

        /**
         * 补全项名称
         */
        val name: String = when (descriptor) {

            /**
             * 构造函数补全
             *
             * 构造函数显示类名，而不是 <init>
             */
            is ConstructorDescriptor -> {

                val classifierDescriptor = descriptor.containingDeclaration

                lookupObject = object : DeclarationLookupObjectImpl(descriptor) {

                    /**
                     * 获取 PSI 元素
                     */
                    override val psiElement by lazy {
                        DescriptorToSourceUtilsIde.getAnyDeclaration(
                            project,
                            classifierDescriptor
                        )
                    }

                    /**
                     * 使用类图标
                     */
                    override fun getIcon(flags: Int): Icon? =
                        getIcon(this, classifierDescriptor, flags)
                }

                classifierDescriptor.name.asString()
            }

            /**
             * 普通符号
             */
            else -> {

                lookupObject = object : DeclarationLookupObjectImpl(descriptor) {

                    /**
                     * 优先获取源码 PSI
                     */
                    override val psiElement by lazy {
                        DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)
                            ?: DescriptorToSourceUtilsIde.getAnyDeclaration(
                                project,
                                descriptor
                            )
                    }

                    override fun getIcon(flags: Int): Icon? =
                        getIcon(this, descriptor, flags)
                }

                descriptor.name.asString()
            }
        }

        /**
         * 创建 LookupElement
         */
        var element = LookupElementBuilder.create(lookupObject, name)

        /**
         * 设置插入处理器
         *
         * 插入处理器决定：
         * - 插入函数时是否加 ()
         * - 是否生成 lambda
         */
        val insertHandler = insertHandlerProvider.insertHandler(descriptor)
        element = element.withInsertHandler(insertHandler)

        /**
         * 根据 Descriptor 类型渲染不同补全展示
         */
        when (descriptor) {

            /**
             * 函数补全
             */
            is FunctionDescriptor -> {

                val returnType = descriptor.returnType

                // 显示返回类型
                element = element.withTypeText(
                    if (returnType != null)
                        SHORT_NAMES_RENDERER.renderType(returnType)
                    else "",
                    parametersAndTypeGrayed
                )

                /**
                 * 是否插入 lambda
                 */
                val insertsLambda = when (insertHandler) {
                    else -> false
                }

                if (insertsLambda) {
                    element = element.appendTailText(" {...} ", parametersAndTypeGrayed)
                }

                /**
                 * 显示参数列表
                 */
                element = element.appendTailText(
                    SHORT_NAMES_RENDERER.renderFunctionParameters(descriptor),
                    parametersAndTypeGrayed || insertsLambda
                )
            }

            /**
             * 变量补全
             */
            is VariableDescriptor -> {

                element = element.withTypeText(
                    SHORT_NAMES_RENDERER.renderType(descriptor.type),
                    parametersAndTypeGrayed
                )
            }

            /**
             * 类 / 接口 / 类型别名
             */
            is ClassifierDescriptorWithTypeParameters -> {

                val typeParams = descriptor.declaredTypeParameters

                /**
                 * 显示泛型参数
                 */
                if (includeClassTypeArguments && typeParams.isNotEmpty()) {
                    element =
                        element.appendTailText(
                            typeParams.joinToString(", ", "<", ">") {
                                it.name.asString()
                            },
                            true
                        )
                }

                var container = descriptor.containingDeclaration

                /**
                 * 处理嵌套类
                 */
                if (descriptor.isArtificialImportAliasedDescriptor) {

                    container = descriptor.original

                } else if (qualifyNestedClasses) {

                    element =
                        element.withPresentableText(
                            SHORT_NAMES_RENDERER.renderClassifierName(descriptor)
                        )

                    while (container is ClassDescriptor) {

                        val containerName = container.name

                        if (!containerName.isSpecial) {
                            element =
                                element.withLookupString(containerName.asString())
                        }

                        container = container.containingDeclaration
                    }
                }

                /**
                 * 显示所属包或类
                 */
                if (container is PackageFragmentDescriptor || container is ClassifierDescriptor) {
                    element =
                        element.appendTailText(
                            " (" + DescriptorUtils.getFqName(container) + ")",
                            true
                        )


                }

                /**
                 * 类型别名
                 */
                if (descriptor is TypeAliasDescriptor) {

                    element = element.withTypeText(
                        DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(
                            descriptor.underlyingType
                        ),
                        false
                    )
                }


            }

            /**
             * 默认渲染
             */
            else -> {
                element =
                    element.withTypeText(
                        SHORT_NAMES_RENDERER.render(descriptor),
                        parametersAndTypeGrayed
                    )
            }
        }

        /**
         * Deprecated 标记
         */
        if (lookupObject.isDeprecated) {
            element = element.withStrikeoutness(true)
        }




        if(descriptor is DeclarationDescriptorWithSource && descriptor.source is MacroExpandedSourceElement){
            element = element.appendTailText(" from macro", true)
        }


        val result = element.withIconFromLookupObject()

        return result
    }

    /**
     * 添加容器信息
     *
     * 示例：
     *   map() (std.collections)
     */
    fun appendContainerAndReceiverInformation(
        descriptor: CallableDescriptor,
        appendTailText: (String) -> Unit
    ) {

        val information =
            CompletionInformationProvider.EP_NAME.extensions.firstNotNullOfOrNull {
                it.getContainerAndReceiverInformation(descriptor)
            }

        if (information != null) {
            appendTailText(information)
            return
        }

        val containerPresentation = containerPresentation(descriptor)

        if (containerPresentation != null) {
            appendTailText(" ")
            appendTailText(containerPresentation)
        }
    }

    /**
     * 构建容器显示文本
     */
    private fun containerPresentation(descriptor: DeclarationDescriptor): String? {

        when {

            descriptor.isArtificialImportAliasedDescriptor -> {
                return "(${DescriptorUtils.getFqName(descriptor.original)})"
            }

            descriptor.isExtension -> {

                val containerPresentation =
                    when (val container = descriptor.containingDeclaration) {

                        is ClassDescriptor ->
                            DescriptorUtils.getFqNameFromTopLevelClass(container).toString()

                        is PackageFragmentDescriptor ->
                            container.fqName.toString()

                        else -> return null
                    }

                return CangJieCompletionBundle.message(
                    "presentation.tail.in.0",
                    containerPresentation
                )
            }

            else -> {

                val container =
                    descriptor.containingDeclaration as? PackageFragmentDescriptor
                        ?: return null

                return "(${container.fqName})"
            }
        }
    }

    /**
     * 在 render 阶段添加 icon
     *
     * IntelliJ 的 LookupElement 只有在 renderElement 时才能确定最终图标
     */
    private fun LookupElement.withIconFromLookupObject(): LookupElement =
        object : LookupElementDecorator<LookupElement>(this) {

            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)

                presentation.icon =
                    DefaultLookupItemRenderer.getRawIcon(this@withIconFromLookupObject)
            }
        }
}
