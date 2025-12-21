/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.cache.trackers

import com.intellij.lang.ASTNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.PomManager
import com.intellij.pom.PomModelAspect
import com.intellij.pom.event.PomModelEvent
import com.intellij.pom.event.PomModelListener
import com.intellij.pom.tree.TreeAspect
import com.intellij.pom.tree.events.TreeChangeEvent
import com.intellij.pom.tree.events.impl.ChangeInfoImpl
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findTopmostParentInFile
import com.intellij.psi.util.findTopmostParentOfType
import com.intellij.psi.util.isAncestor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.anyDescendantOfType

private val FILE_IN_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_IN_BLOCK_MODIFICATION_COUNT")

private val IN_BLOCK_MODIFICATIONS = Key<MutableCollection<CjElement>>("IN_BLOCK_MODIFICATIONS")

/**
 * 在当前文件中添加一个在代码块内修改的元素。
 *
 * 此函数用于记录和管理在代码块内部进行的修改操作，通过维护一个集合并使用同步块来确保线程安全。
 * 它首先检查是否存在一个指定类型的用户数据，如果不存在则创建一个新的集合，并通过同步块来处理集合的更新。
 *
 * @param element 要添加到在代码块内修改项集合中的CjElement对象。
 */
private fun CjFile.addInBlockModifiedItem(element: CjElement) {
    // 获取或初始化在代码块内的修改项集合，并确保线程安全。
    val collection = putUserDataIfAbsent(IN_BLOCK_MODIFICATIONS, mutableSetOf())
    synchronized(collection) {
        // 检查集合中是否存在当前元素的祖先元素，以决定是否需要添加当前元素。
        val needToAddBlock = collection.none { it.isAncestor(element, strict = false) }
        if (needToAddBlock) {
            // 如果需要添加，先移除集合中当前元素的子孙元素，然后添加当前元素。
            collection.removeIf { element.isAncestor(it, strict = false) }
            collection.add(element)
        }
    }
    // 更新文件中代码块内修改的计数。
    val count = getUserData(FILE_IN_BLOCK_MODIFICATION_COUNT) ?: 0
    putUserData(FILE_IN_BLOCK_MODIFICATION_COUNT, count + 1)
}


/**
 * 获取在当前文件中以块级进行的修改集合
 * 这个属性提供了一种访问文件中块级修改的方式，通过自定义的用户数据
 * 如果没有进行任何修改或修改集合为空，则返回一个空列表
 *
 * @return 返回一个[CjElement]类型的集合，表示文件中的块级修改如果数据不存在或为空，则返回空列表
 */
val CjFile.inBlockModifications: Collection<CjElement>
    get() {
        // 尝试获取文件中块级修改的自定义用户数据，如果没有则返回空列表
        val collection = getUserData(IN_BLOCK_MODIFICATIONS) ?: return emptyList()
        // 同步集合以防止并发修改问题
        return synchronized(collection) {
            // 如果集合非空，则返回一个包含这些修改的ArrayList，否则返回空列表
            if (collection.isNotEmpty()) {
                ArrayList(collection)
            } else {
                emptyList()
            }
        }
    }


/**
 * 清除当前文件对象中的块修改记录
 * 此函数主要用于清理在文件处理过程中临时记录的修改信息，以确保文件状态的正确性
 */
fun CjFile.clearInBlockModifications() {
    // 获取当前文件对象中记录的块修改信息
    getUserData(IN_BLOCK_MODIFICATIONS)?.let { collection ->
        // 同步块，确保在同一时间只有一个线程可以修改集合，避免并发修改问题
        synchronized(collection) {
            // 清空块修改记录，以释放资源并准备下一次使用
            collection.clear()
        }
    }
}


/**
 * 从CjFile中移除指定的块修改元素
 * 此函数主要用于清理文件中不再需要的块修改信息，以保持数据结构的准确性和有效性
 *
 * @param blockModifications 一组CjElement对象，表示要从文件中移除的块修改元素
 * 如果传入的集合为空，函数将直接返回，不执行任何操作
 */
fun CjFile.removeInBlockModifications(blockModifications: Collection<CjElement>) {
    // 检查传入的集合是否为空，如果为空则直接返回
    if (blockModifications.isEmpty()) return

    // 使用安全的并发访问方法来修改IN_BLOCK_MODIFICATIONS中的元素
    getUserData(IN_BLOCK_MODIFICATIONS)?.let { collection ->
        // 同步集合以防止并发修改问题
        synchronized(collection) {
            // 将blockModifications转换为Set以优化性能，然后从collection中移除所有对应的元素
            collection.removeAll(blockModifications.toSet())
        }
    }
}


