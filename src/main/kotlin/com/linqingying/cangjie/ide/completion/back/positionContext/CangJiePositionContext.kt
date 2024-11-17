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

package com.linqingying.cangjie.ide.completion.back.positionContext

import com.linqingying.cangjie.references.mainReference
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getReceiverExpression
import com.linqingying.cangjie.references.CjReference
import com.linqingying.cangjie.references.CjSimpleNameReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.parentOfType

sealed class CangJieRawPositionContext {
    abstract val position: PsiElement
}

sealed class CangJieNameReferencePositionContext : CangJieRawPositionContext() {
    abstract val reference: CjReference
    abstract val nameExpression: CjElement
    abstract val explicitReceiver: CjElement?

    abstract fun getName(): Name
}

sealed class CangJieSimpleNameReferencePositionContext : CangJieNameReferencePositionContext() {
    abstract override val reference: CjSimpleNameReference
    abstract override val nameExpression: CjSimpleNameExpression
    abstract override val explicitReceiver: CjExpression?

    override fun getName(): Name = nameExpression.getReferencedNameAsName()
}

class CangJiePackageDirectivePositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?,
) : CangJieSimpleNameReferencePositionContext()

class CangJieImportDirectivePositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?,
) : CangJieSimpleNameReferencePositionContext()

class CangJieCallableReferencePositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?
) : CangJieSimpleNameReferencePositionContext()

class CangJieTypeConstraintNameInWhereClausePositionContext(
    override val position: PsiElement,
    val typeParametersOwner: CjTypeParameterListOwner
) : CangJieRawPositionContext()

class CangJieIncorrectPositionContext(
    override val position: PsiElement
) : CangJieRawPositionContext()

class CangJieUnknownPositionContext(
    override val position: PsiElement
) : CangJieRawPositionContext()
class CangJieSimpleParameterPositionContext(
    override val position: PsiElement,
    override val cjParameter: CjParameter,
) : CangJieValueParameterPositionContext()
sealed class CangJieValueParameterPositionContext : CangJieRawPositionContext() {
    abstract val cjParameter:CjParameter
}

class CangJieClassifierNamePositionContext(
    override val position: PsiElement,
    val classLikeDeclaration: CjClassLikeDeclaration,
) : CangJieRawPositionContext()

class CangJieWithSubjectEntryPositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?,
    val subjectExpression: CjExpression,
    val matchCondition: CjCasePattern,
) : CangJieSimpleNameReferencePositionContext()

/**
 * Example
 * ```
 * class A {
 *   fun test() {
 *     super.<caret>
 *   }
 * }
 * ```
 */
class CangJieSuperReceiverNameReferencePositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?,
    val superExpression: CjSuperExpression,
) : CangJieSimpleNameReferencePositionContext()

/**
 * Example
 * ```
 * class A {
 *   fun test() {
 *     super<<caret>>
 *   }
 * }
 * ```
 */
class CangJieSuperTypeCallNameReferencePositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?,
    val superExpression: CjSuperExpression,
) : CangJieSimpleNameReferencePositionContext()

class CangJieExpressionNameReferencePositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?
) : CangJieSimpleNameReferencePositionContext()

class CangJieInfixCallPositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?
) : CangJieSimpleNameReferencePositionContext()

class CangJieTypeNameReferencePositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?,
    val typeReference: CjTypeReference?,
) : CangJieSimpleNameReferencePositionContext()

class CangJieAnnotationTypeNameReferencePositionContext(
    override val position: PsiElement,
    override val reference: CjSimpleNameReference,
    override val nameExpression: CjSimpleNameExpression,
    override val explicitReceiver: CjExpression?,
    val annotationEntry: CjAnnotationEntry,
) : CangJieSimpleNameReferencePositionContext()


object CangJiePositionContextDetector {
    fun detect(position: PsiElement): CangJieRawPositionContext {
        return detectForPositionWithSimpleNameReference(position)
//            ?: detectForPositionWithKDocReference(position)
            ?: detectForPositionWithoutReference(position)
            ?: CangJieUnknownPositionContext(position)
    }

    private fun detectForPositionWithoutReference(position: PsiElement): CangJieRawPositionContext? {
        val parent = position.parent ?: return null
        val grandparent = parent.parent
        return when {
//            parent is CjClassLikeDeclaration && parent.nameIdentifier == position -> {
//                CangJieClassifierNamePositionContext(position, parent)
//            }

//            parent is CjParameter -> {
//                if (parent.ownerFunction is CjPrimaryConstructor) {
//                    CangJiePrimaryConstructorParameterPositionContext(position, parent)
//                } else {
//                    CangJieSimpleParameterPositionContext(position, parent)
//                }
//            }
//
//            parent is PsiErrorElement && grandparent is CjAbstractClassBody -> {
//                CangJieMemberDeclarationExpectedPositionContext(position, grandparent)
//            }

            else -> null
        }
    }

