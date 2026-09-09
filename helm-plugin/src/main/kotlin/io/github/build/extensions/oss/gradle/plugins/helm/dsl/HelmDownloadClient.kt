package io.github.build.extensions.oss.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.provider.Property
import build.extensions.oss.gradle.pluginutils.booleanProviderFromProjectProperty
import build.extensions.oss.gradle.pluginutils.property
import build.extensions.oss.gradle.pluginutils.providerFromProjectProperty
import javax.inject.Inject


/**
 * Configures downloading of the Helm client executable, as an alternative to specifying the path to a
 * local executable.
 */
interface HelmDownloadClient {

    companion object {

        /**
         * Default version of the Helm client executable. This is the latest version available at the time
         * the plugin is released.
         */
        @JvmStatic
        val DEFAULT_HELM_CLIENT_VERSION = "3.7.1"
    }

    /**
     * Whether to download the Helm client. Defaults to `false`.
     *
     * Can be configured using the `helm.client.download.enabled` project property.
     */
    val enabled: Property<Boolean>

    /**
     * The version of the client to be downloaded.
     *
     * Defaults to the latest version available at the time of the plugin release (currently `4.2.4`).
     *
     * @see DEFAULT_HELM_CLIENT_VERSION
     */
    val version: Property<String>
}


/**
 * This holds only the user-facing settings; locating the download and extract tasks for [version] is done by
 * the `HelmCommandsPlugin`, which has a [Project] to do it with at configuration time.
 *
 * Note that [project] is intentionally a plain constructor parameter rather than a property: it is used only
 * to build the conventions below, and keeping a reference to it in a field would make every task that wires a
 * convention from the `helm` extension unserializable for the configuration cache.
 */
internal open class DefaultHelmDownloadClient
@Inject constructor(
    project: Project
) : HelmDownloadClient {

    override val enabled: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(
                project.booleanProviderFromProjectProperty("helm.client.download.enabled", false)
            )


    final override val version: Property<String> =
        project.objects.property<String>()
            .convention(
                project.providerFromProjectProperty(
                    "helm.client.download.version", HelmDownloadClient.DEFAULT_HELM_CLIENT_VERSION
                )
            )
}
