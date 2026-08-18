package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.PrimaryGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var refreshingInternal by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val pullProgress = (offsetY / 180f).coerceIn(0f, 1f)
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isRefreshing || refreshingInternal) 100f else offsetY,
        animationSpec = tween(durationMillis = 200),
        label = "pullToRefreshOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    if (!isRefreshing && !refreshingInternal) {
                        val newOffset = offsetY + (delta * 0.4f)
                        offsetY = newOffset.coerceIn(0f, 220f)
                    }
                },
                onDragStopped = {
                    if (offsetY >= 120f && !isRefreshing && !refreshingInternal) {
                        refreshingInternal = true
                        onRefresh()
                        coroutineScope.launch {
                            delay(1200)
                            refreshingInternal = false
                            offsetY = 0f
                        }
                    } else {
                        offsetY = 0f
                    }
                }
            )
    ) {
        content()

        if (animatedOffsetY > 0f || isRefreshing || refreshingInternal) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedOffsetY.dp)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = DarkCard,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    shadowElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshing || refreshingInternal) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = PrimaryGold,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Pull to refresh",
                                tint = PrimaryGold,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(pullProgress * 360f)
                            )
                        }
                    }
                }
            }
        }
    }
}
