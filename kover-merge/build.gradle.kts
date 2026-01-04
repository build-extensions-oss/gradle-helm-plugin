// -----------------------------------------------------------------------
// This project doesn't have any code. It is intended to merge code coverage files on GitHub. How it works:
// 1. We compile project, run unit tests and then upload all results into "kover-unit-tests" artefact.
// 2. We run functionalTests and uploaded artefacts with pattern 'kover-functional-tests-*'
// 3. We download all artifacts into folders:
//       - ./build/tmp/kover/unit-tests
//       - ./build/tmp/kover/functional-tests
// 4. (we are here) We run task './gradlew :kover-merge:merge-coverage'
// 5. GitHub actions upload results to codecov website.
// -----------------------------------------------------------------------

kover {
    reports {
        this.filters {
            this.includes {
                projects.add(":helm-*")
            }
        }
        total {
            this.filters {
                this.includes {
                    projects.add(":helm-*")
                }
            }

            val files = rootProject.layout.buildDirectory.dir(
                "tmp/kover-artefacts"
            ).get().asFileTree.matching { include("**/*.ic") }

            additionalBinaryReports.addAll(files)
        }
    }
}