    private fun detectForPositionWithSimpleNameReference(position: PsiElement): CangJieRawPositionContext? {
        val reference = (position.parent as? CjSimpleNameExpression)?.mainReference
            ?: return null
        val nameExpression = reference.expression.takeIf { it !is CjLabelReferenceExpression }
            ?: return null
        val explicitReceiver = nameExpression.getReceiverExpression()
        val parent = nameExpression.parent
        val subjectExpressionForWhenCondition = (parent as? CjCasePattern)?.getSubjectExpression()

        return when {
            parent is CjUserType -> {
                detectForTypeContext(parent, position, reference, nameExpression, explicitReceiver)
            }

            parent is CjCallableReferenceExpression -> {
                CangJieCallableReferencePositionContext(
                    position, reference, nameExpression, parent.receiverExpression
                )
            }

            parent is CjCasePattern && subjectExpressionForWhenCondition != null -> {
                CangJieWithSubjectEntryPositionContext(
                    position,
                    reference,
                    nameExpression,
                    explicitReceiver,
                    subjectExpressionForWhenCondition,
                    matchCondition = parent,
                )
            }

            nameExpression.isReferenceExpressionInImportDirective() -> {
                CangJieImportDirectivePositionContext(
                    position,
                    reference,
                    nameExpression,
                    explicitReceiver,
                )
            }

            nameExpression.isNameExpressionInsidePackageDirective() -> {
                CangJiePackageDirectivePositionContext(
                    position,
                    reference,
                    nameExpression,
                    explicitReceiver,
                )
            }

            parent is CjTypeConstraint -> CangJieTypeConstraintNameInWhereClausePositionContext(
                position,
                position.parentOfType()!!,
            )

            parent is CjBinaryExpression && parent.operationReference == nameExpression -> {
                CangJieInfixCallPositionContext(
                    position, reference, nameExpression, explicitReceiver
                )
            }

            explicitReceiver is CjSuperExpression -> CangJieSuperReceiverNameReferencePositionContext(
                position,
                reference,
                nameExpression,
                explicitReceiver,
                explicitReceiver
            )

            else -> {
                CangJieExpressionNameReferencePositionContext(position, reference, nameExpression, explicitReceiver)
            }
        }
    }
//
//    private fun detectForPositionWithKDocReference(position: PsiElement): CangJieNameReferencePositionContext? {
//        val kDocName = position.getStrictParentOfType<KDocName>() ?: return null
//        val kDocLink = kDocName.getStrictParentOfType<KDocLink>() ?: return null
//        val kDocReference = kDocName.mainReference
//        val kDocNameQualifier = kDocName.getQualifier()
//
//        return when (kDocLink.getTagIfSubject()?.knownTag) {
//            KDocKnownTag.PARAM -> KDocParameterNamePositionContext(position, kDocReference, kDocName, kDocNameQualifier)
//            else -> KDocLinkNamePositionContext(position, kDocReference, kDocName, kDocNameQualifier)
//        }
//    }

    private fun CjCasePattern.getSubjectExpression(): CjExpression? {
        val whenEntry = (parent as? CjMatchEntry) ?: return null
        val whenExpression = whenEntry.parent as? CjMatchExpression ?: return null
        return whenExpression.subjectExpression
    }

    private tailrec fun CjExpression.isReferenceExpressionInImportDirective(): Boolean = when (val parent = parent) {
        is CjImportDirective -> parent.importedReference == this
        is CjDotQualifiedExpression -> parent.isReferenceExpressionInImportDirective()

        else -> false
    }

    private tailrec fun CjExpression.isNameExpressionInsidePackageDirective(): Boolean = when (val parent = parent) {
        is CjPackageDirective -> parent.packageNameExpression == this
        is CjDotQualifiedExpression -> parent.isNameExpressionInsidePackageDirective()

        else -> false
    }

    private fun detectForTypeContext(
        userType: CjUserType,
        position: PsiElement,
        reference: CjSimpleNameReference,
        nameExpression: CjSimpleNameExpression,
        explicitReceiver: CjExpression?
    ): CangJieRawPositionContext {
        val typeReference = (userType.parent as? CjTypeReference)?.takeIf { it.typeElement == userType }
        val typeReferenceOwner = typeReference?.parent
        return when {
            typeReferenceOwner is CjConstructorCalleeExpression -> {
                val constructorCall = typeReferenceOwner.takeIf { it.typeReference == typeReference }
                val annotationEntry =
                    (constructorCall?.parent as? CjAnnotationEntry)?.takeIf { it.calleeExpression == constructorCall }
                annotationEntry?.let {
                    CangJieAnnotationTypeNameReferencePositionContext(
                        position,
                        reference,
                        nameExpression,
                        explicitReceiver,
                        it
                    )
                }
            }

            typeReferenceOwner is CjSuperExpression -> {
                val superTypeCallEntry = typeReferenceOwner.takeIf { it.superTypeQualifier == typeReference }
                superTypeCallEntry?.let {
                    CangJieSuperTypeCallNameReferencePositionContext(
                        position,
                        reference,
                        nameExpression,
                        explicitReceiver,
                        it
                    )
                }
            }

            typeReferenceOwner is CjTypeConstraint && typeReferenceOwner.children.any { it is PsiErrorElement } -> {
                CangJieIncorrectPositionContext(position)
            }

            else -> null
        } ?: CangJieTypeNameReferencePositionContext(
            position,
            reference,
            nameExpression,
            explicitReceiver,
            typeReference
        )
    }
}
