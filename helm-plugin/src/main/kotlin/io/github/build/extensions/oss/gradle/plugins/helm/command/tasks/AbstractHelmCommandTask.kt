package io.github.build.extensions.oss.gradle.plugins.helm.command.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkerExecutor
import io.github.build.extensions.oss.gradle.plugins.helm.HELM_GROUP
import io.github.build.extensions.oss.gradle.plugins.helm.command.ConfigurableGlobalHelmOptions
import io.github.build.extensions.oss.gradle.plugins.helm.command.HelmExecProviderSupport
import io.github.build.extensions.oss.gradle.plugins.helm.command.HelmExecSpec
import io.github.build.extensions.oss.gradle.plugins.helm.command.internal.GlobalHelmOptionsApplier
import io.github.build.extensions.oss.gradle.plugins.helm.command.internal.RegistryLogin
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject


/**
 * Base class for tasks that invoke a Helm CLI command.
 *
 * All the global options below are plain task properties whose conventions are wired from the `helm`
 * extension by the `HelmCommandsPlugin`. They deliberately do _not_ hold on to the extension object itself:
 * the extension is part of the project model, and storing it in task state makes the task unserializable
 * for the configuration cache.
 */
@DisableCachingByDefault(because = "See https://github.com/build-extensions-oss/gradle-helm-plugin/issues/208")
abstract class AbstractHelmCommandTask
    : DefaultTask(), ConfigurableGlobalHelmOptions {

    init {
        group = HELM_GROUP
    }


    @get:Inject
    internal open val workerExecutor: WorkerExecutor
        get() = throw UnsupportedOperationException()


    @get:Inject
    internal open val execOperations: ExecOperations
        get() = throw UnsupportedOperationException()


    @get:Inject
    internal open val providerFactory: ProviderFactory
        get() = throw UnsupportedOperationException()


    /**
     * The name or path of the Helm executable.
     *
     * Defaults to the executable configured on the `helm` extension, falling back to the automatically
     * downloaded client (if that is enabled), and finally to `helm` as found on the `PATH`.
     */
    @get:Input
    abstract override val executable: Property<String>


    /**
     * Path of the Helm executable that was downloaded and extracted by the client download tasks.
     *
     * Has no value unless the automatic client download is enabled.
     */
    @get:Internal("represented by executable")
    internal abstract val downloadedExecutable: Property<String>


    @get:Console
    abstract override val debug: Property<Boolean>


    @get:Input
    abstract override val extraArgs: ListProperty<String>


    @get:Internal
    abstract override val xdgDataHome: DirectoryProperty


    @get:Internal
    abstract override val xdgConfigHome: DirectoryProperty


    @get:Internal
    abstract override val xdgCacheHome: DirectoryProperty


    @get:Internal
    protected val registryConfigFile: Provider<RegularFile>
        get() = xdgConfigHome.file("helm/registry.json")


    @get:Internal
    protected val repositoryCacheDir: Provider<Directory>
        get() = xdgCacheHome.dir("helm/repository")


    @get:Internal
    protected val repositoryConfigFile: Provider<RegularFile>
        get() = xdgConfigHome.file("helm/repositories.yaml")


    protected fun execHelm(
        command: String, subcommand: String? = null, action: (HelmExecSpec.() -> Unit)? = null
    ) {
        execProviderSupport.execHelm(command, subcommand, action?.let { Action(it) })
    }


    protected fun execHelmCaptureOutput(
        command: String, subcommand: String? = null, action: (HelmExecSpec.() -> Unit)? = null
    ): String =
        execProviderSupport.execHelmCaptureOutput(command, subcommand, action?.let { Action(it) })


    /**
     * Runs `helm registry login` with the given login.
     *
     * The password goes to the standard input (`--password-stdin`) rather than to the command line.
     *
     * Returns only once the login is done: Helm may run in a worker, and whatever the caller runs next (for
     * example `helm dependency update`) needs the stored login.
     */
    internal fun loginToRegistry(login: RegistryLogin) {
        logger.info("Logging in to the Helm registry {}", login.host)
        execHelm("registry", "login") {
            login.caFile?.let { option("--ca-file", it) }
            login.certificateFile?.let { option("--cert-file", it) }
            login.keyFile?.let { option("--key-file", it) }
            login.username?.let { option("--username", it) }
            login.password?.let { password ->
                flag("--password-stdin")
                standardInput(providerFactory.provider { password })
            }
            args(login.host)
        }
        workerExecutor.await()
    }


    @get:Internal
    internal open val execProviderSupport: HelmExecProviderSupport
        get() = HelmExecProviderSupport(execOperations, temporaryDir, workerExecutor, this, GlobalHelmOptionsApplier)
}
