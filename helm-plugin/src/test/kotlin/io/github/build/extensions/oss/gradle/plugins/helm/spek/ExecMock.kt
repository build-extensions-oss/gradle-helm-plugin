package io.github.build.extensions.oss.gradle.plugins.helm.spek

import org.gradle.api.Project
import org.spekframework.spek2.dsl.LifecycleAware
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.lifecycle.MemoizedValue
import io.github.build.extensions.oss.gradle.plugins.helm.command.HelmCommandsPlugin
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.helm
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.DefaultExecutableGradleExecMock
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.ExecutableGradleExecMock
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.GradleExecMock
import io.github.build.extensions.oss.gradle.plugins.helm.testutil.exec.withStatefulVerification


/**
 * Uses a [GradleExecMock] for tests that invoke external processes.
 *
 * @param executableFileName the base name of the fake executable file. A wrapper script with this name is
 *        created inside the project directory (with a `.bat` extension added on Windows), and the `helm`
 *        extension's `executable` property is pointed at it.
 * @return a [GradleExecMock] as a Spek [MemoizedValue]
 */
fun LifecycleAware.gradleExecMock(executableFileName: String = "helm"): MemoizedValue<GradleExecMock> {

    val executableExecMock: ExecutableGradleExecMock by memoized(
        mode = CachingMode.SCOPE,
        factory = {
            val execMock: ExecutableGradleExecMock = DefaultExecutableGradleExecMock()
            execMock.start()
            execMock
        },
        destructor = { it.close() }
    )

    beforeEachTest {
        val project: Project by memoized()
        val scriptFile = executableExecMock.createScriptFile(project.projectDir.resolve(executableFileName))

        project.plugins.withType(HelmCommandsPlugin::class.java) {
            project.helm.executable.set(scriptFile.absolutePath)
        }
    }

    afterEachTest {
        executableExecMock.reset()
    }

    return memoized(mode = CachingMode.TEST) {
        executableExecMock.withStatefulVerification()
    }
}
