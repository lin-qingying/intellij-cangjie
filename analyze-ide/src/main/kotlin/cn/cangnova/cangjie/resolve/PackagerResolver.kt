package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.diagnostics.Errors.PACKAGE_ACCESS_VIOLATION
import cn.cangnova.cangjie.psi.CjFile

/**
 * 包解析器类，用于检查和解析包级别的访问控制
 *
 * @param trace 绑定追踪对象，用于报告错误和跟踪绑定信息
 * @param moduleDescriptor 模块描述符对象，包含模块级别的描述信息
 */
class PackagerResolver(
    private val trace: BindingTrace,
    private val moduleDescriptor: ModuleDescriptor
) {
    /**
     * 检查给定上下文中文件的包级别访问控制
     *
     * @param c 顶层分析上下文，包含待分析的文件集合
     */
    fun check(c: TopDownAnalysisContext) {
        // 获取上下文中第一个文件，如果存在，则进行包级别检查
        c.files.firstOrNull()?.let {
            // 获取文件对应的包视图
            val packageView = moduleDescriptor.getPackage(it.packageFqName)

            // 调用函数检查包级别
            checkPackageLevel(it)
        }
    }

    /**
     * 检查文件的包级别访问控制是否违反了访问控制规则
     *
     * @param file 要检查的文件对象
     */
    private fun checkPackageLevel(file: CjFile) {
        // 获取文件对应的包视图
        val packageView = moduleDescriptor.getPackage(file.packageFqName)
        // 获取当前包的访问控制级别
        val currentLevel = packageView.visibility.toAccessControlLevel()
        // 如果当前包的访问控制级别为0，则无需检查
        if (currentLevel == 0) {
            return
        }
        // 如果包名是模块名，则无需检查
        if (packageView.fqName.isModuleName) {
            return
        }
        // 获取父包的访问控制级别，如果无法获取，则终止检查
        val parentLevel = packageView.containingDeclaration?.visibility?.toAccessControlLevel() ?: return
        // 如果父包的访问控制级别低于当前包的级别，则报告错误
        if (parentLevel < currentLevel) {
            // 报告包访问控制违规错误
            trace.report(
                PACKAGE_ACCESS_VIOLATION.on(
                    file.packageDirective,
                    file.packageFqName,
                    packageView.containingDeclaration?.fqName,
                )
            )
        }
    }
}