@Service(Service.Level.PROJECT)
class PureCangJieCodeBlockModificationListener(val project: Project) : Disposable {

    private val outOfCodeBlockModificationTrackerImpl = SimpleModificationTracker()
    val outOfCodeBlockModificationTracker: ModificationTracker = outOfCodeBlockModificationTrackerImpl

    companion object {
        /**
         * 获取 PureCangJieCodeBlockModificationListener 的实例
         * 此函数通过项目服务容器获取 PureCangJieCodeBlockModificationListener 的实例，确保了在整个项目中监听器的单一实例化
         * 使用项目服务容器进行实例化，可以保证实例的延迟加载和唯一性，同时避免了直接构造实例可能导致的资源浪费或状态不一致问题
         *
         * @param project 项目实例，用于从项目服务容器中获取 PureCangJieCodeBlockModificationListener 实例
         * @return 返回一个 PureCangJieCodeBlockModificationListener 实例，该实例用于监听代码块的修改事件
         */
        fun getInstance(project: Project): PureCangJieCodeBlockModificationListener = project.service()

        /**
         * 判断给定的虚拟文件是否为CangJie控制台文件
         *
         * 此函数通过检查文件是否具有特定的用户数据键[CANGJIE_CONSOLE_KEY]来确定文件是否由CangJie控制台创建
         * 它用于在内部区分哪些文件是用户在CangJie控制台中输入的代码文件
         *
         * @param file 虚拟文件实例，用于检查是否为CangJie控制台文件
         * @return Boolean 如果文件是CangJie控制台文件，则返回true；否则返回false
         */
        private fun isReplLine(file: VirtualFile): Boolean = file.getUserData(CANGJIE_CONSOLE_KEY) == true

        /**
         * 增加文件修改计数
         * 此函数用于在文件被修改时增加其修改计数，以便于跟踪文件的变更情况
         * 它通过在文件的用户数据中存储一个修改跟踪器来实现这一点
         *
         * @param file 要增加修改计数的文件
         */
        private fun incFileModificationCount(file: CjFile) {
            // 获取或创建文件的修改跟踪器
            val tracker = file.getUserData(PER_FILE_MODIFICATION_TRACKER)
                ?: file.putUserDataIfAbsent(PER_FILE_MODIFICATION_TRACKER, SimpleModificationTracker())

            // 增加修改计数
            tracker.incModificationCount()
        }

        /**
         * 处理代码块内的修改。
         * 该函数用于处理特定代码块内元素的修改，主要用于结构化文本环境中的解析和修改。
         * 首先检查传入的元素是否有效，如果任何一个元素无效，则直接返回空列表。
         *
         * @param elements 要处理的 AST 节点数组
         * @return 包含修改后的 CjElement 列表
         */
        private fun inBlockModifications(elements: Array<ASTNode>): List<CjElement> {
            if (elements.any { !it.psi.isValid }) return emptyList()

            // 当代码片段重新解析时，IntelliJ 不会进行 AST 差异比较，而是认为整个内容都被替换，
            // 这在 POM 事件中表示为一个空的更改元素列表。

            return elements.map { element ->
                val modificationScope = getInsideCodeBlockModificationScope(element.psi) ?: return emptyList()
                modificationScope.blockDeclaration
            }
        }


        /**
         * 判断特定的树变化事件是否满足给定的预设条件
         * 此函数用于检查在一个树变化事件中，所有更改的元素及其受影响的子元素是否都满足一个特定的预设条件
         * 它通过对每个更改的元素及其受影响的子元素应用预设条件，并检查旧子节点是否也满足预设条件，来确定变化是否特定
         *
         * @param changeSet 树变化事件，包含更改的信息
         * @param precondition 预设条件，一个接受ASTNode?并返回Boolean的函数
         * @return 如果所有更改的元素及其受影响的子元素（包括旧子节点）都满足预设条件，则返回true；否则返回false
         */
        private fun isSpecificChange(changeSet: TreeChangeEvent, precondition: (ASTNode?) -> Boolean): Boolean =
            changeSet.changedElements.all { changedElement ->
                // 获取当前更改元素的所有变化信息
                val changesByElement = changeSet.getChangesByElement(changedElement)
                changesByElement.affectedChildren.all { affectedChild ->
                    // 检查受影响的子元素是否满足预设条件，并且该子元素的变化信息存在
                    precondition(affectedChild) && changesByElement.getChangeByChild(affectedChild)
                        .let { changeByChild ->
                            if (changeByChild is ChangeInfoImpl) {
                                // 如果变化信息是ChangeInfoImpl类型，进一步检查旧子节点是否满足预设条件
                                val oldChild = changeByChild.oldChildNode
                                precondition(oldChild)
                            } else false
                        }
                }
            }

        /**
         * 判断更改事件是否涉及注释的变更
         *
         * 此函数用于分析给定的树更改事件，判断其中是否包含注释节点的变更
         * 主要用于在一系列更改事件中筛选出那些影响注释的更改
         *
         * @param changeSet 树更改事件对象，包含更改的节点信息
         * @return Boolean 如果更改事件中包含注释节点的变更，则返回true；否则返回false
         */
        private fun isCommentChange(changeSet: TreeChangeEvent): Boolean =
            isSpecificChange(changeSet) { it is PsiComment }

        /**
         * 判断更改集是否仅涉及格式化变化
         *
         * 此函数用于分析给定的树更改事件所包含的更改集，判断这些更改是否仅限于格式化变化
         * 格式化变化是指那些仅影响代码布局、缩进或空白行等，而不改变代码逻辑或结构的更改
         *
         * @param changeSet 树更改事件，包含一系列更改信息
         * @return Boolean 如果更改集仅包含格式化变化，则返回true；否则返回false
         */
        private fun isFormattingChange(changeSet: TreeChangeEvent): Boolean =
            isSpecificChange(changeSet) { it is PsiWhiteSpace }

        /**
         * 获取代码块内部修改的脏范围
         *
         * 该函数旨在获取指定元素内部，用于修改的脏范围（dirty scope）这通常用于在代码分析或重构时，
         * 确定哪些部分的代码需要被更新或重新解析此函数与[getInsideCodeBlockModificationScope]保持一致，
         * 以便分析结果能够在脏范围内得到反映两者的唯一区别在于空白和注释
         *
         * @param element 需要分析的 PSI 元素如果元素不是物理存在的，则返回 null
         * @return 返回用于修改的脏范围的 PSI 元素，如果没有有效的范围，则返回 null
         */
        fun getInsideCodeBlockModificationDirtyScope(element: PsiElement): PsiElement? {
            // 如果元素不是物理存在的，则直接返回 null
            if (!element.isPhysical) return null

            // 对于空白和注释，脏范围就是元素本身
            if (element is PsiWhiteSpace || element is PsiComment) return element

            // 对于其他类型的元素，返回其用于修改的范围的 blockDeclaration
            return getInsideCodeBlockModificationScope(element)?.blockDeclaration
        }

        /**
         * 获取内部代码块的修改范围
         * 该函数旨在确定给定元素所在的代码块的修改范围，用于后续的代码分析或重构
         *
         * @param element PsiElement对象，表示代码中的一个元素，可以是任何代码结构，如类、方法、变量等
         * @return BlockModificationScopeElement? 返回一个BlockModificationScopeElement对象，表示修改范围，如果没有合适的范围则返回null
         */
        fun getInsideCodeBlockModificationScope(element: PsiElement): BlockModificationScopeElement? {
            // 检查是否在lambda表达式中，如果是，则进一步检查是否在特定的上下文中
            val lambda = element.findTopmostParentOfType<CjLambdaExpression>()
            if (lambda is CjLambdaExpression) {
                lambda.findTopmostParentOfType<CjSuperTypeCallEntry>()?.findTopmostParentOfType<CjTypeStatement>()
                    ?.let {
                        return BlockModificationScopeElement(it, it)
                    }
            }

            // 查找文件中顶级的声明，如果找不到，则返回null
            val blockDeclaration =
                element.findTopmostParentInFile { isBlockDeclaration(it) } as? CjDeclaration ?: return null

            // 如果声明是局部的，则不考虑其修改范围
            if (CjPsiUtil.isLocal(blockDeclaration))
                return null

            // 获取声明的直接父类或对象
            val directParentClassOrObject = PsiTreeUtil.getParentOfType(blockDeclaration, CjTypeStatement::class.java)
            val parentClassOrObject = directParentClassOrObject

            // 根据不同的声明类型，确定修改范围
            when (blockDeclaration) {

                is CjSecondaryConstructor -> {
                    // 对于次构造函数，检查元素是否在其身体表达式或委托调用中
                    blockDeclaration.takeIf {
                        it.bodyExpression?.isAncestor(element) ?: false || it.getDelegationCallOrNull()
                            ?.isAncestor(element) ?: false
                    }?.let { cjConstructor ->
                        parentClassOrObject?.let {
                            return if (parentClassOrObject == directParentClassOrObject) {
                                BlockModificationScopeElement(it, cjConstructor)
                            } else {
                                BlockModificationScopeElement(parentClassOrObject, cjConstructor)
                            }
                        }
                    }
                }

                is CjFunction -> {
                    // 对于函数，如果其有块体，则进一步检查元素是否在块体中
                    if (blockDeclaration.hasBlockBody()) {
                        if(blockDeclaration is CjFunctionImpl<*, *>){
                            if(blockDeclaration.isInferReturnType) return null
                        }
                        return blockDeclaration.bodyExpression
                            ?.takeIf { it.isAncestor(element) }
                            ?.let {
                                if (parentClassOrObject == directParentClassOrObject) {
                                    BlockModificationScopeElement(blockDeclaration, it)
                                } else if (parentClassOrObject != null) {
                                    BlockModificationScopeElement(parentClassOrObject, it)
                                } else null
                            }
                    }
                }

                is CjVariable -> {
                    // 目前不处理变量声明
                }

                is CjProperty -> {
                    // 对于属性，如果其有类型引用，并且不在特定的上下文中，则进一步检查
                    if (blockDeclaration.typeReference != null &&
                        (parentClassOrObject == null || element !is CjConstantExpression)
                    ) {
                        // 如果元素不是注释，则检查其是否在访问器或初始化器中
                        if (element !is CjAnnotated || element.annotationEntries.isEmpty()) {
                            val properExpression = blockDeclaration.accessors
                                .firstOrNull { (it.initializer ?: it.bodyExpression).isAncestor(element) }
                                ?: blockDeclaration.initializer?.takeIf {
                                    it.isAncestor(element) && !it.anyDescendantOfType<CjNameReferenceExpression>()
                                }

                            if (properExpression != null) {
                                val declaration =
                                    blockDeclaration.findTopmostParentOfType<CjTypeStatement>() as? CjElement

                                if (declaration != null) {
                                    return if (parentClassOrObject == directParentClassOrObject) {
                                        BlockModificationScopeElement(declaration, properExpression)
                                    } else if (parentClassOrObject != null) {
                                        BlockModificationScopeElement(parentClassOrObject, properExpression)
                                    } else null
                                }
                            }
                        }
                    }
                }

                is CjClassInitializer -> {
                    // 对于类初始化器，检查元素是否在其内部
                    blockDeclaration
                        .takeIf { it.isAncestor(element) }
                        ?.let { cjClassInitializer ->
                            parentClassOrObject?.let {
                                return if (parentClassOrObject == directParentClassOrObject) {
                                    BlockModificationScopeElement(it, cjClassInitializer)
                                } else {
                                    BlockModificationScopeElement(parentClassOrObject, cjClassInitializer)
                                }
                            }
                        }
                }

                else -> throw IllegalStateException()
            }

            // 如果没有匹配的修改范围，则返回null
            return null
        }

        /**
         * 表示一个区块修改作用域元素
         * 用于封装一个区块声明及其内部的特定元素
         * 主要用途是在于描述在代码结构中，哪些元素可以被修改以及如何修改
         *
         * @param blockDeclaration 区块声明元素，代表一个代码区块，如类、方法等
         * @param element 当前关注的元素，通常是区块内的具体代码元素，如变量、语句等
         */
        data class BlockModificationScopeElement(val blockDeclaration: CjElement, val element: CjElement)

        /**
         * 判断给定的声明是否为块声明
         * 块声明包括属性访问器、变量、函数、类初始化器等
         *
         * @param declaration 要检查的声明对象
         * @return 如果声明是块声明则返回true，否则返回false
         */
        fun isBlockDeclaration(declaration: PsiElement): Boolean {
            // 检查声明是否为属性访问器、变量、函数或类初始化器
            return declaration is CjPropertyAccessor || declaration is CjVariable ||
                    declaration is CjFunction ||
                    declaration is CjClassInitializer
        }
    }

