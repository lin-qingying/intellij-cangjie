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

import cn.cangnova.cangjie.descriptors.SourceFile
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.deserialization.NameResolver
import cn.cangnova.cangjie.name.CangJieClassName
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.serialization.CangJieMetadataVersion

interface FacadeClassSource {
    val className: CangJieClassName
    val facadeClassName: CangJieClassName?
}
class CangJiePackagePartSource(


    override val className: CangJieClassName,
    override val facadeClassName: CangJieClassName?,
    packageProto: ProtoBuf.Package,
    nameResolver: NameResolver,
    override val incompatibility: IncompatibleVersionErrorData<CangJieMetadataVersion>? = null,
    override val isPreReleaseInvisible: Boolean = false,
    override val abiStability: DeserializedContainerAbiStability = DeserializedContainerAbiStability.STABLE,

    ):DeserializedContainerSource,FacadeClassSource  {
    override val presentableString: String
        get() = "Class '${classId.asSingleFqName().asString()}'"
    val classId: ClassId get() = ClassId(className.packageFqName, simpleName)
    val simpleName: Name get() = Name.identifier(className.internalName.substringAfterLast('/'))
    override fun toString() = "${this::class.java.simpleName}: $className"

    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE

}
