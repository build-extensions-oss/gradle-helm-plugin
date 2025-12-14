package io.github.build.extensions.oss.gradle.plugins.helm

import assertk.all
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.each
import assertk.assertions.extracting
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import assertk.assertions.prop
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmAddRepository
import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmTemplate
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.Filtering
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmChart
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmExtension
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmRepository
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.charts
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.internal.helm
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
})
