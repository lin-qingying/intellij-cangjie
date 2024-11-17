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

package com.linqingying.cangjie.ide.cache.trackers

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
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.anyDescendantOfType

private val FILE_IN_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_IN_BLOCK_MODIFICATION_COUNT")

private val IN_BLOCK_MODIFICATIONS = Key<MutableCollection<CjElement>>("IN_BLOCK_MODIFICATIONS")
private fun CjFile.addInBlockModifiedItem(element: CjElement) {
    val collection = putUserDataIfAbsent(IN_BLOCK_MODIFICATIONS, mutableSetOf())
    synchronized(collection) {
        val needToAddBlock = collection.none { it.isAncestor(element, strict = false) }
        if (needToAddBlock) {
            collection.removeIf { element.isAncestor(it, strict = false) }
            collection.add(element)
        }
    }
    val count = getUserData(FILE_IN_BLOCK_MODIFICATION_COUNT) ?: 0
    putUserData(FILE_IN_BLOCK_MODIFICATION_COUNT, count + 1)
}

val CjFile.inBlockModifications: Collection<CjElement>
    get() {
        val collection = getUserData(IN_BLOCK_MODIFICATIONS) ?: return emptyList()
        return synchronized(collection) {
            if (collection.isNotEmpty()) {
                ArrayList(collection)
            } else {
                emptyList()
            }
        }
    }


fun CjFile.clearInBlockModifications() {
    getUserData(IN_BLOCK_MODIFICATIONS)?.let { collection ->
        synchronized(collection) {
            collection.clear()
        }
    }
}

fun CjFile.removeInBlockModifications(blockModifications: Collection<CjElement>) {
    if (blockModifications.isEmpty()) return

    getUserData(IN_BLOCK_MODIFICATIONS)?.let { collection ->
        synchronized(collection) {
            collection.removeAll(blockModifications.toSet())
        }
    }
}


@Service(Service.Level.PROJECT)
class PureCangJieCodeBlockModificationListener(val project: Project) : Disposable {

    private val outOfCodeBlockModificationTrackerImpl = SimpleModificationTracker()
    val outOfCodeBlockModificationTracker: ModificationTracker = outOfCodeBlockModificationTrackerImpl

