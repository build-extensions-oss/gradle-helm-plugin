/**
 * Covers the registry login variants.
 */
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmRegistryLogin
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.CertificateCredentials

plugins {
    id("io.github.build-extensions-oss.helm") version "0.0.1"
}

helm {
    registries {
        // public registry: logged in to without any credentials
        create("anonymous") {
            host.set("anonymous.example.com")
        }

        // authenticates with a client certificate and has its own CA
        create("certificate") {
            host.set("certificate.example.com")
            caFile.set(file("certs/ca.pem"))
            credentials(CertificateCredentials::class.java) {
                certificateFile.set(file("certs/client.pem"))
                keyFile.set(file("certs/client.key"))
            }
        }
    }

    repositories {
        // a classic repository is added with `helm repo add`, and never logged in to
        create("classic") {
            url("https://charts.example.com")
        }

        // an OCI repository is logged in to rather than added
        create("ociWithCertificate") {
            url("oci://oci.example.com/helm-local")
            caFile.set(file("certs/ca.pem"))
            credentials(CertificateCredentials::class.java) {
                certificateFile.set(file("certs/client.pem"))
                keyFile.set(file("certs/client.key"))
            }
        }
    }

    charts {
        // API version v2: dependencies are declared in Chart.yaml
        create("main") {
            sourceDir.set(file("src/main/helm"))
        }

        // API version v1: dependencies are declared in requirements.yaml
        create("legacy") {
            sourceDir.set(file("src/legacy/helm"))
        }

        // API version v1 without requirements.yaml: there are no dependencies at all
        create("empty") {
            sourceDir.set(file("src/empty/helm"))
        }
    }
}

// An ad-hoc login with TLS files only: no username, so no password on the standard input either
tasks.register<HelmRegistryLogin>("helmTlsRegistryLogin") {
    registry.set("tls.example.com:8443")
    caFile.set(file("certs/ca.pem"))
    certificateFile.set(file("certs/client.pem"))
    keyFile.set(file("certs/client.key"))
}
