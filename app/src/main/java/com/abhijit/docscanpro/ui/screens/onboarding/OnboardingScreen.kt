@file:OptIn(ExperimentalFoundationApi::class)

package com.abhijit.docscanpro.ui.screens.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhijit.docscanpro.data.preferences.AppPreferences
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>
)

private val pages = listOf(
    OnboardingPage(
        Icons.Default.CameraAlt,
        "Scan Anything",
        "Capture documents, receipts, ID cards and more with automatic edge detection and perspective correction.",
        listOf(Color(0xFF0D1117), Color(0xFF1A237E))
    ),
    OnboardingPage(
        Icons.Default.TextFields,
        "Instant Text Recognition",
        "Smart OCR reads text in English, Hindi and 100+ languages. Copy, search, or export in one tap.",
        listOf(Color(0xFF0D1117), Color(0xFF1B5E20))
    ),
    OnboardingPage(
        Icons.Default.Lock,
        "Secure & Private",
        "All scans stay on your device. Protect them with fingerprint, face lock, or a 4-digit PIN.",
        listOf(Color(0xFF0D1117), Color(0xFF4A148C))
    ),
    OnboardingPage(
        Icons.Default.Search,
        "Find Anything, Fast",
        "Full-text search across all your scans, smart filters, and automatic document classification.",
        listOf(Color(0xFF0D1117), Color(0xFF880E4F))
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            val page = pages[index]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(page.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .padding(bottom = 180.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            page.icon,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(48.dp))
                    Text(
                        page.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        page.subtitle,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.80f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Controls anchored to bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated page indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            .height(8.dp)
                            .width(if (isSelected) 28.dp else 8.dp)
                            .background(
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                    )
                }
            }

            val isLastPage = pagerState.currentPage == pages.size - 1

            Button(
                onClick = {
                    if (isLastPage) {
                        scope.launch {
                            AppPreferences(context).markFirstLaunchDone()
                            onFinished()
                        }
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0D1117)
                )
            ) {
                Text(
                    if (isLastPage) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isLastPage) {
                TextButton(
                    onClick = {
                        scope.launch {
                            AppPreferences(context).markFirstLaunchDone()
                            onFinished()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                ) {
                    Text("Skip", fontSize = 14.sp)
                }
            }
        }
    }
}
