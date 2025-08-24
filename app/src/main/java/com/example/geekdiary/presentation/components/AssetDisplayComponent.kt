package com.example.geekdiary.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.example.geekdiary.R
import java.io.File

/**
 * Component for displaying assets (images/videos) with loading states and placeholders
 */
@Composable
fun AssetDisplayComponent(
    assetPath: String,
    contentDescription: String? = null,
    isVideo: Boolean = false,
    isLoading: Boolean = false,
    onLoadingStateChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                // Show loading indicator
                LoadingAssetPlaceholder(isVideo = isVideo)
            }
            assetPath.startsWith("android_asset://stub") || assetPath.contains("stub") -> {
                // Show stub placeholder
                StubAssetPlaceholder(isVideo = isVideo)
            }
            assetPath.startsWith("file://") -> {
                // Local file
                val file = File(assetPath.removePrefix("file://"))
                if (file.exists()) {
                    if (isVideo) {
                        VideoThumbnailComponent(
                            videoPath = assetPath,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(file)
                                .crossfade(true)
                                .build(),
                            contentDescription = contentDescription,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onState = { state ->
                                onLoadingStateChange?.invoke(state is AsyncImagePainter.State.Loading)
                            },
                            error = painterResource(R.drawable.ic_image_placeholder)
                        )
                    }
                } else {
                    // File doesn't exist, show error placeholder
                    ErrorAssetPlaceholder(isVideo = isVideo)
                }
            }
            else -> {
                // Remote URL or other format
                if (isVideo) {
                    VideoThumbnailComponent(
                        videoPath = assetPath,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(assetPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onState = { state ->
                            onLoadingStateChange?.invoke(state is AsyncImagePainter.State.Loading)
                        },
                        error = painterResource(R.drawable.ic_image_placeholder)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingAssetPlaceholder(
    isVideo: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isVideo) "Loading video..." else "Loading image...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StubAssetPlaceholder(
    isVideo: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(16.dp)
    ) {
        Icon(
            painter = painterResource(
                if (isVideo) R.drawable.ic_video_placeholder 
                else R.drawable.ic_image_placeholder
            ),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isVideo) "Video downloading..." else "Image downloading...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorAssetPlaceholder(
    isVideo: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(16.dp)
    ) {
        Icon(
            painter = painterResource(
                if (isVideo) R.drawable.ic_video_placeholder 
                else R.drawable.ic_image_placeholder
            ),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isVideo) "Video unavailable" else "Image unavailable",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VideoThumbnailComponent(
    videoPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    // For now, show video placeholder with play button
    // In a full implementation, you would extract video thumbnail
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_video_placeholder),
            contentDescription = contentDescription,
            modifier = Modifier.size(64.dp),
            tint = Color.White
        )
        
        // Play button overlay
        Surface(
            modifier = Modifier
                .size(48.dp)
                .offset(y = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.7f)
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_media_play),
                contentDescription = "Play video",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                tint = Color.White
            )
        }
    }
}
