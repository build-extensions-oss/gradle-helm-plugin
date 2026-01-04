plugins {
    id("io.github.build-extensions-oss.helm") version "0.0.1"
    id("io.github.build-extensions-oss.helm-publish") version "0.0.1"
}

helm {
    charts {
        create("main") {
            sourceDir.set(file("src/main/helm"))
            // check project settings - they will be provided via command line
            publish = (project.extra["publishHelmPlugin"] as String).toBoolean()
        }
    }
    publishing {
        repositories {
            artifactory {
                url = uri("http://localhost:999999/artifactory/folder1/folder2")

                credentials {
                    username = "testUserName"
                    password = "testPassword"
                }
            }
        }
    }
}