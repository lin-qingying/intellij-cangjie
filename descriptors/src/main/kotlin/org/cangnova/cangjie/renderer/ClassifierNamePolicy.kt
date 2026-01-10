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

package org.cangnova.cangjie.renderer
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.PrimitiveClassDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils.getFqName

/**
 * Classifier 名称渲染策略接口。
 *
 * 不同实现决定如何将 ClassifierDescriptor（类、接口、类型参数等）渲染为字符串。
 */
interface ClassifierNamePolicy {
    /**
     * 简短名称策略：对嵌套类使用限定名（只到外部类），对类型参数渲染为其简单名称。
     *
     * 适用于在简洁输出中保留必要的上下文（例如嵌套关系）时使用。
     */
    object SHORT : ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
            if (classifier is TypeParameterDescriptor) return renderer.renderName(classifier.name, false)

            val qualifiedNameElements = ArrayList<Name>()

            // for nested classes qualified name should be used
            var current: DeclarationDescriptor? = classifier
            do {
                qualifiedNameElements.add(current!!.name)
                current = current.containingDeclaration
            } while (current is ClassDescriptor)

            return renderFqName(qualifiedNameElements.asReversed())
        }
    }

    /**
     * 完全限定名策略：始终使用完整的包 + 类路径进行渲染。
     *
     * 对类型参数同样仅渲染其名称。适用于需要唯一标识类型的场景。
     */
    object FULLY_QUALIFIED : ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
            if (classifier is TypeParameterDescriptor) return renderer.renderName(classifier.name, false)

            return renderer.renderFqName( getFqName(classifier))
        }
    }

    // for local declarations qualified up to function scope
    /**
     * 源代码限定名策略：对于局部声明（例如在函数内部声明的类），渲染到函数/源代码可见的限定范围。
     *
     * 主要用于在将渲染结果与源代码对齐时显示足够的限定信息，但避免过度使用完整包名。
     */
    object SOURCE_CODE_QUALIFIED : ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String =
            qualifiedNameForSourceCode(classifier)

        private fun qualifiedNameForSourceCode(descriptor: ClassifierDescriptor): String {
            val nameString = descriptor.name.render()
            if (descriptor is TypeParameterDescriptor) {
                return nameString
            }
            val qualifier = qualifierName(descriptor.containingDeclaration)
            return if (qualifier != null && qualifier != "") qualifier + "." + nameString else nameString
        }

        private fun qualifierName(descriptor: DeclarationDescriptor): String? = when (descriptor) {
            is PrimitiveClassDescriptor -> null

            is ClassDescriptor -> qualifiedNameForSourceCode(descriptor)
            is PackageFragmentDescriptor -> descriptor.fqName.toUnsafe().render()
            else -> null
        }
    }

    /**
     * 将指定的 ClassifierDescriptor 渲染为字符串。
     *
     * @param classifier 要渲染的分类器描述符（类、接口、类型参数等）
     * @param renderer 用于渲染名称和限定名的 DescriptorRenderer 实例
     * @return 渲染后的字符串表示
     */
    fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String
}
