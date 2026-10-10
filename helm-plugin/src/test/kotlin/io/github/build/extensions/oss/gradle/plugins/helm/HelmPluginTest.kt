package io.github.build.extensions.oss.gradle.plugins.helm

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.each
import assertk.assertions.extracting
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import assertk.assertions.rootCause
import io.github.build.extensions.oss.gradle.plugins.helm.command.internal.RegistryLogin
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmAddRepository
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmTemplate
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmUpdateDependencies
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmUpdateRepositories
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.Filtering
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmChart
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmExtension
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmRegistry
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmRepository
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.credentials
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.charts
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.helm
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.registries
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.repositories
import java.net.URI
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import build.extensions.oss.gradle.pluginutils.test.assertions.assertk.containsItem
import build.extensions.oss.gradle.pluginutils.test.assertions.assertk.containsTask
import build.extensions.oss.gradle.pluginutils.test.assertions.assertk.hasExtension
import build.extensions.oss.gradle.pluginutils.test.assertions.assertk.hasTaskDependencies
import build.extensions.oss.gradle.pluginutils.test.assertions.assertk.isPresent
import build.extensions.oss.gradle.pluginutils.test.assertions.assertk.taskDependencies
import build.extensions.oss.gradle.pluginutils.test.evaluate
import build.extensions.oss.gradle.pluginutils.test.spek.applyPlugin
import build.extensions.oss.gradle.pluginutils.test.spek.setupGradleProject


