package io.github.build.extensions.oss.gradle.plugins.helm.rules

import build.extensions.oss.gradle.pluginutils.rules.AbstractPatternRuleOuterInner
import build.extensions.oss.gradle.pluginutils.rules.AbstractTaskRule
import build.extensions.oss.gradle.pluginutils.rules.RuleNamePattern
import build.extensions.oss.gradle.pluginutils.rules.RuleNamePattern2
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmChart


abstract class AbstractHelmChartTaskRule<T : Task>(
    taskType: Class<T>,
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>,
    namePattern: RuleNamePattern
) : AbstractTaskRule<HelmChart, T>(
    taskType, tasks, charts, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    abstract override fun T.configureFrom(chart: HelmChart)
}


abstract class AbstractHelmChartTaskRuleOuterInner<S : Named, T : Task>(
    taskType: Class<T>,
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>,
    innerSourceContainerFunction: (HelmChart) -> NamedDomainObjectCollection<S>,
    namePattern: RuleNamePattern2
) : AbstractPatternRuleOuterInner<HelmChart, S, T>(
    taskType, tasks, charts, innerSourceContainerFunction, namePattern
) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    abstract override fun T.configureFrom(chart: HelmChart, innerSource: S)
}
