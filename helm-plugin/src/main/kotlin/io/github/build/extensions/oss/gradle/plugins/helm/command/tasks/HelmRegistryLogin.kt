package io.github.build.extensions.oss.gradle.plugins.helm.command.tasks

import org.gradle.api.file.RegularFileProperty
import io.github.build.extensions.oss.gradle.plugins.helm.command.internal.RegistryLogin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import build.extensions.oss.gradle.pluginutils.property
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault


/**
 * Logs in to an OCI registry. Corresponds to the `helm registry login` CLI command.
 *
 * For chart dependencies, prefer declaring the registry in `helm.registries`: the dependency tasks then log in
 * themselves, and only when they actually run. Use this task for anything else that needs a login, and wire it
 * with `dependsOn` - it is not connected to any other task automatically:
 *
 * ```
 * val helmRegistryLogin by tasks.registering(HelmRegistryLogin::class) {
 *     registry.set("registry.example.com")
 *     username.set(providers.environmentVariable("REGISTRY_USER"))
 *     password.set(providers.environmentVariable("REGISTRY_PASSWORD"))
 * }
 * ```
 *
 * The login is stored below the plugin's `xdgConfigHome`, which is where the other Helm tasks of the build look
 * for it - a login done in a shell outside of Gradle is not visible to them. The password is passed on the
 * standard input (`--password-stdin`), never on the command line.
 *
 * The task declares no outputs, so it is never up-to-date: a login leaves nothing that could be checked, and
 * credentials may have expired since the last build.
 */
@DisableCachingByDefault(because = "Login task should not be cached")
abstract class HelmRegistryLogin : AbstractHelmCommandTask() {

    /**
     * The registry host, optionally with a port, e.g. `registry.example.com:5000`.
     */
    @get:Input
    val registry: Property<String> =
        project.objects.property()


    /**
     * Username to access the registry.
     *
     * Corresponds to the `--username` CLI parameter.
     */
    @get:[Input Optional]
    val username: Property<String> =
        project.objects.property()


    /**
     * Password or identity token to access the registry.
     *
     * Passed to Helm on the standard input, using the `--password-stdin` CLI parameter.
     */
    @get:[Input Optional]
    val password: Property<String> =
        project.objects.property()


    /**
     * A CA bundle used to verify certificates of HTTPS-enabled registries.
     *
     * Corresponds to the `--ca-file` CLI parameter.
     */
    @get:[InputFile Optional]
    @PathSensitive(PathSensitivity.RELATIVE)
    val caFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * Path to a certificate file for client SSL authentication.
     *
     * Corresponds to the `--cert-file` CLI parameter.
     */
    @get:[InputFile Optional]
    @PathSensitive(PathSensitivity.RELATIVE)
    val certificateFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * Path to a certificate private key file for client SSL authentication.
     *
     * Corresponds to the `--key-file` CLI parameter.
     */
    @get:[InputFile Optional]
    @PathSensitive(PathSensitivity.RELATIVE)
    val keyFile: RegularFileProperty =
        project.objects.fileProperty()


    @TaskAction
    fun login() {
        loginToRegistry(
            RegistryLogin(
                host = registry.get(),
                username = username.orNull,
                password = password.orNull,
                caFile = caFile.orNull?.asFile,
                certificateFile = certificateFile.orNull?.asFile,
                keyFile = keyFile.orNull?.asFile
            )
        )
    }
}
