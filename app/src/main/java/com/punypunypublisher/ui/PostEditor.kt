package com.punypunypublisher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.punypunypublisher.data.model.Post
import com.punypunypublisher.posts.Edit
import com.punypunypublisher.posts.PostRules

/**
 * Full-screen text editor for a new ([initial] == null) or upcoming post. Ids and dates are
 * handled by [PostRules]; the user only sees the text. Saving goes straight to GitHub; on failure
 * the dialog stays open with the text intact and shows the error.
 */
@Composable
fun PostEditor(
    initial: Post?,
    maxChars: Int,
    busy: Boolean,
    onSubmit: (Edit, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    var problem by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val count = PostRules.charCount(text)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(if (initial == null) "New post" else "Edit post", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; problem = null },
                    label = { Text("Post") },
                    supportingText = {
                        Text(
                            "$count / $maxChars",
                            color = if (count > maxChars) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    isError = count > maxChars,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (initial != null) {
                        TextButton(onClick = { confirmDelete = true }, enabled = !busy) { Text("Delete") }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        enabled = !busy,
                        onClick = {
                            val edit = if (initial == null) Edit.Add(text, PostRules.today())
                            else Edit.Update(initial.id, text)
                            onSubmit(edit) { problem = it }
                        },
                    ) { Text(if (busy) "Saving…" else "Save") }
                }
            }
        }
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this post?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onSubmit(Edit.Delete(initial.id)) { problem = it }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
