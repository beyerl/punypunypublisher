package com.punypunypublisher.export

import android.content.Context
import android.net.Uri

/** Writes file content to a user-picked location (via the Storage Access Framework). */
object Exporter {
    fun writeTo(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        }
    }
}
