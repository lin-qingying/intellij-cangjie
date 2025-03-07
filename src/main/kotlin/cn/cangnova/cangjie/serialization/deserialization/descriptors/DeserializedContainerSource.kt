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

package cn.cangnova.cangjie.serialization.deserialization.descriptors

import cn.cangnova.cangjie.descriptors.SourceElement
import cn.cangnova.cangjie.name.ClassId

interface DeserializedContainerSource : SourceElement {
    // Non-null if this container is loaded from a class with an incompatible binary version
    val incompatibility: IncompatibleVersionErrorData<*>?

    // True iff this is container is "invisible" because it's loaded from a pre-release class and this compiler is a release
    val isPreReleaseInvisible: Boolean

    // True iff this container was compiled by the new IR backend, this compiler is not using the IR backend right now,
    // and no additional flags to override this behavior were specified.
    val abiStability: DeserializedContainerAbiStability

    // This string should only be used in error messages
    val presentableString: String
}
enum class DeserializedContainerAbiStability {
    // Either the container is stable, or this compiler is configured to ignore ABI stability of dependencies.
    STABLE,

    // The container is unstable because either:
    // 1) it is compiled with JVM IR prior to 1.4.30, or
    // 2) it is compiled with JVM IR >= 1.4.30 with the `-Xabi-stability=unstable` compiler option,
    // 3) it is compiled with FIR prior to 2.0.0,
    // and this compiler is _not_ configured to ignore that.
    UNSTABLE,
}
data class IncompatibleVersionErrorData<out T>(
    val actualVersion: T,
    val compilerVersion: T,
    val languageVersion: T,
    val expectedVersion: T,
    val filePath: String,
    val classId: ClassId
)
