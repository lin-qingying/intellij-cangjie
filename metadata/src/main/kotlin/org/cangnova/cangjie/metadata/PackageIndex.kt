package org.cangnova.cangjie.metadata

/**
 * 表示仓颉编译器中用于AST序列化和包引用处理的包索引值的Kotlin枚举类。
 */
enum class PackageIndex(val value: Int) {
    /**
     * 无效的包引用 (-1)
     */
    INVALID_PACKAGE_INDEX(-1),
    
    /**
     * 当前包索引 (-2)  
     * 用于当前包内的声明
     */
    CURRENT_PKG_INDEX(-2),
    
    /**
     * 包引用索引 (-3)
     * 用于引用整个包时
     */
    PKG_REFERENCE_INDEX(-3),

//
//    /**
//     * 包引用索引(n)
//     * 用于引用导入的包，n为非负整数
//     */
//    IMPORTED_PKG_INDEX( ) // 占位符，实际导入包索引为非负整数
//
    ;

    companion object {
        /**
         * 将整数值转换为PackageIndex枚举
         * @param value 整数值
         * @return PackageIndex枚举，如果值表示导入的包（≥0）则返回null
         */
        fun fromValue(value: Int): PackageIndex? {
            return values().find { it.value == value }
        }
        
        /**
         * 检查给定的值是否表示导入的包索引
         * @param value 要检查的整数值
         * @return 如果值 ≥ 0 则返回true，表示导入的包
         */
        fun isImportedPackage(value: Int): Boolean {
            return value >= 0
        }
    }
}