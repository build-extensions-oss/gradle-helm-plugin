package io.github.build.extensions.oss.gradle.plugins.helm.command.tasks

import build.extensions.oss.gradle.pluginutils.test.execute
import io.github.build.extensions.oss.gradle.plugins.helm.command.HelmCommandsPlugin
import io.github.build.extensions.oss.gradle.plugins.helm.command.internal.RegistryLogin
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.helm
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.DefaultExecutableGradleExecMock
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.ExecutableGradleExecMock
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.GradleExecMock
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.singleInvocation
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.verifyNoInvocations
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.withStatefulVerification
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.File
import java.net.URI
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.io.TempDirDeletionStrategy

/**
 * Checks the `helm registry login` calls of the tasks which log in to OCI registries.
 */
@Suppress("DEPRECATION") // Task.execute - there is no other way to run a task action in a unit test
internal class RegistryLoginTasksTest {

    // don't fail on temp file removal - otherwise test report from GitHub Actions might hide the real issue
    @TempDir(deletionStrategy = TempDirDeletionStrategy.IgnoreFailures::class)
    private lateinit var tempFolder: File

    private lateinit var project: Project

    private val executableExecMock: ExecutableGradleExecMock = DefaultExecutableGradleExecMock()

    private val execMock: GradleExecMock by lazy { executableExecMock.withStatefulVerification() }

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().withProjectDir(tempFolder).build()
        project.plugins.apply(HelmCommandsPlugin::class.java)

