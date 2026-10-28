package io.github.build.extensions.oss.gradle.plugins.helm.command.tasks

import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import build.extensions.oss.gradle.pluginutils.property
import io.github.build.extensions.oss.gradle.plugins.helm.command.internal.RegistryLogin
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import io.github.build.extensions.oss.gradle.plugins.helm.util.isOciRegistry
import io.github.build.extensions.oss.gradle.plugins.helm.util.ociRegistryHost
import org.yaml.snakeyaml.Yaml
import java.net.URI


/**
 * Registers a known repository with Helm. Corresponds to the `helm repo add` CLI command.
 *
 * If the repository [url] is an OCI registry (`oci://...`), the task runs `helm registry login` instead:
 * OCI registries cannot be added with `helm repo add`, but they need the login before charts can be
 * pulled from them, for example by `helm dependency update`.
 */
@DisableCachingByDefault(because = "See https://github.com/build-extensions-oss/gradle-helm-plugin/issues/208")
abstract class HelmAddRepository : AbstractHelmCommandTask() {

    /**
     * Name of the repository.
     */
    @get:Input
    val repositoryName: Property<String> =
        project.objects.property()


    /**
     * URL of the repository.
     */
    @get:Input
    val url: Property<URI> =
        project.objects.property()


    /**
     * A CA bundle used to verify certificates of HTTPS-enabled servers.
     *
     * Corresponds to the `--ca-file` CLI parameter.
     */
    @get:[InputFile Optional]
    // let's consider k8s configuration is defined in the same repository - and not in the shared file at the computer.
    // Alternatively we will have a cache miss (which is correct)
    @PathSensitive(PathSensitivity.RELATIVE)
    val caFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * Username to access the chart repository.
     *
     * Corresponds to the `--username` CLI parameter.
     */
    @get:[Input Optional]
    val username: Property<String> =
        project.objects.property()


    /**
     * Password to access the chart repository.
     *
     * Corresponds to the `--password` CLI parameter.
     */
    @get:[Input Optional]
    val password: Property<String> =
        project.objects.property()


    /**
     * Path to a certificate file for client SSL authentication.
     *
     * Corresponds to the `--cert-file` CLI parameter.
     */
    @get:[InputFile Optional]
    // let's consider k8s configuration is defined in the same repository - and not in the shared file at the computer.
    // Alternatively we will have a cache miss (which is correct)
    @PathSensitive(PathSensitivity.RELATIVE)
    val certificateFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * Path to a certificate private key file for client SSL authentication.
     *
     * Corresponds to the `--key-file` CLI parameter.
     */
    @get:[InputFile Optional]
    // let's consider value files are defined in the same repository - and not in the shared file at the computer
    // Alternatively we will have a cache miss (which is correct)
    @PathSensitive(PathSensitivity.RELATIVE)
    val keyFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * If set to `true`, the task fails if the repository is already registered, instead of leaving it to Helm.
     *
     * The check reads the Helm repository configuration, so it needs no command line flag of its own; a configuration
     * file that exists but cannot be read fails the task too. It applies to classic repositories only: an OCI registry
     * is never added, so the task logs in to it either way.
     */
    @get:Internal
    val failIfExists: Property<Boolean> =
        project.objects.property()


    @TaskAction
    fun addRepository() {
        val url = this.url.get()
        if (url.isOciRegistry) {
            loginToRegistry(url)
        } else {
            addClassicRepository()
        }
    }


    private fun loginToRegistry(url: URI) {
        loginToRegistry(
            RegistryLogin(
                host = url.ociRegistryHost,
                username = username.orNull,
                password = password.orNull,
                caFile = caFile.orNull?.asFile,
                certificateFile = certificateFile.orNull?.asFile,
                keyFile = keyFile.orNull?.asFile
            )
        )
    }


    private fun addClassicRepository() {
        failIfAlreadyRegistered()

        execHelm("repo", "add") {
            option("--ca-file", caFile)
            option("--cert-file", certificateFile)
            option("--key-file", keyFile)
            option("--username", username)
            option("--password", password)
            args(repositoryName)
            args(url)
        }
    }


