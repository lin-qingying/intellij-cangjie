package com.linqingying.cangjie.ide.refactoring.move

import com.linqingying.cangjie.name.FqName

sealed interface MoveContainerInfo {
    val fqName: FqName?

    object UnknownPackage : MoveContainerInfo {
        override val fqName: FqName? = null
    }

    class Package(override val fqName: FqName) : MoveContainerInfo {
        override fun equals(other: Any?) = other is Package && other.fqName == fqName

        override fun hashCode() = fqName.hashCode()
    }

    class Class(override val fqName: FqName) : MoveContainerInfo {
        override fun equals(other: Any?) = other is Class && other.fqName == fqName

        override fun hashCode() = fqName.hashCode()
    }
}

data class MoveContainerChangeInfo(val oldContainer: MoveContainerInfo, val newContainer: MoveContainerInfo)
