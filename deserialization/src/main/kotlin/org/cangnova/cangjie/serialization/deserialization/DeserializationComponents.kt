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

package org.cangnova.cangjie.serialization.deserialization

import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.NotFoundClasses
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentProvider
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.Package
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.DefaultTypeAttributeTranslator
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeAttributeTranslator
import org.cangnova.cangjie.types.checker.NewCangJieTypeChecker

/**
 * Configuration interface for handling local classifier types during deserialization.
 * Local classifiers are types that are defined locally within a function or class scope
 * and may need special handling during the deserialization process.
 */
interface LocalClassifierTypeSettings {
    val replacementTypeForLocalClassifiers: SimpleType?

    object Default : LocalClassifierTypeSettings {
        override val replacementTypeForLocalClassifiers: SimpleType?
            get() = null
    }
}

/**
 * DeserializationComponents 是仓颉语言反序列化系统的核心组件容器类。
 * 该类管理和协调所有反序列化过程中所需的组件和服务。
 *
 * 主要作用：
 * 1. 集中管理反序列化所需的各种组件（存储管理器、模块描述符、错误报告器等）
 * 2. 为反序列化过程提供统一的访问接口和配置
 * 3. 支持从flatbuffers格式的元数据文件反序列化仓颉语言的类型信息
 * 4. 处理类型检查、错误处理、查找跟踪等核心功能
 *
 * 该类是反序列化流程的基础设施，为 DeserializationContext 和其他反序列化组件
 * 提供必要的依赖和配置信息。
 *
 * @param storageManager 存储管理器，用于管理计算结果的缓存和懒加载
 * @param moduleDescriptor 模块描述符，表示当前正在反序列化的模块
 * @param configuration 反序列化配置，控制反序列化行为的各种参数
 * @param packageFragmentProvider 包片段提供者，用于查找和提供包的信息
 * @param localClassifierTypeSettings 本地分类器类型设置，处理局部定义的类型
 * @param errorReporter 错误报告器，用于报告反序列化过程中的错误
 * @param lookupTracker 查找跟踪器，用于增量编译中跟踪符号查找
 * @param notFoundClasses 未找到类处理器，处理反序列化时无法找到的类
 * @param cangjieTypeChecker 仓颉类型检查器，用于类型检查和验证
 * @param typeAttributeTranslators 类型属性转换器列表，用于处理类型注解和属性
 */
class DeserializationComponents(
    val storageManager: StorageManager,
    val moduleDescriptor: ModuleDescriptor,
    val configuration: DeserializationConfiguration,
    val classDataFinder: ClassDataFinder,
//    val annotationAndConstantLoader: AnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>>,
    val packageFragmentProvider: PackageFragmentProvider,
    val localClassifierTypeSettings: LocalClassifierTypeSettings,
    val errorReporter: ErrorReporter,
    val lookupTracker: LookupTracker,
//    val flexibleTypeDeserializer: FlexibleTypeDeserializer,
    val fictitiousClassDescriptorFactories: Iterable<ClassDescriptorFactory>,
    val notFoundClasses: NotFoundClasses,
//    val contractDeserializer: ContractDeserializer,
//    val additionalClassPartsProvider: AdditionalClassPartsProvider = AdditionalClassPartsProvider.None,
//    val platformDependentDeclarationFilter: PlatformDependentDeclarationFilter = PlatformDependentDeclarationFilter.All,
//    val extensionRegistryLite: ExtensionRegistryLite,
    val cangjieTypeChecker: NewCangJieTypeChecker = NewCangJieTypeChecker.Default,
//    val samConversionResolver: SamConversionResolver,
    val typeAttributeTranslators: List<TypeAttributeTranslator> = listOf(DefaultTypeAttributeTranslator),
//    val enumEntriesDeserializationSupport: EnumEntriesDeserializationSupport = EnumEntriesDeserializationSupport.Default,
) {
    //    val classDeserializer: ClassDeserializer = ClassDeserializer(this)
//
//    fun deserializeClass(classId: ClassId): ClassDescriptor? = classDeserializer.deserializeClass(classId)
//
    fun createContext(
        descriptor: PackageFragmentDescriptor,

        metadataVersion: BinaryVersion,
        containerSource: DeserializedContainerSource?
    ): DeserializationContext =
        DeserializationContext(
            this, descriptor, metadataVersion, containerSource,
//            parentTypeDeserializer = null/*, typeParameters = listOf()*/
        )
}

/**
 * DeserializationContext 是反序列化过程中的上下文环境类。
 * 该类为特定的反序列化操作提供上下文信息和工具。
 *
 * 主要作用：
 * 1. 维护反序列化操作的上下文状态（包含声明、元数据版本等）
 * 2. 提供成员反序列化器，用于反序列化类成员（函数、属性、字段等）
 * 3. 管理反序列化过程中的类型信息和作用域
 * 4. 支持嵌套的反序列化上下文创建
 *
 * 该类是反序列化操作的执行环境，包含了执行具体反序列化任务所需的
 * 所有上下文信息和工具。
 *
 * @param components 反序列化组件容器，提供所有必需的服务和配置
 * @param containingDeclaration 包含当前反序列化内容的声明描述符
 * @param metadataVersion 元数据版本信息，用于版本兼容性处理
 * @param parentTypeDeserializer 父级类型反序列化器，用于嵌套类型处理
 */
class DeserializationContext(
    val components: DeserializationComponents,

    val containingDeclaration: DeclarationDescriptor,


    val metadataVersion: BinaryVersion,
    val containerSource: DeserializedContainerSource?,
//    parentTypeDeserializer: TypeDeserializer?,
//    typeParameters: List<TypeParameter>
) {
//        val typeDeserializer: TypeDeserializer = TypeDeserializer(
//        this, parentTypeDeserializer, typeParameters,
//        "Deserializer for \"${containingDeclaration.name}\"",
//        containerSource?.presentableString ?: "[container not found]"
//    )

    val declarationDeserializer: DeclarationDeserializer = DeclarationDeserializer(this)

    //
    val storageManager: StorageManager get() = components.storageManager

    //
    fun childContext(
        descriptor: DeclarationDescriptor,
//        typeParameterParams: List<TypeParameter>,

        metadataVersion: BinaryVersion = this.metadataVersion
    ): DeserializationContext = DeserializationContext(
        components, descriptor,

        metadataVersion, this.containerSource,
//        parentTypeDeserializer = this.typeDeserializer,
    )
}

/**
 * 此文件中工具函数的目的是根据给定二进制文件的版本在反序列化过程中支持不同的行为。
 *
 * 例如，如果我们在序列化/反序列化中发现一个bug，并希望修复它同时保持与仓颉的两个最新版本的兼容性，
 * 我们可以使用此类的方法来修复"未来"二进制文件的反序列化，然后在下一个主版本中修复序列化中的bug，
 * 当二进制版本前进到第一个bug修复中支持的值时。
 */

/**
 * 在元数据版本1.4之前，嵌套类的版本需求被错误地反序列化：版本需求表
 * 从最外层类加载并传递给嵌套类及其成员，即使它们的版本需求的索引
 * 指向存储在嵌套类中的其他表（反序列化时未读取）。
 */
fun isVersionRequirementTableWrittenCorrectly(version: BinaryVersion): Boolean =
    isCangJie1Dot4OrLater(version)

fun isCangJie1Dot4OrLater(version: BinaryVersion): Boolean =
    (version.major == 1 && version.minor >= 4) || version.major > 1
