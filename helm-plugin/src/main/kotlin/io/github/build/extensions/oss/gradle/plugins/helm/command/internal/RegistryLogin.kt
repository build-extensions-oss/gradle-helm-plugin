package io.github.build.extensions.oss.gradle.plugins.helm.command.internal

import java.io.File
import java.io.Serializable
import java.net.URI
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmRegistry
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmRepository
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.PasswordCredentials
import io.github.build.extensions.oss.gradle.plugins.helm.util.isOciRegistry
import io.github.build.extensions.oss.gradle.plugins.helm.util.ociRegistryHost


internal data class RegistryLogin(
    val host: String,
    val username: String? = null,
    val password: String? = null,
    val caFile: File? = null,
    val certificateFile: File? = null,
    val keyFile: File? = null
) : Serializable {

    /**
     * Whether a chart dependency with the given `repository` URL is pulled from this registry.
     */
    fun servesRepository(repository: String): Boolean {
        val uri = runCatching { URI(repository) }.getOrNull()
            ?: return false
        return uri.scheme != null && uri.isOciRegistry && uri.ociRegistryHost.equals(host, ignoreCase = true)
    }


    // never print the password, e.g. in configuration cache reports or debug logs
    override fun toString(): String =
        "RegistryLogin(host=$host, username=$username, password=${password?.let { "******" }}, " +
                "caFile=$caFile, certificateFile=$certificateFile, keyFile=$keyFile)"


    private companion object {
        private const val serialVersionUID = 1L
    }
}


/**
 * Resolves the login for this registry.
 */
internal fun HelmRegistry.toRegistryLogin(): RegistryLogin =
    loginFromCredentials(
        host = requireNotNull(host.orNull) { "The host of the Helm registry \"$name\" is not set." },
        caFile = caFile.orNull?.asFile
    )


/**
 * Resolves the login for this repository, if its URL points to an OCI registry.
 */
internal fun HelmRepository.toRegistryLoginOrNull(): RegistryLogin? =
    url.orNull
        ?.takeIf { it.isOciRegistry }
        ?.let { url -> loginFromCredentials(url.ociRegistryHost, caFile.orNull?.asFile) }


private fun CredentialsContainer.loginFromCredentials(host: String, caFile: File?): RegistryLogin =
    when (val credentials = configuredCredentials.orNull) {
        null ->
            RegistryLogin(host, caFile = caFile)
        is PasswordCredentials ->
            RegistryLogin(host, credentials.username.orNull, credentials.password.orNull, caFile)
        is CertificateCredentials ->
            RegistryLogin(
                host, caFile = caFile,
                certificateFile = credentials.certificateFile.orNull?.asFile,
                keyFile = credentials.keyFile.orNull?.asFile
            )
        else ->
            throw IllegalArgumentException(
                "Only PasswordCredentials and CertificateCredentials are supported for Helm registries"
            )
    }
