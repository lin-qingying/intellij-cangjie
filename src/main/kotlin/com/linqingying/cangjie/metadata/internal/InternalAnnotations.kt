
package com.linqingying.cangjie.metadata.internal

/**
 * Workaround for https://github.com/Kotlin/binary-compatibility-validator/issues/104:
 *
 * When internal declaration has some classes that were relocated by shadowJar plugin,
 * binary-compatibility-validator can't filter them out.
 * Therefore, we explicitly exclude declarations marked with this annotation.
 */
@Retention(AnnotationRetention.BINARY)
internal annotation class IgnoreInApiDump