    companion object {
        fun getInstance(project: Project): PureCangJieCodeBlockModificationListener = project.service()

        private fun isReplLine(file: VirtualFile): Boolean = file.getUserData(CANGJIE_CONSOLE_KEY) == true

        private fun incFileModificationCount(file: CjFile) {
            val tracker = file.getUserData(PER_FILE_MODIFICATION_TRACKER)
                ?: file.putUserDataIfAbsent(PER_FILE_MODIFICATION_TRACKER, SimpleModificationTracker())

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


        private fun isSpecificChange(changeSet: TreeChangeEvent, precondition: (ASTNode?) -> Boolean): Boolean =
            changeSet.changedElements.all { changedElement ->
                val changesByElement = changeSet.getChangesByElement(changedElement)
                changesByElement.affectedChildren.all { affectedChild ->
                    precondition(affectedChild) && changesByElement.getChangeByChild(affectedChild)
                        .let { changeByChild ->
                            if (changeByChild is ChangeInfoImpl) {
                                val oldChild = changeByChild.oldChildNode
                                precondition(oldChild)
                            } else false
                        }
                }
            }

        private fun isCommentChange(changeSet: TreeChangeEvent): Boolean =
            isSpecificChange(changeSet) { it is PsiComment }

        private fun isFormattingChange(changeSet: TreeChangeEvent): Boolean =
            isSpecificChange(changeSet) { it is PsiWhiteSpace }

        /**
         * Has to be aligned with [getInsideCodeBlockModificationScope] :
         *
         * result of analysis has to be reflected in dirty scope,
         * the only difference is whitespaces and comments
         */
        fun getInsideCodeBlockModificationDirtyScope(element: PsiElement): PsiElement? {
            if (!element.isPhysical) return null
            // dirty scope for whitespaces and comments is the element itself
            if (element is PsiWhiteSpace || element is PsiComment) return element

            return getInsideCodeBlockModificationScope(element)?.blockDeclaration
        }

        fun getInsideCodeBlockModificationScope(element: PsiElement): BlockModificationScopeElement? {
            val lambda = element.findTopmostParentOfType<CjLambdaExpression>()
            if (lambda is CjLambdaExpression) {
                lambda.findTopmostParentOfType<CjSuperTypeCallEntry>()?.findTopmostParentOfType<CjTypeStatement>()
                    ?.let {
                        return BlockModificationScopeElement(it, it)
                    }
            }

            val blockDeclaration =
                element.findTopmostParentInFile { isBlockDeclaration(it) } as? CjDeclaration ?: return null
            //                CjPsiUtil.getTopmostParentOfType<CjClassOrObject>(element) as? CjDeclaration ?: return null

            // should not be local declaration
            if (CjPsiUtil.isLocal(blockDeclaration))
                return null

            val directParentClassOrObject = PsiTreeUtil.getParentOfType(blockDeclaration, CjTypeStatement::class.java)
            val parentClassOrObject = directParentClassOrObject
//                ?.takeIf { !it.isTopLevel() && it.hasModifier(CjTokens.INNER_KEYWORD) }?.let {
//                    var e: CjTypeStatement? = it
//                    while (e != null) {
//                        e = PsiTreeUtil.getParentOfType(e, CjTypeStatement::class.java)
//                        if (e?.hasModifier(CjTokens.INNER_KEYWORD) == false) {
//                            break
//                        }
//                    }
//                    e
//                } ?: directParentClassOrObject

            when (blockDeclaration) {

                is CjSecondaryConstructor -> {
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
                    //                    if (blockDeclaration.visibilityModifierType()?.toVisibility() == Visibilities.PRIVATE) {
                    //                        topClassLikeDeclaration(blockDeclaration)?.let {
                    //                            return BlockModificationScopeElement(it, it)
                    //                        }
                    //                    }
                    if (blockDeclaration.hasBlockBody()) {
                        // case like `fun foo(): String {...<caret>...}`
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

                }

                is CjProperty -> {
                    //                    if (blockDeclaration.visibilityModifierType()?.toVisibility() == Visibilities.PRIVATE) {
                    //                        topClassLikeDeclaration(blockDeclaration)?.let {
                    //                            return BlockModificationScopeElement(it, it)
                    //                        }
                    //                    }

                    if (blockDeclaration.typeReference != null &&

                        //  FE does not report CONSTANT_EXPECTED_TYPE_MISMATCH within a property within a class
                        (parentClassOrObject == null || element !is CjConstantExpression)
                    ) {

                        // adding annotations to accessor is the same as change contract of property
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


                //                is CjClassOrObject -> {
                //                    return when (element) {
                //                        is CjProperty, is CjNamedFunction -> {
                //                            if ((element as? CjModifierListOwner)?.visibilityModifierType()?.toVisibility() == Visibilities.PRIVATE)
                //                                BlockModificationScopeElement(blockDeclaration, blockDeclaration) else null
                //                        }
                //                        else -> null
                //                    }
                //                }

                else -> throw IllegalStateException()
            }

            return null
        }

        data class BlockModificationScopeElement(val blockDeclaration: CjElement, val element: CjElement)

        fun isBlockDeclaration(declaration: PsiElement): Boolean {
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

                override fun modelChanged(event: PomModelEvent) {
                    val changeSet = event.getChangeSet(treeAspect) as TreeChangeEvent? ?: return
                    val cjFile = changeSet.rootElement.psi.containingFile as? CjFile ?: return

                    incFileModificationCount(cjFile)

                    val changedElements = changeSet.changedElements

                    // skip change if it contains only virtual/fake change
                    if (changedElements.isNotEmpty()) {
                        // ignore formatting (whitespaces etc)
                        if (isFormattingChange(changeSet) || isCommentChange(changeSet)) return
                    }

                    val inBlockElements = inBlockModifications(changedElements)
                    val physicalFile = cjFile.isPhysical

                    if (inBlockElements.isEmpty()) {
                        val physical = physicalFile && !isReplLine(cjFile.virtualFile)
                        cjFile.incOutOfBlockModificationCount()

                        didChangeCangJieCode(cjFile, physical)
                    } else if (physicalFile) {
                        inBlockElements.forEach { it.getContainingCjFile().addInBlockModifiedItem(it) }
                    }
                }
            },
            this,
        )
        project.messageBus.connect(this).subscribe(FileDocumentManagerListener.TOPIC, object :
            FileDocumentManagerListener {
            override fun fileWithNoDocumentChanged(file: VirtualFile) {
                //no document means no pomModel change
                //if psi was not loaded, then the count would be modified by PsiTreeChangeEvent.PROP_UNLOADED_PSI
                //if psi was loaded, then [FileManagerImpl.reloadPsiAfterTextChange] is fired which doesn't provide any explicit change anyway,
                //so one need to inc the tracker to ensure that nothing significant was changed externally
                CangJieCodeBlockModificationListener.getInstance(project).incModificationCount()
            }
        })
    }

    private fun didChangeCangJieCode(cjFile: CjFile, physical: Boolean) {
        if (physical) {
            outOfCodeBlockModificationTrackerImpl.incModificationCount()
            CangJieCodeBlockModificationListener.getInstance(project).incModificationCount()
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
