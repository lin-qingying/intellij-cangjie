package cn.cangnova.cangjie.dag

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import cn.cangnova.cangjie.ide.stubindex.CangJieExactPackagesIndex
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjImportDirectiveItem
import cn.cangnova.cangjie.resolve.descriptorUtil.targetPackageView

class DependencyGraphListener : PsiTreeChangeAdapter() {

//    override fun childAdded(event: PsiTreeChangeEvent) {
//        handleFileChange(event.file)
//    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
        handleFileDeletion(event.file)
    }


    override fun childReplaced(event: PsiTreeChangeEvent) {
        handleFileChange(event.file)
    }

    private fun handleFileDeletion(file: PsiFile?) {
        if (file !is CjFile) return
        val dependencyGraph = CangJieDependencyGraph.getInstance(file.project)

        // 直接移除文件及其依赖关系
        val fqName = file.packageFqName
        dependencyGraph.removeNode(fqName, file)
    }

    private fun handleFileChange(file: PsiFile?) {
        if (file !is CjFile) return
        val dependencyGraph = CangJieDependencyGraph.getInstance(file.project)

        // 先移除旧的依赖关系，再更新为新的依赖关系
        val fqName = file.packageFqName
        dependencyGraph.removeNode(fqName, file)
        dependencyGraph.updateDependenciesForFile(file)
    }

}


@Service(Service.Level.PROJECT)
class CangJieDependencyGraph(val project: Project) {
    private val adjacencyList = mutableMapOf<FqName, MutableSet<FqName>>()

    private fun getImportFqNames(fqName: FqName): Set<FqName> {
        return adjacencyList[fqName] ?: emptySet()
    }

    private fun addDependency(from: FqName, to: FqName) {
        adjacencyList.computeIfAbsent(from) { mutableSetOf() }.add(to)


    }

    // 新增：存储 CjFile -> FqName -> List<CjImport> 的映射
    private val fileDependencyMap = mutableMapOf<CjFile, MutableMap<FqName, MutableList<CjImportDirectiveItem>>>()

    // 删除文件中的特定依赖信息，如果依赖为空，则删除整个文件键
    private fun removeFileDependencies(file: CjFile) {

        fileDependencyMap.remove(file)

    }

    fun getImportItems(file: CjFile, fqName: FqName): List<CjImportDirectiveItem> {
        return fileDependencyMap[file]?.get(fqName) ?: emptyList()
    }

    // 删除节点并更新相关边
    fun removeNode(node: FqName, file: CjFile? = null) {
        // 1. 删除该节点的所有边（入边和出边）
        adjacencyList.remove(node)

        // 2. 从其他节点的依赖中移除该节点
//        adjacencyList.forEach { (key, neighbors) ->
//            neighbors.remove(node)
//        }


        // 删除文件依赖信息
        if (file != null) {
            removeFileDependencies(file)
        }
    }

    // 检查图结构
    fun printGraph() {
        println("依赖图形结构:")
        println("====================================================================================")
        adjacencyList.forEach { (key, neighbors) ->
            println("$key -> $neighbors")
        }
        println("====================================================================================")

    }

    /**
     * 检查指定的 fqName 是否存在于依赖图中的环中。
     * 如果存在，返回包含该 fqName 的环的元素。
     * @param fqName 要检查的 FqName。
     * @return 如果 fqName 在环中，则返回包含 fqName 的环；否则返回 null。
     */
    fun findCycleContaining(fqName: FqName): List<FqName> {
        // 遍历检测到的所有环
        for (cycle in detectCycles()) {
            if (fqName in cycle) {
                return cycle
            }
        }
        // 如果没有找到对应的环，返回 null
        return emptyList()
    }

    /**
     * 检测依赖图中的所有环。
     * @return 返回一个包含所有检测到的环的列表，每个环表示为 FqName 对象的列表。
     */
    fun detectCycles(): List<List<FqName>> {
        val visited = mutableSetOf<FqName>()
        val recursionStack = mutableSetOf<FqName>()
        val cycles = mutableListOf<List<FqName>>()

        adjacencyList.keys.forEach { node ->
            if (node !in visited) {
                dfs(node, mutableListOf(), visited, recursionStack, cycles)
            }
        }

        return cycles
    }

