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

package org.cangnova.cangjie.serialization.deserialization.descriptors

import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.name.ClassId

/**
 * 反序列化容器来源接口，继承自 [SourceElement]。
 * 用于描述从类文件加载的反序列化容器的元信息，包括版本兼容性、预发布可见性及 ABI 稳定性等。
 */
interface DeserializedContainerSource : SourceElement {

    /**
     * 若该容器是从二进制版本不兼容的类文件中加载的，则此字段非空，包含具体的不兼容版本错误信息。
     * 若版本兼容，则为 null。
     */
    val incompatibility: IncompatibleVersionErrorData<*>?

    /**
     * 若为 true，表示该容器因从预发布版本的类文件加载、而当前编译器为正式发布版本，
     * 导致该容器处于"不可见"状态。
     */
    val isPreReleaseInvisible: Boolean

    /**
     * 描述该容器的 ABI 稳定性状态。
     * 当容器由新 IR 后端编译、而当前编译器未使用 IR 后端，且未指定任何覆盖标志时，
     * 该字段将反映相应的不稳定状态。
     */
    val abiStability: DeserializedContainerAbiStability

    /**
     * 该容器的可读描述字符串，仅用于在错误信息中展示，不应用于逻辑判断。
     */
    val presentableString: String
}

/**
 * 反序列化容器的 ABI 稳定性枚举。
 * 用于标识容器是否与当前编译器的 ABI 兼容。
 */
enum class DeserializedContainerAbiStability {

    /**
     * 容器是稳定的，或当前编译器已配置为忽略依赖项的 ABI 稳定性检查。
     */
    STABLE,

    /**
     * 容器不稳定
     */
    UNSTABLE,
}

/**
 * 版本不兼容错误数据类，封装了导致版本不兼容的相关版本信息。
 *
 * @param T 版本类型参数（协变，只读）
 * @property actualVersion   类文件中实际记录的版本号
 * @property compilerVersion 当前编译器的版本号
 * @property languageVersion 语言版本号
 * @property expectedVersion 编译器期望的版本号
 * @property filePath        发生版本不兼容的类文件路径
 * @property classId         发生版本不兼容的类的标识符
 */
data class IncompatibleVersionErrorData<out T>(
    val actualVersion: T,
    val compilerVersion: T,
    val languageVersion: T,
    val expectedVersion: T,
    val filePath: String,
    val classId: ClassId
)