        executableExecMock.start()
        val helmExecutable = executableExecMock.createScriptFile(project.projectDir.resolve("helm"))
        project.helm.executable.set(helmExecutable.absolutePath)
    }

    @AfterEach
    fun tearDown() {
        executableExecMock.close()
    }

    @Test
    fun `HelmRegistryLogin should pass all credentials to helm registry login`() {
        val task = project.tasks.create("login", HelmRegistryLogin::class.java) {
            it.registry.set("registry.example.com:5000")
            it.username.set("ci-user")
            it.password.set("s3cr3t")
            it.caFile.set(project.file("ca.pem"))
            it.certificateFile.set(project.file("cert.pem"))
            it.keyFile.set(project.file("key.pem"))
        }

        task.execute()

        execMock.singleInvocation {
            expectCommand("registry", "login")
            expectOption("--ca-file", project.file("ca.pem").absolutePath)
            expectOption("--cert-file", project.file("cert.pem").absolutePath)
            expectOption("--key-file", project.file("key.pem").absolutePath)
            expectOption("--username", "ci-user")
            // the password goes to the standard input, never to the command line
            expectFlag("--password-stdin")
            expectArg("registry.example.com:5000")
            stdin shouldBe "s3cr3t"
        }
    }

    @Test
    fun `HelmRegistryLogin should pass only the registry if nothing else is set`() {
        val task = project.tasks.create("login", HelmRegistryLogin::class.java) {
            it.registry.set("registry.example.com")
        }

        task.execute()

        execMock.singleInvocation {
            expectCommand("registry", "login")
            expectArg("registry.example.com")
            stdin shouldBe ""
        }
    }

    @Test
    fun `HelmAddRepository should log in instead of adding an OCI repository`() {
        val task = project.tasks.create("addRepo", HelmAddRepository::class.java) {
            it.repositoryName.set("oci-repo")
            it.url.set(URI("oci://registry.example.com:5000/helm-local"))
            it.username.set("ci-user")
            it.password.set("s3cr3t")
            it.caFile.set(project.file("ca.pem"))
            it.certificateFile.set(project.file("cert.pem"))
            it.keyFile.set(project.file("key.pem"))
        }

        // an OCI registry is never up-to-date, so the up-to-date check must not skip the login
        task.execute(checkUpToDate = true)

        execMock.singleInvocation {
            expectCommand("registry", "login")
            expectOption("--ca-file", project.file("ca.pem").absolutePath)
            expectOption("--cert-file", project.file("cert.pem").absolutePath)
            expectOption("--key-file", project.file("key.pem").absolutePath)
            expectOption("--username", "ci-user")
            expectFlag("--password-stdin")
            expectArg("registry.example.com:5000")
            stdin shouldBe "s3cr3t"
        }
    }

    @Test
    fun `HelmAddRepository should log in to an OCI repository without credentials`() {
        val task = project.tasks.create("addRepo", HelmAddRepository::class.java) {
            it.repositoryName.set("oci-repo")
            it.url.set(URI("oci://registry.example.com/helm-local"))
        }

        task.execute()

        execMock.singleInvocation {
            expectCommand("registry", "login")
            expectArg("registry.example.com")
        }
    }

    @Test
    fun `HelmUpdateDependencies should log in once to each registry a dependency is pulled from`() {
        project.file("chart").mkdirs()
        project.file("chart/Chart.yaml").writeText(
            """
            |apiVersion: v2
            |name: my-chart
            |version: 1.0.0
            |dependencies:
            |  - name: from-registry
            |    version: 1.2.3
            |    repository: oci://registry.example.com/helm-local
            |  - name: from-classic-repository
            |    version: 1.2.3
            |    repository: https://charts.example.com
            |  - name: local
            |    version: 1.2.3
            """.trimMargin()
        )

        val task = project.tasks.create("updateDependencies", HelmUpdateDependencies::class.java) {
            it.chartDir.set(project.file("chart"))
            it.registryLogins.set(
                listOf(
                    RegistryLogin("registry.example.com", username = "ci-user", password = "s3cr3t"),
                    // same host in another case: logging in twice is not needed
                    RegistryLogin("REGISTRY.example.com", username = "other-user"),
                    // no dependency is pulled from it
                    RegistryLogin("unused.example.com", username = "unused-user")
                )
            )
        }

        task.execute(checkUpToDate = false)

        execMock.invocations.map { it.args.take(2) } shouldContainExactly listOf(
            listOf("registry", "login"),
            listOf("dependency", "update")
        )
        execMock.forCommand("registry", "login").singleInvocation {
            expectCommand("registry", "login")
            expectOption("--username", "ci-user")
            expectFlag("--password-stdin")
            expectArg("registry.example.com")
            stdin shouldBe "s3cr3t"
        }
    }

    @Test
    fun `HelmUpdateDependencies should read v1 chart dependencies from requirements yaml`() {
        project.file("chart").mkdirs()
        project.file("chart/Chart.yaml").writeText("apiVersion: v1\nname: my-chart\nversion: 1.0.0")
        project.file("chart/requirements.yaml").writeText(
            """
            |dependencies:
            |  - name: from-registry
            |    version: 1.2.3
            |    repository: oci://registry.example.com/helm-local
            """.trimMargin()
        )

        val task = project.tasks.create("updateDependencies", HelmUpdateDependencies::class.java) {
            it.chartDir.set(project.file("chart"))
            it.registryLogins.set(listOf(RegistryLogin("registry.example.com")))
        }

        task.execute(checkUpToDate = false)

        execMock.invocations.map { it.args.take(2) } shouldContainExactly listOf(
            listOf("registry", "login"),
            listOf("dependency", "update")
        )
    }

    @Test
    fun `HelmBuildDependencies should not log in if a v1 chart has no requirements yaml`() {
        project.file("chart").mkdirs()
        project.file("chart/Chart.yaml").writeText("apiVersion: v1\nname: my-chart\nversion: 1.0.0")

        val task = project.tasks.create("buildDependencies", HelmBuildDependencies::class.java) {
            it.chartDir.set(project.file("chart"))
            it.registryLogins.set(listOf(RegistryLogin("registry.example.com")))
        }

        task.execute(checkUpToDate = false, checkOnlyIf = false)

        execMock.forCommand("registry", "login").verifyNoInvocations()
        execMock.forCommand("dependency", "build").singleInvocation {
            expectCommand("dependency", "build")
            expectArg(project.file("chart").absolutePath)
        }
    }
}
