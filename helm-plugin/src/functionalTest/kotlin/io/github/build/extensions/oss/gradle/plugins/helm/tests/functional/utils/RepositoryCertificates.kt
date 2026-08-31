package io.github.build.extensions.oss.gradle.plugins.helm.tests.functional.utils

import java.io.File
import javax.net.ssl.SSLSocketFactory
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate

/**
 * The special class to test mTLS connectivity. The goal is to generate certificates for tests to use them.
 *
 * The real use case - corporate helm repository might have independent certificates.
 */
internal object RepositoryCertificates {

    fun create(testProjectDir: File): Configuration {
        val certificateAuthority = HeldCertificate.Builder()
            .certificateAuthority(0)
            .commonName("Test Helm repository CA")
            .build()
        val serverCertificate = HeldCertificate.Builder()
            .commonName("localhost")
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .signedBy(certificateAuthority)
            .build()
        val clientCertificate = HeldCertificate.Builder()
            .commonName("Test Helm repository client")
            .signedBy(certificateAuthority)
            .build()

        val certificateDirectory = testProjectDir.resolve("repository-certificates").apply {
            check(mkdir()) { "Unable to create certificate directory at $this" }
        }
        val caCertificateFile = certificateDirectory.resolve("ca.pem").apply {
            writeText(certificateAuthority.certificatePem())
        }
        val clientCertificateFile = certificateDirectory.resolve("client.pem").apply {
            writeText(clientCertificate.certificatePem())
        }
        val clientKeyFile = certificateDirectory.resolve("client-key.pem").apply {
            writeText(clientCertificate.privateKeyPkcs8Pem())
        }
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(serverCertificate, certificateAuthority.certificate)
            .addTrustedCertificate(certificateAuthority.certificate)
            .build()

        return Configuration(
            serverSocketFactory = serverCertificates.sslSocketFactory(),
            caCertificateFile = caCertificateFile,
            clientCertificateFile = clientCertificateFile,
            clientKeyFile = clientKeyFile,
        )
    }

    data class Configuration(
        val serverSocketFactory: SSLSocketFactory,
        val caCertificateFile: File,
        val clientCertificateFile: File,
        val clientKeyFile: File,
    )
}
