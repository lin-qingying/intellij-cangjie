@file:JvmName("RepoDependencies")

import org.gradle.api.Project
import org.gradle.kotlin.dsl.project


@JvmOverloads
fun Project.kotlinStdlib(suffix: String? = null, classifier: String? = null): Any {
//    return if (kotlinBuildProperties.useBootstrapStdlib)
//        kotlinDep(listOfNotNull("stdlib", suffix).joinToString("-"), bootstrapKotlinVersion, classifier)
//    else
    return dependencies.project(listOfNotNull(":kotlin-stdlib", suffix).joinToString("-"), classifier)
}

object EmbeddedComponents {
    const val CONFIGURATION_NAME = "embedded"
}