    init {
        val treeAspect: TreeAspect = TreeAspect.getInstance(project)
        val model = PomManager.getModel(project)

        model.addModelListener(
            object : PomModelListener {
                override fun isAspectChangeInteresting(aspect: PomModelAspect): Boolean = aspect == treeAspect

                /**
                 * 当模型发生变化时调用此方法，处理不同类型的变更事件。
                 *
                 * @param event 发生变更的事件对象
                 */
                override fun modelChanged(event: PomModelEvent) {
                    // 获取变更集，如果为空则直接返回
                    val changeSet = event.getChangeSet(treeAspect) as TreeChangeEvent? ?: return

                    // 获取变更集中的文件，如果文件类型不符合则直接返回
                    val cjFile = changeSet.rootElement.psi.containingFile as? CjFile ?: return

                    // 增加文件修改计数
                    incFileModificationCount(cjFile)

                    // 获取变更的元素列表
                    val changedElements = changeSet.changedElements

                    // 如果有变更的元素
                    if (changedElements.isNotEmpty()) {
                        // 忽略格式化和注释的变更
                        if (isFormattingChange(changeSet) || isCommentChange(changeSet)) return
                    }

                    // 获取在代码块内的变更元素
                    val inBlockElements = inBlockModifications(changedElements)
                    val physicalFile = cjFile.isPhysical

                    // 如果没有在代码块内的变更元素
                    if (inBlockElements.isEmpty()) {
                        // 检查是否为物理文件且不是REPL行
                        val physical = physicalFile && !isReplLine(cjFile.virtualFile)
                        // 增加文件外部代码块的修改计数
                        cjFile.incOutOfBlockModificationCount()

                        // 处理文件变更
                        didChangeCangJieCode(cjFile, physical)
                    } else if (physicalFile) {
//                        didChangeCangJieCode(cjFile, false)

                        // 遍历在代码块内的变更元素，并添加到对应的文件中
                        inBlockElements.forEach { it.getContainingCjFile().addInBlockModifiedItem(it) }
                    }
                }

            },
            this,
        )
        project.messageBus.connect(this).subscribe(FileDocumentManagerListener.TOPIC, object :
            FileDocumentManagerListener {
            override fun fileWithNoDocumentChanged(file: VirtualFile) {
      //没有文档意味着没有 pomModel 的变化
//如果 psi 没有加载，那么计数会通过 PsiTreeChangeEvent.PROP_UNLOADED_PSI 进行修改
//如果 psi 已经加载，那么会触发 [FileManagerImpl.reloadPsiAfterTextChange]，但它并不会提供任何明确的变化，
//因此需要增加跟踪器，确保外部没有发生任何显著变化
                CangJieCodeBlockModificationListener.getInstance(project).incModificationCount()
            }
        })
    }

