package io.github.build.extensions.oss.gradle.plugins.helm.command

import org.gradle.api.Project
import io.github.build.extensions.oss.gradle.plugins.helm.command.GlobalServerOptionsTests.serverCommands
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.helm
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.GradleExecMock
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.Invocation
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.eachInvocation


object GlobalServerOptionsTests : AbstractOptionsTests({

    val project: Project by memoized()
    val execMock: GradleExecMock by memoized()

    fun Invocation.isServerCommand(): Boolean =
        this.args.firstOrNull() in serverCommands


    variant("with custom kubeConfig property") {

        beforeEachTest {
            project.helm.kubeConfig.set(project.file("custom-kubeconfig"))
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::isServerCommand) {
                expectEnvironment("KUBECONFIG", "${project.projectDir}/custom-kubeconfig")
            }
        }
    }


    variant("with custom kubeContext property") {

        beforeEachTest {
            project.helm.kubeContext.set("custom-kubecontext")
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::isServerCommand) {
                expectOption("--kube-context", "custom-kubecontext")
            }
        }
    }


    variant("should use namespace property") {

        beforeEachTest {
            project.helm.namespace.set("custom-namespace")
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::isServerCommand) {
                expectOption("--namespace", "custom-namespace")
            }
        }
    }
}) {

    private val serverCommands = setOf(
        "get",
        "history", "hist",
        "install",
        "list", "ls",
        "rollback",
        "test",
        "uninstall", "un", "del", "delete",
        "upgrade"
    )
}
