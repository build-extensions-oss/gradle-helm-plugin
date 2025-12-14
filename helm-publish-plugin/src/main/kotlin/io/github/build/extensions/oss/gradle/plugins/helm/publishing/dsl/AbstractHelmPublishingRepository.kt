package io.github.build.extensions.oss.gradle.plugins.helm.publishing.dsl

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.internal.CredentialsContainerSupport
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.internal.CredentialsFactory
import io.github.build.extensions.oss.gradle.plugins.helm.dsl.credentials.internal.DefaultCredentialsFactory
import build.extensions.oss.gradle.pluginutils.property
import java.net.URI


internal abstract class AbstractHelmPublishingRepository
private constructor(
    objects: ObjectFactory,
    private val name: String,
    credentialsContainer: CredentialsContainer
) : HelmPublishingRepositoryInternal, CredentialsContainer by credentialsContainer {

    private constructor(objects: ObjectFactory, name: String, credentialsFactory: CredentialsFactory)
            : this(objects, name, CredentialsContainerSupport(objects, credentialsFactory))


    protected constructor(objects: ObjectFactory, name: String)
            : this(objects, name, DefaultCredentialsFactory(objects))


    protected constructor(project: Project, name: String)
            : this(project.objects, name)


    final override fun getName(): String =
        name


    final override val url: Property<URI> =
        objects.property()
}