    /**
     * 深度优先搜索（DFS）方法，用于检测从给定节点开始的环。
     *
     * @param node 当前访问的节点。
     * @param path 当前路径上的节点列表。
     * @param visited 已访问的节点集合。
     * @param recursionStack 当前递归调用栈中的节点集合。
     * @param cycles 用于存储检测到的环的列表。
     * @return 如果检测到环则返回 true，否则返回 false。
     */
    private fun dfs(
        node: FqName,
        path: MutableList<FqName>,
        visited: MutableSet<FqName>,
        recursionStack: MutableSet<FqName>,
        cycles: MutableList<List<FqName>>
    ): Boolean {
        if (node in recursionStack) {
            // 找到一个环，提取环路径
            val cycleStartIndex = path.indexOf(node)
            if (cycleStartIndex != -1) {
                cycles.add(path.subList(cycleStartIndex, path.size).toList())
                return true
            }

        }
        if (node in visited) return false

        visited.add(node)
        recursionStack.add(node)
        path.add(node)

        adjacencyList[node]?.forEach { neighbor ->
            if (dfs(neighbor, path, visited, recursionStack, cycles)) return true
        }

        path.removeAt(path.size - 1)
        recursionStack.remove(node)
        return false
    }

    // 新增：添加文件与依赖的对应关系
    private fun addFileDependencies(
        file: CjFile?,
        fqName: FqName?,
        imports: List<CjImportDirectiveItem> = emptyList()
    ) {
        if (file != null && fqName != null) {
            val fqNameMap = fileDependencyMap.computeIfAbsent(file) { mutableMapOf() }
            fqNameMap.computeIfAbsent(fqName) { mutableListOf() }
            fqNameMap[fqName]?.addAll(imports)
        }
    }

    /**
     * 从索引中获取并更新添加
     */
    private fun parseFileForDependenciesByStubIndex(fqName: FqName) {
        val files = CangJieExactPackagesIndex[fqName.asString(), project]

        updateDependenciesForFile(files)
    }

    private fun parsePackageForDependencies(fqName: FqName): List<FqName> {
        // 假设解析包依赖逻辑为读取 import 语句或其他语法
        val dependencies = mutableListOf<FqName>()

        val files = CangJieExactPackagesIndex[fqName.asString(), project]
        val imports = files.flatMap { it.importDirectivesItem }
        imports.forEach {


            val packageView = it.targetPackageView()


            packageView?.let { descriptor ->


                dependencies.add(descriptor.fqName)
//                // 为依赖的父级更新 HashMap
//                addFileDependencies(file, descriptor.fqName, listOfNotNull(it))


//                updateDependenciesForFqName(descriptor.fqName)
            }
        }



        return dependencies
    }


    private fun updateDependenciesForFile(files: List<CjFile>) {
        files.forEach {
            updateDependenciesForFqName(it.packageFqName)
        }
    }

    fun updateDependenciesForFqName(fqName: FqName) {

        val dependencies = parsePackageForDependencies(fqName)
        dependencies.forEach { dependency ->
            addDependency(fqName, dependency)

        }

    }

    fun updateDependenciesForFile(file: CjFile) {
        val fqName = file.packageFqName
        updateDependenciesForFqName(fqName)

        val importsFqName = getImportFqNames(fqName)
        importsFqName.forEach {
            updateDependenciesForFqName(it)
        }
        checkForCycles()
    }

    private fun checkForCycles() {
        val cycles = detectCycles()
        if (cycles.isNotEmpty()) {
            cycles.forEach { cycle ->
                println("检测到环: ${cycle.joinToString(" -> ")}")
            }
        } else {
            printGraph()
        }
    }


    companion object {
        fun getInstance(project: Project): CangJieDependencyGraph {
            return project.service()
        }
    }
}


