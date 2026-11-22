package com.mirzadev.onecenter.data

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

/**
 * @Immutable lets the Compose compiler skip recomposition of any composable
 * whose only changed input is an equal instance of these. Cheap win for the
 * APK and document lists.
 */

@Immutable
data class LatestInformation(
    @field:SerializedName("title") val title: String = "",
    @field:SerializedName("content") val content: String = "",
    @field:SerializedName("updatedAt") val updatedAt: Long = 0L
)

@Immutable
data class GitHubApk(
    @field:SerializedName("name") val name: String = "",
    @field:SerializedName("version") val version: String = "",
    @field:SerializedName("size") val size: String = "",
    @field:SerializedName("downloadUrl") val downloadUrl: String = ""
)

@Immutable
data class PrivateDocument(
    @field:SerializedName("id") val id: String = "",
    @field:SerializedName("title") val title: String = "",
    @field:SerializedName("content") val content: String = "",
    @field:SerializedName("updatedAt") val updatedAt: Long = 0L,
    @field:SerializedName("grade") val grade: String? = null,
    @field:SerializedName("subject") val subject: String? = null
)

@Immutable
data class PrivateDocumentsResponse(
    @field:SerializedName("ok") val ok: Boolean = true,
    @field:SerializedName("allowedDocumentIds") val allowedDocumentIds: List<String> = emptyList(),
    @field:SerializedName("documents") val documents: List<PrivateDocument> = emptyList()
)
