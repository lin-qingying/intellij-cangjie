package com.linqingying.cangjie.dag

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiImportStatementBase
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjFile

import com.linqingying.cangjie.resolve.descriptorUtil.targetPackageView

class DependencyGraphListener : PsiTreeChangeAdapter() {
    override fun childAdded(event: PsiTreeChangeEvent) {
        handleFileChange(event.file)
    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
        handleFileChange(event.file)
    }

    override fun childReplaced(event: PsiTreeChangeEvent) {
        handleFileChange(event.file)
    }


    private fun handleFileChange(file: PsiFile?) {
        if (file !is CjFile) return
        updateDependenciesForFile(file)
    }
}


val dependencyDAG = DependencyDAG()

fun parseFileForDependencies(file: CjFile): List<FqName> {
    // 假设解析包依赖逻辑为读取 import 语句或其他语法
    val dependencies = mutableListOf<FqName>()

    file.importDirectives.forEach {
      val a =  it.targetPackageView()

        a?.let{
            dependencies.add(it.fqName)
        }
    }
//    file.children.forEach { element ->
//        if (element is PsiImportStatementBase) {
//            val importedPackage = resolveToPackage(element)
//            importedPackage?.let { dependencies.add(it) }
//        }
//    }
    return dependencies
}

fun updateDependenciesForFile(file: CjFile) {
    val packageName = file.packageFqName
    val dependencies = parseFileForDependencies(file)

    // 移除旧依赖
    dependencyDAG.removeDependencies(packageName)

    // 添加新依赖
    for (dependency in dependencies) {
        if (!dependencyDAG.addDependency(packageName, dependency)) {
            // 如果检测到环，则可以在此处报告错误
            println("Circular dependency detected: $packageName -> $dependency")
        }
    }
}
