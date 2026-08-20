package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
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

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            refreshingInternal = false
            offsetY = 0f
        }
    }

    val pullProgress = (offsetY / 140f).coerceIn(0f, 1f)
    val isBusy = isRefreshing || refreshingInternal

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isBusy) 64f else offsetY,
        animationSpec = tween(durationMillis = 200),
        label = "pullToRefreshOffset"
    )

    fun triggerRefreshIfThreshold() {
        if (offsetY >= 100f && !isBusy) {
            refreshingInternal = true
            offsetY = 100f
            onRefresh()
            coroutineScope.launch {
                delay(1200)
                refreshingInternal = false
                offsetY = 0f
            }
        } else if (!isBusy) {
            offsetY = 0f
        }
    }

    val nestedScrollConnection = remember(isBusy) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (available.y < 0 && offsetY > 0) {
                    val newOffset = (offsetY + available.y).coerceAtLeast(0f)
                    val consumed = offsetY - newOffset
                    offsetY = newOffset
                    Offset(0f, consumed)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.y > 0 && !isBusy) {
                    offsetY = (offsetY + available.y * 0.4f).coerceAtMost(180f)
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                triggerRefreshIfThreshold()
                return super.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                triggerRefreshIfThreshold()
                return super.onPostFling(consumed, available)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        content()

        if (animatedOffsetY > 0f || isBusy) {
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
                        if (isBusy) {
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
