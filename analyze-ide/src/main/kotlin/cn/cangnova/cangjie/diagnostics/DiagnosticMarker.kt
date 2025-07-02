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

package cn.cangnova.cangjie.diagnostics

import com.intellij.psi.PsiElement

/**
 * 诊断标记接口
 * 
 * 定义了诊断与PSI元素之间的关联，提供基本的诊断信息
 */
interface DiagnosticMarker {
    /**
     * 与诊断关联的PSI元素
     */
    val psiElement: PsiElement
    
    /**
     * 诊断工厂名称
     */
    val factoryName: String
}

/**
 * 单参数诊断标记接口
 * 
 * 扩展基本诊断标记，添加一个参数
 *
 * @param A 参数类型
 */
interface DiagnosticWithParameters1Marker<A> : DiagnosticMarker {
    /**
     * 诊断参数
     */
    val a: A
}

/**
 * 双参数诊断标记接口
 * 
 * 扩展基本诊断标记，添加两个参数
 *
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 */
interface DiagnosticWithParameters2Marker<A, B> : DiagnosticMarker {
    /**
     * 第一个诊断参数
     */
    val a: A
    
    /**
     * 第二个诊断参数
     */
    val b: B
}

/**
 * 三参数诊断标记接口
 * 
 * 扩展基本诊断标记，添加三个参数
 *
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 */
interface DiagnosticWithParameters3Marker<A, B, C> : DiagnosticMarker {
    /**
     * 第一个诊断参数
     */
    val a: A
    
    /**
     * 第二个诊断参数
     */
    val b: B
    
    /**
     * 第三个诊断参数
     */
    val c: C
}

/**
 * 四参数诊断标记接口
 * 
 * 扩展基本诊断标记，添加四个参数
 *
 * @param A 第一个参数类型
 * @param B 第二个参数类型
 * @param C 第三个参数类型
 * @param D 第四个参数类型
 */
interface DiagnosticWithParameters4Marker<A, B, C, D> : DiagnosticMarker {
    /**
     * 第一个诊断参数
     */
    val a: A
    
    /**
     * 第二个诊断参数
     */
    val b: B
    
    /**
     * 第三个诊断参数
     */
    val c: C
    
    /**
     * 第四个诊断参数
     */
    val d: D
}
