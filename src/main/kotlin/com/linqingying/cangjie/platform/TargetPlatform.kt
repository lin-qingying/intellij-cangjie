package com.linqingying.cangjie.platform

open class TargetPlatform(val componentPlatforms: Set<SimplePlatform>) : Collection<SimplePlatform> by componentPlatforms
abstract class SimplePlatform(val platformName: String) {
    override fun toString(): String {
        val targetName = targetName
        return if (targetName.isNotEmpty()) "$platformName ($targetName)" else platformName
    }

    // description of TargetPlatformVersion or name of custom platform-specific target; used in serialization
    open val targetName: String
        get() = targetPlatformVersion.description

    /** See KDoc for [TargetPlatform.oldFashionedDescription] */
    abstract val oldFashionedDescription: String

    // FIXME(dsavvinov): hack to allow injection inject JvmTarget into container.
    //   Proper fix would be to rewrite clients to get JdkPlatform from container, and pull JvmTarget from it
    //   (this will also remove need in TargetPlatformVersion as the whole, and, in particular, ugly passing
    //   of TargetPlatformVersion.NoVersion in non-JVM code)
    open val targetPlatformVersion: TargetPlatformVersion = TargetPlatformVersion.NoVersion
}
interface TargetPlatformVersion {
    val description: String

    object NoVersion : TargetPlatformVersion {
        override val description = ""
    }
}