    /**
     * 当CangJie编码发生变化时调用此方法
     *
     * 此方法关注于处理CangJie编码的变更，特别是区分了物理文件的变更和逻辑变更
     * 它通过更新修改计数来通知监听器和相关的更新器，以便它们能够相应地响应这些变更
     *
     * @param cjFile 发生变更的CangJie文件对象，代表了一个CangJie编码文件
     * @param physical 一个布尔值，指示变更是否是物理性的如果为true，表示文件在磁盘上实际发生了变化
     */
    private fun didChangeCangJieCode(cjFile: CjFile, physical: Boolean) {
        if (physical) {
            // 当物理文件发生变更时，增加修改计数，以跟踪文件的变更次数
            outOfCodeBlockModificationTrackerImpl.incModificationCount()
            // 获取CangJie编码块修改监听器的实例，并增加其修改计数，以便监听器能够检测到变更
            CangJieCodeBlockModificationListener.getInstance(project).incModificationCount()
            // 调用更新器来处理CangJie物理文件在代码块之外的变更，true表示这是一个增加的变更
            CangJieModuleOutOfCodeBlockModificationTracker.getUpdaterInstance(project)
                .onCangJiePhysicalFileOutOfBlockChange(cjFile, true)
        }
    }

    override fun dispose() = Unit
}

private val FILE_OUT_OF_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")

private fun CjFile.incOutOfBlockModificationCount() {
    clearInBlockModifications()

    val count = getUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT) ?: 0
    putUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT, count + 1)
}
