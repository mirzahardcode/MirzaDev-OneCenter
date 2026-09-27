package com.mirzadev.onecenter

/**
 * Single place where app-level configuration lives.
 *
 * The GitHub token is read from BuildConfig so it is no longer a literal in
 * source control. See NOTES.md for the Gradle snippet and for why this is
 * still only a stopgap.
 */
object AppConfig {

    val githubToken: String
        get() = BuildConfig.GITHUB_TOKEN

    const val PRIVATE_REPO = "mirzahardcode/MirzaDevPrivateData"
    const val RELEASES_REPO = "mirzahardcode/cracking-exam-app"

    const val CACHE_TTL_MILLIS = 30 * 60 * 1000L
}
