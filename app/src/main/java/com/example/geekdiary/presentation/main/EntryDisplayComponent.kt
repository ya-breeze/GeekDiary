package com.example.geekdiary.presentation.main

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geekdiary.domain.model.DiaryEntry
import com.example.geekdiary.ui.theme.GeekDiaryTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDisplayComponent(
    entry: DiaryEntry,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Entry title
            Text(
                text = entry.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Tags if available
            if (entry.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    entry.tags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = { /* TODO: Implement tag filtering */ },
                            label = { Text(tag) }
                        )
                    }
                    if (entry.tags.size > 3) {
                        AssistChip(
                            onClick = { /* TODO: Show all tags */ },
                            label = { Text("+${entry.tags.size - 3} more") }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Entry body
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entry.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 5,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    modifier = if (isExpanded) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier
                    }
                )
                
                // Expand/Collapse button if content is long
                if (entry.body.length > 200 || entry.body.lines().size > 5) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Show less" else "Show more"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isExpanded) "Show less" else "Show more")
                    }
                }
            }
            
            // Entry metadata
            if (entry.isLocal || entry.needsSync) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (entry.isLocal) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Local") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                    if (entry.needsSync) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Needs Sync") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntryDisplayComponentPreview() {
    GeekDiaryTheme {
        EntryDisplayComponent(
            entry = DiaryEntry(
                date = LocalDate.now(),
                title = "My Amazing Day",
                body = "Today was a wonderful day filled with exciting adventures and meaningful moments. I woke up early to catch the sunrise, which painted the sky in beautiful shades of orange and pink. The morning was crisp and fresh, perfect for a long walk in the park.\n\nI spent the afternoon working on my personal projects, making significant progress on the mobile app I've been developing. The feeling of solving complex problems and seeing the code come together is incredibly satisfying.\n\nIn the evening, I met with friends for dinner at that new restaurant downtown. The food was exceptional, and the company was even better. We laughed, shared stories, and made plans for our upcoming weekend trip.\n\nAs I reflect on the day, I'm filled with gratitude for all the small moments that made it special.",
                tags = listOf("personal", "work", "friends", "gratitude", "development"),
                isLocal = true,
                needsSync = true
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
