package io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.internal

import io.github.build.extensions.oss.gradle.plugins.helm.dsl.HelmExtension
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.HELM_PUBLISHING_EXTENSION_NAME
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.HelmPublishingExtension
import io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl.HelmPublishingRepositoryContainer
import build.extensions.oss.gradle.pluginutils.requiredExtension


/**
 * Gets the `publishing` sub-extension.
 */
val HelmExtension.publishing: HelmPublishingExtension
    get() = requiredExtension(HELM_PUBLISHING_EXTENSION_NAME)


/**
 * Gets the `publishing.repositories` sub-extension.
 */
val HelmPublishingExtension.repositories: HelmPublishingRepositoryContainer
    get() = requiredExtension(HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME)
