plugins {
    id("io.github.build-extensions-oss.helm-releases")
}

helm {
    releases {
        create("test") {
            from(chart(project = ":project-with-chart", chart = "main"))
        }
    }
}