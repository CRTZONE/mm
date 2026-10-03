package com.example.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.FloatingLogoBubble
import com.example.ui.components.GuideDialog
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val TARGET_URL = "https://www.emailondeck.com/eod.php"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDeckScreen(
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current

    val floatingState = remember { FloatingWindowState() }

    var currentUrl by remember { mutableStateOf(TARGET_URL) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorDescription by remember { mutableStateOf("") }
    var webViewGeneration by remember { mutableIntStateOf(0) }
    var useSoftwareRendering by remember { mutableStateOf(true) }

    var showGuideDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showTipBanner by remember { mutableStateOf(true) }

    // Reusable WebView instance retained across mode transitions (bubble, floating, fullscreen, pip)
    // so captcha verification state & temporary email session are not lost!
    val cachedWebView = remember(context, webViewGeneration) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Fix MESA rendernode error by using software layer type
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            // Fix Invalid first_paint timing error by setting an explicit initial background color
            setBackgroundColor(android.graphics.Color.WHITE)

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                allowFileAccess = true
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                val defaultUa = userAgentString
                userAgentString = defaultUa.replace("; wv", "")
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progress = newProgress / 100f
                    isLoading = newProgress < 100
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    isLoading = true
                    hasError = false
                    url?.let { currentUrl = it }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false
                    url?.let { currentUrl = it }
                    canGoBack = canGoBack()
                    canGoForward = canGoForward()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        hasError = true
                        isLoading = false
                        errorDescription = error?.description?.toString() ?: "Connection failed"
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    try {
                        (view?.parent as? ViewGroup)?.removeView(view)
                        view?.destroy()
                    } catch (_: Exception) {}
                    useSoftwareRendering = true
                    hasError = true
                    isLoading = false
                    errorDescription = "Display renderer recovered. Tap Retry to reload in safe mode."
                    webViewGeneration++
                    return true
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith("mailto:")) {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        return true
                    }
                    if (url.contains("emailondeck.com") || url.contains("cloudflare") || url.contains("hcaptcha") || url.contains("recaptcha")) {
                        return false
                    }
                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            loadUrl(TARGET_URL)
        }
    }

    DisposableEffect(cachedWebView) {
        onDispose {
            try {
                (cachedWebView.parent as? ViewGroup)?.removeView(cachedWebView)
                cachedWebView.destroy()
            } catch (_: Exception) {}
        }
    }

    // Hardware back handler
    BackHandler(enabled = canGoBack || floatingState.windowMode == WindowMode.FULLSCREEN) {
        if (canGoBack) {
            cachedWebView.goBack()
        } else if (floatingState.windowMode == WindowMode.FULLSCREEN) {
            floatingState.windowMode = WindowMode.FLOATING_WINDOW
        }
    }

    // Direct PiP Mode rendering
    if (isInPipMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = {
                    (cachedWebView.parent as? ViewGroup)?.removeView(cachedWebView)
                    cachedWebView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (floatingState.windowMode == WindowMode.FULLSCREEN) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Email On Deck",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "emailondeck.com",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { floatingState.windowMode = WindowMode.FLOATING_WINDOW },
                            modifier = Modifier.testTag("exit_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Floating Window Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { floatingState.minimizeToBubble() },
                            modifier = Modifier.testTag("fullscreen_minimize_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Minimize to Floating Logo"
                            )
                        }
                        IconButton(
                            onClick = onEnterPip,
                            modifier = Modifier.testTag("fullscreen_pip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture in Picture"
                            )
                        }
                        IconButton(
                            onClick = {
                                hasError = false
                                cachedWebView.reload()
                            },
                            modifier = Modifier.testTag("fullscreen_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val parentWidthPx = constraints.maxWidth.toFloat()
            val parentHeightPx = constraints.maxHeight.toFloat()
            val parentWidthDp = maxWidth
            val parentHeightDp = maxHeight

            // Mode 1: FULLSCREEN
            if (floatingState.windowMode == WindowMode.FULLSCREEN) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showTipBanner) {
                        QuickTipBanner(
                            onDismiss = { showTipBanner = false }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        AndroidView(
                            factory = {
                                (cachedWebView.parent as? ViewGroup)?.removeView(cachedWebView)
                                cachedWebView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom Bar in Fullscreen
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { cachedWebView.goBack() },
                                    enabled = canGoBack
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                                IconButton(
                                    onClick = { cachedWebView.goForward() },
                                    enabled = canGoForward
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Forward"
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.testTag("fullscreen_new_email_button")
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Email")
                            }

                            IconButton(
                                onClick = { floatingState.windowMode = WindowMode.FLOATING_WINDOW }
                            ) {
                                Icon(
                                    Icons.Default.FullscreenExit,
                                    contentDescription = "Floating Window"
                                )
                            }
                        }
                    }
                }
            } else {
                // Workspace Backdrop for FLOATING_WINDOW and MINIMIZED_BUBBLE modes
                WorkspaceBackdrop(
                    windowMode = floatingState.windowMode,
                    onOpenFloating = { floatingState.openFloatingWindow() },
                    onOpenFullscreen = { floatingState.windowMode = WindowMode.FULLSCREEN },
                    onEnterPip = onEnterPip,
                    onOpenGuide = { showGuideDialog = true },
                    onResetSession = { showResetDialog = true }
                )

                // FLOATING WINDOW CONTAINER
                if (floatingState.windowMode == WindowMode.FLOATING_WINDOW) {
                    val maxWindowWidthDp = (parentWidthDp - 16.dp).coerceAtLeast(280.dp)
                    val maxWindowHeightDp = (parentHeightDp - 20.dp).coerceAtLeast(360.dp)

                    val currentWidthDp = floatingState.windowWidth.coerceIn(280.dp, maxWindowWidthDp)
                    val currentHeightDp = floatingState.windowHeight.coerceIn(360.dp, maxWindowHeightDp)

                    val currentWidthPx = with(density) { currentWidthDp.toPx() }
                    val currentHeightPx = with(density) { currentHeightDp.toPx() }

                    // Clamp offsets to keep window on screen
                    val clampedOffsetX = floatingState.windowOffsetX.coerceIn(
                        0f,
                        max(0f, parentWidthPx - currentWidthPx)
                    )
                    val clampedOffsetY = floatingState.windowOffsetY.coerceIn(
                        0f,
                        max(0f, parentHeightPx - currentHeightPx)
                    )

                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(clampedOffsetX.roundToInt(), clampedOffsetY.roundToInt()) }
                            .size(currentWidthDp, currentHeightDp)
                            .shadow(16.dp, RoundedCornerShape(18.dp))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .testTag("floating_window_surface"),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Draggable Top Bar
                            FloatingHeaderBar(
                                onDrag = { dx, dy ->
                                    floatingState.windowOffsetX = (floatingState.windowOffsetX + dx).coerceIn(
                                        0f,
                                        max(0f, parentWidthPx - currentWidthPx)
                                    )
                                    floatingState.windowOffsetY = (floatingState.windowOffsetY + dy).coerceIn(
                                        0f,
                                        max(0f, parentHeightPx - currentHeightPx)
                                    )
                                },
                                onMinimize = { floatingState.minimizeToBubble() },
                                onFullscreen = { floatingState.windowMode = WindowMode.FULLSCREEN },
                                onPip = onEnterPip,
                                onSizePreset = { preset -> floatingState.applyPreset(preset) }
                            )

                            // Loading progress bar
                            if (isLoading) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Interactive WebView or Error Screen
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                if (hasError) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.WifiOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Connection Issue",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            errorDescription.ifEmpty { "Check internet connection" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                hasError = false
                                                webViewGeneration++
                                            }
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                } else {
                                    AndroidView(
                                        factory = {
                                            (cachedWebView.parent as? ViewGroup)?.removeView(cachedWebView)
                                            cachedWebView
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Bottom Controls + Corner Resize Handle
                            FloatingBottomBar(
                                canGoBack = canGoBack,
                                canGoForward = canGoForward,
                                onBack = { cachedWebView.goBack() },
                                onForward = { cachedWebView.goForward() },
                                onRefresh = {
                                    hasError = false
                                    cachedWebView.reload()
                                },
                                onReset = { showResetDialog = true },
                                onResizeDrag = { dx, dy ->
                                    val newWidthDp = floatingState.windowWidth + with(density) { dx.toDp() }
                                    val newHeightDp = floatingState.windowHeight + with(density) { dy.toDp() }
                                    floatingState.windowWidth = newWidthDp.coerceIn(280.dp, maxWindowWidthDp)
                                    floatingState.windowHeight = newHeightDp.coerceIn(360.dp, maxWindowHeightDp)
                                }
                            )
                        }
                    }
                }

                // MINIMIZED FLOATING LOGO BUBBLE
                if (floatingState.windowMode == WindowMode.MINIMIZED_BUBBLE) {
                    val bubbleClampedX = floatingState.bubbleOffsetX.coerceIn(0f, max(0f, parentWidthPx - 180f))
                    val bubbleClampedY = floatingState.bubbleOffsetY.coerceIn(0f, max(0f, parentHeightPx - 180f))

                    FloatingLogoBubble(
                        offsetX = bubbleClampedX,
                        offsetY = bubbleClampedY,
                        onDrag = { dx, dy ->
                            floatingState.bubbleOffsetX = (floatingState.bubbleOffsetX + dx).coerceIn(
                                0f,
                                max(0f, parentWidthPx - 180f)
                            )
                            floatingState.bubbleOffsetY = (floatingState.bubbleOffsetY + dy).coerceIn(
                                0f,
                                max(0f, parentHeightPx - 180f)
                            )
                        },
                        onClick = { floatingState.openFloatingWindow() }
                    )
                }
            }
        }
    }

    // Guide Dialog
    if (showGuideDialog) {
        GuideDialog(onDismiss = { showGuideDialog = false })
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Get New Email Session?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This resets cookies and loads a fresh session on EmailOnDeck so you can solve captcha again and generate a new temporary email."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        CookieManager.getInstance().removeAllCookies {
                            CookieManager.getInstance().flush()
                        }
                        cachedWebView.clearCache(true)
                        cachedWebView.clearHistory()
                        cachedWebView.loadUrl(TARGET_URL)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Session reset. Loading new email setup...")
                        }
                    },
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text("Reset & Create New")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun QuickTipBanner(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("tip_banner_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "1. Click [  ] I'm Human  ➔  2. Solve Captcha  ➔  3. Get Email",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingHeaderBar(
    onDrag: (dx: Float, dy: Float) -> Unit,
    onMinimize: () -> Unit,
    onFullscreen: () -> Unit,
    onPip: () -> Unit,
    onSizePreset: (WindowSizePreset) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .testTag("floating_header_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Drag handle & title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to move window",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Email On Deck",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Size preset quick chips: S, M, L
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSizePreset(WindowSizePreset.COMPACT) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "S",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSizePreset(WindowSizePreset.BALANCED) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "M",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSizePreset(WindowSizePreset.LARGE) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "L",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onPip,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Picture in picture",
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onFullscreen,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("floating_minimize_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Minimize to bubble",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
    onResizeDrag: (dx: Float, dy: Float) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier
                    .height(30.dp)
                    .testTag("floating_new_email_btn")
            ) {
                Text("New Email", style = MaterialTheme.typography.labelSmall)
            }

            // Bottom-right Resize Handle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResizeDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .testTag("floating_resize_handle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = "Drag to resize window",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkspaceBackdrop(
    windowMode: WindowMode,
    onOpenFloating: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenGuide: () -> Unit,
    onResetSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF1E293B),
                                        Color(0xFF0F172A)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailUnread,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Email On Deck",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (windowMode == WindowMode.MINIMIZED_BUBBLE)
                                "Floating Logo active - tap logo to open"
                            else
                                "Floating Window mode active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenFloating,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backdrop_open_floating_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Float Window")
                    }

                    OutlinedButton(
                        onClick = onOpenFullscreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backdrop_open_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fullscreen")
                    }
                }
            }
        }

        // Floating Control Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Floating Window Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                FloatingTipRow(
                    icon = Icons.Default.DragHandle,
                    title = "Drag to Any Side",
                    description = "Hold & drag the top header bar to move the window anywhere on screen."
                )

                FloatingTipRow(
                    icon = Icons.Default.Straighten,
                    title = "Resize Freely",
                    description = "Use the bottom-right handle or tap S, M, L to change window size anytime."
                )

                FloatingTipRow(
                    icon = Icons.Default.Remove,
                    title = "Floating Logo Bubble",
                    description = "Tap [—] to minimize to a draggable floating logo bubble."
                )

                FloatingTipRow(
                    icon = Icons.Default.PictureInPictureAlt,
                    title = "Picture-in-Picture (PiP)",
                    description = "Pop out into Android PiP mode to use over other apps."
                )
            }
        }

        // Quick Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onOpenGuide,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Captcha Guide")
            }

            FilledTonalButton(
                onClick = onResetSession,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Session")
            }
        }
    }
}

@Composable
private fun FloatingTipRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
