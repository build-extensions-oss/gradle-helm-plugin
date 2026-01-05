import org.gradle.api.Project


internal val Project.cleanRunEnabled: Boolean
    get() = findProperty(BuildConstants.FUNCTIONAL_TESTS_ONLY)?.toString()
        ?.toBoolean()
        ?: false