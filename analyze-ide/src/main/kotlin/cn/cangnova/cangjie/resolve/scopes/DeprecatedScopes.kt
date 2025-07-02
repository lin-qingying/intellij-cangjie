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

package cn.cangnova.cangjie.resolve.scopes

import cn.cangnova.cangjie.descriptors.ClassifierDescriptor
import cn.cangnova.cangjie.descriptors.DescriptorWithDeprecation
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.name.Name


class DeprecatedLexicalScope(private val workerScope: LexicalScope) : LexicalScope by workerScope {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

    override fun getContributedClassifierIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? {
        return workerScope.getContributedClassifier(name, location)?.let { DescriptorWithDeprecation.createDeprecated(it) }
    }
}

class DeprecatedMemberScope(private val workerScope: MemberScope) : MemberScope by workerScope {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

    override fun getContributedClassifierIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? {
        return workerScope.getContributedClassifier(name, location)?.let { DescriptorWithDeprecation.createDeprecated(it) }
    }
}
