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

package com.linqingying.cangjie.types;

import com.linqingying.cangjie.builtins.CangJieBuiltIns;
import com.linqingying.cangjie.descriptors.ClassifierDescriptor;
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor;
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner;
import com.linqingying.cangjie.types.model.TypeConstructorMarker;
import com.linqingying.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 类型构造器接口，扩展了 TypeConstructorMarker 接口。
 * 该接口定义了类型系统中类型构造器的基本操作和属性。
 */
public interface TypeConstructor extends TypeConstructorMarker {

    /**
     * 获取当前类型的父类型集合。
     *
     * @return 父类型集合，不能为空且只读
     */
    @NotNull
    @ReadOnly
    Collection<CangJieType> getSupertypes();

    /**
     * 获取扩展的父类型集合，可以根据提供的扩展 ID 进行排除。
     *
     * @param extendId 需要排除的扩展 ID，可以为 null
     * @return 扩展的父类型集合，不能为空且只读
     */
    @NotNull
    @ReadOnly
    default Collection<CangJieType> getExtendSupertypes(@Nullable String extendId) {
        return Collections.emptyList();
    }

    /**
     * 获取内置类型信息。
     *
     * @return 内置类型信息，不能为空
     */
    @NotNull
    CangJieBuiltIns getBuiltIns();

    /**
     * 判断该类型是否可表示（denotable）。
     * 如果类型不可表示，则不能直接在代码中书写，仅能在类型检查器内部出现。
     * 例如：交集类型或数值类型。
     *
     * @return 如果类型可表示则返回 true，否则返回 false
     */
    boolean isDenotable();

    /**
     * 获取分类描述符。
     *
     * @return 分类描述符，可以为 null
     */
    @Nullable
    ClassifierDescriptor getDeclarationDescriptor();

    /**
     * 使用给定的类型精炼器对当前类型构造器进行精炼。
     * 返回经过精炼后的类型构造器。
     * <p>
     * 合约：
     * - 返回的类型构造器具有经过精炼的父类型，即这些父类型已根据精炼模块正确解析。
     * - 不会添加平台声明的父类型！
     * - 其他类似的 CangJieTypes/Descriptors 源也应返回经过精炼的实例。
     * <p>
     * 注意：
     * - 该方法是内部精炼基础设施的一部分，不应从任何其他地方调用，除非是从精炼方法中调用。
     * - 实现细节请参见 'AbstractTypeConstructor' 和其他特殊类型构造器。
     *
     * @param cangjieTypeRefiner 类型精炼器，不能为空
     * @return 经过精炼的类型构造器，不能为空
     */
    @TypeRefinement
    @NotNull
    TypeConstructor refine(@NotNull CangJieTypeRefiner cangjieTypeRefiner);

    /**
     * 判断该类型是否为最终类型，即不允许有子类型。
     *
     * @return 如果是最终类型则返回 true，否则返回 false
     */
    boolean isFinal();

    /**
     * 获取类型构造器的参数列表。
     * 对于内部类，此列表可能包含来自外部声明的额外参数。
     *
     * @return 参数列表，不能为空且只读
     */
    @NotNull
    @ReadOnly
    List<TypeParameterDescriptor> getParameters();
}
