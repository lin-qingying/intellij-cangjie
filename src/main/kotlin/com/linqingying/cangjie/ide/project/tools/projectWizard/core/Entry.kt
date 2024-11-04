package com.linqingying.cangjie.ide.project.tools.projectWizard.core

interface Entity {
    val path: String
}
abstract class EntityBase: Entity {
    final override fun equals(other: Any?): Boolean = other.safeAs<Entity>()?.path == path
    final override fun hashCode(): Int = path.hashCode()
    final override fun toString(): String = path
}

abstract class EntityWithValue<out T : Any> : EntityBase()
