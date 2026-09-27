package com.mirzadev.onecenter.data

import androidx.compose.runtime.Immutable

/**
 * @Immutable lets the Compose compiler skip recomposition of any composable
 * whose only changed input is an equal instance of these. Cheap win for the
 * APK and document lists.
 */

@Immutable
data class LatestInformation(
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = 0L
)

@Immutable
data class GitHubApk(
    val name: String = "",
    val version: String = "",
    val size: String = "",
    val downloadUrl: String = ""
)

@Immutable
data class PrivateDocument(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = 0L
)

@Immutable
data class PrivateDocumentsResponse(
    val documents: List<PrivateDocument> = emptyList()
)
