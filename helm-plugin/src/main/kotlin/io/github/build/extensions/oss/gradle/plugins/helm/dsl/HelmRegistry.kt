package io.github.build.extensions.oss.gradle.plugins.helm.dsl

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.internal.CredentialsContainerSupport
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.internal.DefaultCredentialsFactory
import build.extensions.oss.gradle.pluginutils.property
import javax.inject.Inject


/**
 * Represents an OCI registry that charts are pulled from, and which needs a `helm registry login` first.
 *
 * Unlike a [HelmRepository], a registry is not registered with Helm by a task of its own. Instead, the tasks
 * that update or build chart dependencies log in to it right before they call Helm, and only if a chart
 * dependency is pulled from it (a `repository: oci://<host>/...` entry). An up-to-date or skipped task therefore
 * does not log in at all.
 */
interface HelmRegistry : Named, CredentialsContainer {

    /**
     * The registry host, optionally with a port, e.g. `registry.example.com` or `registry.example.com:5000`.
     *
     * This is what `helm registry login` expects: no scheme and no path.
     */
    val host: Property<String>


    /**
     * An optional path to a CA bundle used to verify certificates of HTTPS-enabled registries.
     */
    val caFile: RegularFileProperty
}


private open class DefaultHelmRegistry
private constructor(
    private val name: String,
    project: Project,
    credentialsContainer: CredentialsContainer
) : HelmRegistry, CredentialsContainer by credentialsContainer {

    @Inject
    constructor(project: Project, name: String)
            : this(name, project, CredentialsContainerSupport(project.objects, DefaultCredentialsFactory(project.objects)))


    final override fun getName(): String =
        name


    final override val host: Property<String> =
        project.objects.property()


    final override val caFile: RegularFileProperty =
        project.objects.fileProperty()
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmRegistry] objects.
 *
 * @receiver the Gradle [Project]
 * @return the container for `HelmRegistry` objects
 */
internal fun Project.helmRegistryContainer(): NamedDomainObjectContainer<HelmRegistry> =
    container(HelmRegistry::class.java) { name ->
        objects.newInstance(DefaultHelmRegistry::class.java, project, name)
    }
