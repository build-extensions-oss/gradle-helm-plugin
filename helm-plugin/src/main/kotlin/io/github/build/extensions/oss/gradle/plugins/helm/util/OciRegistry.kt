package io.github.build.extensions.oss.gradle.plugins.helm.util

import java.net.URI


private const val OCI_SCHEME = "oci"


/**
 * Whether this URI points to an OCI registry (`oci://...`).
 */
internal val URI.isOciRegistry: Boolean
    get() = scheme.equals(OCI_SCHEME, ignoreCase = true)


/**
 * The registry host (and port, if any) that `helm registry login` expects.
 *
 * Returns `registry.example.com:5000` for `oci://registry.example.com:5000/helm-local`.
 */
internal val URI.ociRegistryHost: String
    get() = if (port != -1) "$host:$port" else host