    private fun failIfAlreadyRegistered() {
        if (!failIfExists.getOrElse(false)) {
            return
        }

        val repositoryName = this.repositoryName.get()
        val registered = try {
            registeredRepositories()
        } catch (e: Exception) {
            throw GradleException(
                "Unable to read the Helm repository configuration ${repositoryConfigFile.get().asFile} to check " +
                    "the 'failIfExists' property.",
                e
            )
        }

        if (registered.none { it["name"] == repositoryName }) {
            return
        }

        throw GradleException(
            "Repository '$repositoryName' is already registered in ${repositoryConfigFile.get().asFile}; " +
                "the 'failIfExists' property makes the task fail."
        )
    }


    /**
     * The repositories Helm has registered, read from the repository configuration file.
     *
     * A missing file registers nothing. A file that exists but cannot be read throws, so each caller decides what a
     * damaged configuration means for it.
     */
    @Suppress("UNCHECKED_CAST")
    private fun registeredRepositories(): List<Map<String, String?>> {
        val configFile = repositoryConfigFile.get().asFile
        if (!configFile.exists()) {
            return emptyList()
        }

        return configFile.inputStream().use { input ->
            val map = Yaml().loadAs(input, Map::class.java) ?: emptyMap<String, Any>()
            map["repositories"] as? List<Map<String, String?>> ?: emptyList()
        }
    }


    init {
        outputs.file(repositoryConfigFile)
            .withPropertyName("repositoryConfigFile")
            .optional()
        outputs.upToDateWhen { task -> checkUpToDate(task) }
    }


    private fun checkUpToDate(task: Task): Boolean {

        // A registry login leaves nothing behind that we could compare against (and the credentials may have
        // expired in the meantime), so always log in again
        if (url.get().isOciRegistry) {
            logger.debug("{} is not up-to-date because the repository is an OCI registry.", task)
            return false
        }

        // The check runs in the task action, so the task is never up to date when it is set
        if (failIfExists.getOrElse(false)) {
            logger.debug("{} is not up-to-date because the \"failIfExists\" flag is set.", task)
            return false
        }

        val actualConfig = loadRepositoryConfig()
        if (actualConfig == null) {
            logger.debug("{} is not up-to-date because the desired repository configuration does not exist.", task)
            return false
        }

        val expectedConfig = RepositoryConfig(
            name = repositoryName.getOrElse(""),
            url = url.map { it.toString() }.getOrElse(""),
            username = username.getOrElse(""),
            password = password.getOrElse(""),
            caFile = caFile.map { it.asFile.absolutePath }.getOrElse(""),
            certFile = certificateFile.map { it.asFile.absolutePath }.getOrElse(""),
            keyFile = keyFile.map { it.asFile.absolutePath }.getOrElse("")
        )

        return if (actualConfig == expectedConfig) {
            logger.debug(
                "{} is up-to-date because the current repository configuration matches the desired one.", task
            )
            true
        } else {
            logger.debug(
                "{} is not up-to-date because the current repository configuration does not match the desired one.",
                task
            )
            false
        }
    }


    private fun loadRepositoryConfig(): RepositoryConfig? =
        runCatching {
            registeredRepositories()
                .firstOrNull { it["name"] == repositoryName.get() }
                ?.let { RepositoryConfig.fromMap(it) }
        }.getOrNull()


    private data class RepositoryConfig(
        val name: String,
        val url: String,
        val username: String,
        val password: String,
        val caFile: String,
        val certFile: String,
        val keyFile: String
    ) {

        companion object {

            fun fromMap(map: Map<String, String?>) = RepositoryConfig(
                name = map["name"] ?: "",
                url = map["url"] ?: "",
                username = map["username"] ?: "",
                password = map["password"] ?: "",
                caFile = map["caFile"] ?: "",
                certFile = map["certFile"] ?: "",
                keyFile = map["keyFile"] ?: ""
            )
        }
    }
}
