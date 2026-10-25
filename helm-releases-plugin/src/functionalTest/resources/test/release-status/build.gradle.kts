plugins {
    id("io.github.build-extensions-oss.helm-releases") version "0.0.1"
}

// A status check needs no chart at all - only a release name and the server options of the target it
// runs against - so this fixture is a single project with a plain chart reference.
helm {
    releaseTargets {
        // "default" is the active target unless -Phelm.release.target says otherwise.
        create("default") {
            namespace.set("default-ns")
        }
        // Every server option set here, so that each one can be seen arriving at the helm CLI.
        create("staging") {
            namespace.set("staging-ns")
            kubeContext.set("staging-ctx")
            kubeConfig.set(file("staging.kubeconfig"))
        }
    }

    releases {
        create("awesome") {
            from("my-repo/awesome-chart")
            releaseName.set("awesome-release")

            // Exercises the tag expression the status task evaluates in onlyIf.
            tags("smoke")

            // The status task must use the target-specific release, not the plain one.
            forTarget("staging") {
                releaseName.set("awesome-on-staging")
            }
        }
    }
}
