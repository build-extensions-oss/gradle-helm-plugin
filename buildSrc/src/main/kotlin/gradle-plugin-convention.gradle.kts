import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import kotlin.text.set

/**
 * Collect here all plugins which we need to apply for a project which represents Gradle plugin.
 */
plugins {
    id("kotlin-convention")
    id("dokka-convention")
}
