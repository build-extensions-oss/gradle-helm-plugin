import io.github.build.extensions.oss.gradle.plugins.helm.command.tasks.HelmAddRepository

plugins {
    id("io.github.build-extensions-oss.helm") version "0.0.1"
}

afterEvaluate {
    val passwordRepository = tasks.getByName("helmAddPasswordRepoRepository") as HelmAddRepository
    check(passwordRepository.url.get() == uri("https://password.example.com/charts"))
    check(passwordRepository.username.get() == "test-user")
    check(passwordRepository.password.get() == "test-password")
    check(!passwordRepository.certificateFile.isPresent)
    check(!passwordRepository.keyFile.isPresent)

    val certificateRepository = tasks.getByName("helmAddCertificateRepoRepository") as HelmAddRepository
    check(certificateRepository.url.get() == uri("https://certificate.example.com/charts"))
    check(!certificateRepository.username.isPresent)
    check(!certificateRepository.password.isPresent)
    check(certificateRepository.certificateFile.get().asFile == file("client.pem"))
    check(certificateRepository.keyFile.get().asFile == file("client-key.pem"))

    println("Repository project properties verified")
}
