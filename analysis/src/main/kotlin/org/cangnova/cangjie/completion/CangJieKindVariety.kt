package org.cangnova.cangjie.completion

import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.codeInsight.completion.CompletionParameters
import org.cangnova.cangjie.completion.turboComplete.KindVariety

/**
 * 仓颉语言代码补全的候选项类别枚举
 *
 * 每个枚举值对应一种补全子类型，用于：
 * 1. 创建对应的 KindCollector（负责收集该类型的候选项）
 * 2. 通过 SortingExecutor 对不同类型的补全结果进行重排序
 *
 * 执行顺序由 SortingExecutor 决定，通常优先展示响应快的类型（如关键字），
 * 再异步补充慢的类型（如未导入的符号）。
 */
enum class CangJieCompletionKindName {
    /** DSL 函数补全（特定 DSL 上下文中的函数建议） */
    DSL_FUNCTION,

    /** 智能补全附加项（基于期望类型推断出的高优先级候选） */
    SMART_ADDITIONAL_ITEM,

    /** 基础引用补全（当前作用域内已导入的变量、函数、类等） */
    REFERENCE_BASIC,

    /** 扩展函数引用补全（需要自动添加 import 的扩展函数） */
    REFERENCE_EXTENSION,

    /** 顶层包名补全（FqName.ROOT 的直接子包） */
    PACKAGE_NAME,

    /** 具名参数补全（函数调用中 paramName = ▌ 位置的参数名） */
    NAMED_ARGUMENT,

    /** 函数类型值的扩展补全（对函数类型接收者调用 invoke 等） */
    EXTENSION_FUNCTION_TYPE_VALUE,

    /** 上下文变量类型匹配的智能补全（有匹配函数类型变量时才触发） */
    CONTEXT_VARIABLE_TYPE_SC,

    /** 上下文变量类型匹配的引用补全（有匹配函数类型变量时才触发） */
    CONTEXT_VARIABLE_TYPE_REFERENCE,

    /** 从已导入类补全静态成员（已 import 的类的静态函数/属性） */
    STATIC_MEMBER_FROM_IMPORTS,

    /** 未导入符号补全（顶层函数、未导入的类，选中后自动添加 import） */
    NON_IMPORTED,

    /** 调试器专用补全（运行时实际类型的成员，仅调试会话中启用） */
    DEBUGGER_VARIANTS,

    /** 从 object 单例补全匹配接收者类型的扩展成员 */
    STATIC_MEMBER_OBJECT_MEMBER,

    /** 从继承链和显式导入补全匹配接收者类型的扩展成员 */
    STATIC_MEMBER_EXPLICIT_INHERITED,

    /** 不可直接访问的静态成员补全（需要限定符才能访问，从全局索引查询） */
    STATIC_MEMBER_INACCESSIBLE,

    /** 仅关键字补全（val/var/fun/if/when 等，根据 PSI 上下文过滤） */
    KEYWORD_ONLY,

    /** 运算符名称补全（operator fun ▌ 位置的 plus/minus/invoke 等） */
    OPERATOR_NAME,

    /** 声明名称补全（val/fun/class 后面的标识符位置） */
    DECLARATION_NAME,

    /** 顶层类名补全（建议使用与文件名相同的类名） */
    TOP_LEVEL_CLASS_NAME,

    /** super 限定符补全（super<▌> 位置的父类/接口列表） */
    SUPER_QUALIFIER,

    /** 来自未解析引用或 override 的声明名称补全 */
    DECLARATION_NAME_FROM_UNRESOLVED_OVERRIDE,

    /** 参数名/变量名 + 类型的联合补全（"name: Type" 形式） */
    PARAMETER_OR_VAR_NAME_AND_TYPE,
}

/**
 * 仓颉语言的补全类型变体标识
 *
 * 实现 KindVariety 接口，用于告知 TurboComplete 框架：
 * - 何时应该使用仓颉语言的补全 Kind 分类体系
 * - 对应的实际补全贡献者是哪个类
 *
 * TurboComplete 框架通过此对象判断当前补全请求
 * 是否属于仓颉语言，从而决定是否启用上面定义的 KindName 分类和调度逻辑。
 */
object CangJieKindVariety : KindVariety {

    /**
     * 判断当前补全参数是否对应仓颉语言
     * 通过检查光标所在位置的语言类型来判断，
     * 只有当光标在仓颉语言文件中时才返回 true。
     *
     * @param parameters IntelliJ 补全参数
     * @return 光标位置的语言是仓颉语言时返回 true
     */
    override fun kindsCorrespondToParameters(parameters: CompletionParameters): Boolean {
        return parameters.position.language == CangJieLanguage
    }

    /**
     * 实际执行补全的 Contributor 类
     * TurboComplete 框架通过此属性找到对应的补全贡献者，
     * 将 KindCollector 与具体的补全实现绑定。
     */
    override val actualCompletionContributorClass: Class<*>
        get() = CangJieCompletionContributor::class.java
}