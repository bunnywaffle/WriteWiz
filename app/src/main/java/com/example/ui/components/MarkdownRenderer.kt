package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Parses markdown inline styles (bold, italic, code) into an AnnotatedString.
 */
fun parseMarkdownInline(text: String, baseSpanStyle: SpanStyle = SpanStyle()): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val boldIndex = text.indexOf("**", index)
            val italicIndex = text.indexOf("*", index)
            val codeIndex = text.indexOf("`", index)

            // Find closest token
            val nextToken = listOf(
                if (boldIndex != -1) boldIndex to "**" else -1 to "",
                if (italicIndex != -1) italicIndex to "*" else -1 to "",
                if (codeIndex != -1) codeIndex to "`" else -1 to ""
            ).filter { it.first != -1 }.minByOrNull { it.first }

            if (nextToken == null) {
                // No more tokens, append rest of text
                append(text.substring(index))
                break
            }

            // Append plain text up to token
            if (nextToken.first > index) {
                append(text.substring(index, nextToken.first))
            }

            val tokenStart = nextToken.first
            val token = nextToken.second
            val contentStart = tokenStart + token.length

            // Find matching ending token
            val tokenEnd = text.indexOf(token, contentStart)
            if (tokenEnd == -1) {
                // No match, treat token literally and move past it
                append(token)
                index = contentStart
            } else {
                val inlineText = text.substring(contentStart, tokenEnd)
                pushStyle(
                    when (token) {
                        "**" -> SpanStyle(fontWeight = FontWeight.Bold)
                        "*" -> SpanStyle(fontStyle = FontStyle.Italic)
                        "`" -> SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.LightGray.copy(alpha = 0.3f),
                            color = Color(0xFFD81B60),
                            fontSize = 14.sp
                        )
                        else -> SpanStyle()
                    }
                )
                append(parseMarkdownInline(inlineText)) // recurse in case of nested style (e.g. bold italic)
                pop()
                index = tokenEnd + token.length
            }
        }
    }
}

@Composable
fun MarkdownListBlock(items: List<String>) {
    items.forEach { item ->
        Row(
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                text = parseMarkdownInline(item),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Renders fully structured markdown beautifully inside a Composable layout.
 */
@Composable
fun MarkdownViewer(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.split("\n")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (markdown.trim().isEmpty()) {
            Text(
                text = "Empty document. Start writing your story scene below!",
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
                fontStyle = FontStyle.Italic
            )
        }

        var inList = false
        val listItems = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                inList = true
                listItems.add(trimmed.substring(2))
            } else {
                if (inList) {
                    MarkdownListBlock(items = listItems.toList())
                    listItems.clear()
                    inList = false
                }

                when {
                    trimmed.startsWith("# ") -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parseMarkdownInline(trimmed.substring(2)),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                    }
                    trimmed.startsWith("## ") -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = parseMarkdownInline(trimmed.substring(3)),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    trimmed.startsWith("### ") -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = parseMarkdownInline(trimmed.substring(4)),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    trimmed.startsWith("> ") -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.secondary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = parseMarkdownInline(trimmed.substring(2)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    trimmed.isEmpty() -> {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    else -> {
                        Text(
                            text = parseMarkdownInline(line),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
        // Flush any remaining list items
        if (inList) {
            MarkdownListBlock(items = listItems.toList())
            listItems.clear()
        }
    }
}