object HelmPluginTest : Spek({

    val project by setupGradleProject { applyPlugin<HelmPlugin>() }


    describe("applying the helm plugin") {

        it("project can be evaluated successfully") {
            project.evaluate()
        }


        it("should create a helm DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
        }


        it("should create a helm filtering DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
                .hasExtension<Filtering>("filtering")
        }


        it("should create a helm charts DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
                .hasExtension<NamedDomainObjectContainer<HelmChart>>("charts")
        }
    }


    describe("renderings") {
        it("should create a default rendering") {
            val chart = project.helm.charts.create("my-chart")
            assertThat(chart.renderings)
                .containsItem("default")
        }

        it("should create a HelmTemplate task for each rendering") {
            val chart = project.helm.charts.create("foo")
            chart.renderings.create("red")

            assertThat(project)
                .containsTask<HelmTemplate>("helmRenderFooChartRedRendering")
        }

        it("should create a task that renders all renderings for a chart") {
            val chart = project.helm.charts.create("foo")
            chart.renderings.create("red")
            chart.renderings.create("yellow")

            assertThat(project)
                .containsTask<Task>("helmRenderFooChart")
                .hasTaskDependencies(
                    "helmRenderFooChartDefaultRendering",
                    "helmRenderFooChartRedRendering",
                    "helmRenderFooChartYellowRendering",
                    exactly = true
                )
        }

        it("should create a task that renders all renderings for all charts") {
            with(project.helm.charts) {
                create("foo")
                create("bar")
            }

            assertThat(project)
                .containsTask<Task>("helmRender")
                .hasTaskDependencies(
                    "helmRenderFooChart",
                    "helmRenderBarChart",
                    exactly = true
                )
        }
    }


    describe("repositories") {

        it("should create a helm repositories DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
                .hasExtension<NamedDomainObjectContainer<HelmRepository>>("repositories")
        }


        it("should create a HelmAddRepository task for each repository") {
            with(project.helm.repositories) {
                create("myRepo") { repo ->
                    repo.url.set(project.uri("http://repository.example.com"))
                }
            }

            assertThat(project)
                .containsTask<HelmAddRepository>("helmAddMyRepoRepository")
                .prop(HelmAddRepository::url)
                .isPresent().isEqualTo(URI("http://repository.example.com"))
        }


        it("should create a helmAddRepositories task that registers all repos") {

            with(project.helm.repositories) {
                create("myRepo1") { repo ->
                    repo.url.set(project.uri("http://repository1.example.com"))
                }
                create("myRepo2") { repo ->
                    repo.url.set(project.uri("http://repository2.example.com"))
                }
            }

            assertThat(project)
                .containsTask<Task>("helmAddRepositories")
                .taskDependencies.all {
                    each { it.isInstanceOf(HelmAddRepository::class) }
                    extracting { it.name }.containsOnly("helmAddMyRepo1Repository", "helmAddMyRepo2Repository")
                }
        }
    }

    describe("registries") {
        it("should create a helm registries DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
                .hasExtension<NamedDomainObjectContainer<HelmRegistry>>("registries")
        }


        it("should expose the registries extension via the accessor") {
            project.helm.registries.create("myRegistry")

            assertThat(project.helm.registries)
                .containsItem("myRegistry")
        }


        it("should not add OCI repositories in helmAddRepositories") {
            with(project.helm.repositories) {
                create("classic") { repo ->
                    repo.url.set(project.uri("https://charts.example.com"))
                }
                create("oci") { repo ->
                    repo.url.set(URI("oci://registry.example.com/helm-local"))
                }
            }

            assertThat(project)
                .containsTask<Task>("helmAddRepositories")
                .taskDependencies
                .extracting { it.name }.containsOnly("helmAddClassicRepository")
        }


        it("should not update OCI repositories in helmUpdateRepositories") {
            with(project.helm.repositories) {
                create("classic") { repo ->
                    repo.url.set(project.uri("https://charts.example.com"))
                }
                // a repository without URL is not an OCI registry, so it's kept
                create("noUrl")
                create("oci") { repo ->
                    repo.url.set(URI("oci://registry.example.com/helm-local"))
                }
            }

            assertThat(project)
                .containsTask<HelmUpdateRepositories>("helmUpdateRepositories")
                .prop(HelmUpdateRepositories::repositoryNames)
                .isPresent().containsOnly("classic", "noUrl")
        }


        it("should pass registry logins to the dependency tasks") {
            with(project.helm.registries) {
                create("anonymous") { registry ->
                    registry.host.set("anonymous.example.com")
                    registry.caFile.set(project.file("ca.pem"))
                }
                create("withPassword") { registry ->
                    registry.host.set("password.example.com:5000")
                    registry.credentials { cred ->
                        cred.username.set("ci-user")
                        cred.password.set("s3cr3t")
                    }
                }
                create("withCertificate") { registry ->
                    registry.host.set("certificate.example.com")
                    registry.credentials(CertificateCredentials::class) {
                        certificateFile.set(project.file("cert.pem"))
                        keyFile.set(project.file("key.pem"))
                    }
                }
            }
            with(project.helm.repositories) {
                create("ociRepo") { repo ->
                    repo.url.set(URI("oci://oci-repo.example.com:8443/helm-local"))
                    repo.caFile.set(project.file("repo-ca.pem"))
                    repo.credentials { cred ->
                        cred.username.set("repo-user")
                        cred.password.set("repo-password")
                    }
                }
                // classic repositories don't need a registry login
                create("classic") { repo ->
                    repo.url.set(project.uri("https://charts.example.com"))
                }
            }

            val task = project.tasks.register("updateDependencies", HelmUpdateDependencies::class.java).get()

            assertThat(task.registryLogins.get()).containsOnly(
                RegistryLogin("anonymous.example.com", caFile = project.file("ca.pem")),
                RegistryLogin("password.example.com:5000", username = "ci-user", password = "s3cr3t"),
                RegistryLogin(
                    "certificate.example.com",
                    certificateFile = project.file("cert.pem"),
                    keyFile = project.file("key.pem")
                ),
                RegistryLogin(
                    "oci-repo.example.com:8443",
                    username = "repo-user",
                    password = "repo-password",
                    caFile = project.file("repo-ca.pem")
                )
            )
        }


        it("should fail to resolve registry logins if the registry host is not set") {
            project.helm.registries.create("noHost")

            val task = project.tasks.register("updateDependencies", HelmUpdateDependencies::class.java).get()

            assertFailure { task.registryLogins.get() }
                .rootCause()
                .hasMessage("The host of the Helm registry \"noHost\" is not set.")
        }
    }
})
