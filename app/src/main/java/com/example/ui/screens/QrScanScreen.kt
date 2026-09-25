package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.ParsedCardPayload
import com.example.ui.QrScanStatus
import com.example.ui.QrScannerViewModel
import com.example.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
private class QrBarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_PDF417
            )
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { raw ->
                            onBarcodeDetected(raw)
                        }
                    }
                }
                .addOnFailureListener {
                    // Suppress per-frame scan failures
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

@Composable
fun QrScanScreen(
    viewModel: QrScannerViewModel = viewModel(),
    onBack: () -> Unit,
    onTokenValidated: (ParsedCardPayload) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var cameraControlInstance by remember { mutableStateOf<Camera?>(null) }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onCameraPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onCameraPermissionResult(isGranted)
        if (!isGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Toggle camera torch whenever isTorchEnabled changes
    LaunchedEffect(uiState.isTorchEnabled, cameraControlInstance) {
        cameraControlInstance?.cameraControl?.enableTorch(uiState.isTorchEnabled)
    }

    // Laser scanning animation transition
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    Scaffold(
        topBar = {
            QrScanTopBar(
                isTorchEnabled = uiState.isTorchEnabled,
                onBack = onBack,
                onToggleTorch = { viewModel.toggleTorch() }
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.hasCameraPermission) {
                // 1. CameraX PreviewView with ML Kit Barcode Analyzer
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(
                                        cameraExecutor,
                                        QrBarcodeAnalyzer { barcodeValue ->
                                            viewModel.onBarcodeDetected(barcodeValue)
                                        }
                                    )
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControlInstance = cam
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize().testTag("camera_preview_view")
                )

                // 2. High-Tech Viewfinder Framing Overlay
                ViewfinderOverlay(
                    scanStatus = uiState.scanStatus,
                    laserProgress = laserProgress
                )
            } else {
                // Camera Permission Fallback Card
                CameraPermissionCard(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }

            // 3. Top Floating Status Chip
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                ScanStatusBadge(scanStatus = uiState.scanStatus)
            }

            // 4. Bottom Controls & Interactive Result Sheet
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fast Demo Simulation Selector (for rapid testing in browser emulator)
                DemoSimulationRow(
                    onSimulateCard = { cardId, isTampered ->
                        viewModel.simulatePreloadedCard(cardId, isTampered)
                    }
                )

                // Card Validation Result Bottom Card
                AnimatedVisibility(
                    visible = uiState.scanStatus !is QrScanStatus.Scanning && uiState.scanStatus !is QrScanStatus.Idle,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    when (val status = uiState.scanStatus) {
                        is QrScanStatus.Validating -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("validating_card"),
                                colors = CardDefaults.cardColors(containerColor = NavyDark.copy(alpha = 0.95f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = SaffronAccent,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "Verifying Cryptographic Seal...",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            "Checking HMAC-SHA256 signature against National Root Key",
                                            color = Color.LightGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        is QrScanStatus.Success -> {
                            ValidatedSuccessCard(
                                parsedCard = status.parsedCard,
                                onProceed = { onTokenValidated(status.parsedCard) },
                                onScanAgain = { viewModel.resumeScanning() }
                            )
                        }

                        is QrScanStatus.Error -> {
                            ValidationErrorCard(
                                errorCode = status.errorCode,
                                errorMessage = status.errorMessage,
                                onRetry = { viewModel.resumeScanning() }
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrScanTopBar(
    isTorchEnabled: Boolean,
    onBack: () -> Unit,
    onToggleTorch: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Smart Ration Card Scanner",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                Text(
                    "Cryptographic Token Verification",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("qr_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier.testTag("qr_torch_toggle")
            ) {
                Icon(
                    imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (isTorchEnabled) "Turn Torch Off" else "Turn Torch On",
                    tint = if (isTorchEnabled) SaffronAccent else Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    )
}

@Composable
private fun ViewfinderOverlay(
    scanStatus: QrScanStatus,
    laserProgress: Float
) {
    val cornerColor = when (scanStatus) {
        is QrScanStatus.Success -> EmeraldGreen
        is QrScanStatus.Error -> DangerRed
        is QrScanStatus.Validating -> SaffronAccent
        else -> SaffronAccent
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = maxWidth
        val canvasHeight = maxHeight
        val boxSize = minOf(canvasWidth * 0.72f, 290.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = (size.width - boxSize.toPx()) / 2
            val top = (size.height - boxSize.toPx()) / 2.3f
            val right = left + boxSize.toPx()
            val bottom = top + boxSize.toPx()

            // 1. Semi-transparent black scrim cutout
            val scrimColor = Color(0x99000000)
            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                )
            }
            drawPath(path = path, color = scrimColor)

            // 2. Corner reticle brackets
            val cornerLength = 36.dp.toPx()
            val strokeWidth = 5.dp.toPx()

            // Top-Left Corner
            drawLine(cornerColor, Offset(left, top + cornerLength), Offset(left, top), strokeWidth, StrokeCap.Round)
            drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth, StrokeCap.Round)

            // Top-Right Corner
            drawLine(cornerColor, Offset(right - cornerLength, top), Offset(right, top), strokeWidth, StrokeCap.Round)
            drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth, StrokeCap.Round)

            // Bottom-Left Corner
            drawLine(cornerColor, Offset(left, bottom - cornerLength), Offset(left, bottom), strokeWidth, StrokeCap.Round)
            drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth, StrokeCap.Round)

            // Bottom-Right Corner
            drawLine(cornerColor, Offset(right - cornerLength, bottom), Offset(right, bottom), strokeWidth, StrokeCap.Round)
            drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth, StrokeCap.Round)

            // 3. Laser scan line
            if (scanStatus is QrScanStatus.Scanning) {
                val laserY = top + (boxSize.toPx() * laserProgress)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            SaffronAccent.copy(alpha = 0.8f),
                            Color.White,
                            SaffronAccent.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(left + 8.dp.toPx(), laserY),
                    end = Offset(right - 8.dp.toPx(), laserY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun ScanStatusBadge(scanStatus: QrScanStatus) {
    val (bgColor, textColor, icon, label) = when (scanStatus) {
        is QrScanStatus.Success -> Quad(
            EmeraldGreen.copy(alpha = 0.9f),
            Color.White,
            Icons.Default.VerifiedUser,
            "CRYPTOGRAPHIC SEAL VERIFIED"
        )
        is QrScanStatus.Error -> Quad(
            DangerRed.copy(alpha = 0.9f),
            Color.White,
            Icons.Default.GppBad,
            "INVALID / TAMPERED TOKEN"
        )
        is QrScanStatus.Validating -> Quad(
            SaffronAccent.copy(alpha = 0.95f),
            NavyDark,
            Icons.Default.Security,
            "AUTHENTICATING TOKEN..."
        )
        else -> Quad(
            NavyDark.copy(alpha = 0.85f),
            Color.White,
            Icons.Default.QrCodeScanner,
            "ALIGN QR CODE INSIDE FRAME"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun ValidatedSuccessCard(
    parsedCard: ParsedCardPayload,
    onProceed: () -> Unit,
    onScanAgain: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("validated_success_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Beneficiary Authenticated",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = NavyDark
                    )
                    Text(
                        "Government PDS Digital Seal Validated",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = EmeraldGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "SECURE",
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Token Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CARD IDENTIFIER", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(parsedCard.cardId, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("HMAC SEAL (SHA-256)", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        parsedCard.signature.take(12) + "...",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NavyDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("NONCE TOKEN", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(parsedCard.nonce, fontSize = 12.sp, color = Color.DarkGray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PAYLOAD VERSION", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(parsedCard.version, fontSize = 12.sp, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier.weight(1f).testTag("scan_again_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Scan Again")
                }

                Button(
                    onClick = onProceed,
                    modifier = Modifier.weight(1.5f).testTag("proceed_biometrics_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Proceed", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ValidationErrorCard(
    errorCode: String,
    errorMessage: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("validation_error_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DangerRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Authentication Rejected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DangerRed
                    )
                    Text(
                        "Error Code: $errorCode",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                errorMessage,
                fontSize = 13.sp,
                color = Color.DarkGray,
                lineHeight = 18.sp
            )

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().testTag("retry_scan_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Another Card")
            }
        }
    }
}

@Composable
private fun DemoSimulationRow(
    onSimulateCard: (cardId: String, isTampered: Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("demo_simulation_selector"),
        colors = CardDefaults.cardColors(containerColor = NavyDark.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null,
                    tint = SaffronAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Quick Test Smart Cards (Browser/Emulator Preset)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    DemoChip(
                        label = "Ramesh Kumar (Delhi PHH)",
                        tag = "demo_card_ramesh",
                        onClick = { onSimulateCard("SRC-DL-2026-99214", false) }
                    )
                }
                item {
                    DemoChip(
                        label = "Sunita Devi (Delhi AAY)",
                        tag = "demo_card_sunita",
                        onClick = { onSimulateCard("SRC-DL-2026-88102", false) }
                    )
                }
                item {
                    DemoChip(
                        label = "Rajesh Patel (Gujarat ONORC)",
                        tag = "demo_card_rajesh",
                        onClick = { onSimulateCard("SRC-GJ-2026-14029", false) }
                    )
                }
                item {
                    DemoChip(
                        label = "Tampered Card (Forgery)",
                        isDanger = true,
                        tag = "demo_card_tampered",
                        onClick = { onSimulateCard("SRC-DL-2026-99214", true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoChip(
    label: String,
    isDanger: Boolean = false,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isDanger) DangerRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
        modifier = Modifier.testTag(tag)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isDanger) Color(0xFFFF8080) else Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CameraPermissionCard(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavyDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = SaffronAccent,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Camera Access Required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Smart Ration Card scanning requires camera permissions to capture and cryptographically authenticate the encrypted QR tokens.",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Grant Permission", color = NavyDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
