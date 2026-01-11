plugins {
    id("io.github.build-extensions-oss.helm")
}

helm {
    charts {
        create("main") {
            sourceDir.set(file("src/main/helm"))
        }
    }
}