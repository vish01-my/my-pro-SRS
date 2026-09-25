package com.example.biometrics

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

data class FaceGuidanceState(
    val message: String = "Position face inside target oval",
    val statusColor: Color = Color.White,
    val isFaceDetected: Boolean = false,
    val isAligned: Boolean = false,
    val isBlinkDetected: Boolean = false,
    val leftEyeOpenProb: Float = 1.0f,
    val rightEyeOpenProb: Float = 1.0f,
    val headEulerY: Float = 0.0f,
    val headEulerZ: Float = 0.0f
)

@Composable
fun CameraFaceDetectionGuidanceComponent(
    modifier: Modifier = Modifier,
    onFaceVerifiedAndCaptured: (FaceGuidanceState) -> Unit,
    onManualCaptureRequested: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var guidanceState by remember { mutableStateOf(FaceGuidanceState()) }
    var useFrontCamera by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraXFaceDetectionPreview(
                useFrontCamera = useFrontCamera,
                onGuidanceUpdate = { state ->
                    guidanceState = state
                    if (state.isAligned && state.isBlinkDetected) {
                        onFaceVerifiedAndCaptured(state)
                    }
                }
            )
        } else {
            // Permission request or fallback UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Allow camera access to run live ML Kit facial biometric alignment and liveness check.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent)
                ) {
                    Text("Grant Camera Permission")
                }
            }
        }

        // HUD Overlay
        FaceGuideOvalOverlay(guidanceState = guidanceState)

        // Top Control Bar (Flip Camera & Liveness Status)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ML Kit Badge
            Surface(
                color = Color(0xAA000000),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (guidanceState.isFaceDetected) VerifiedGreen else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (guidanceState.isFaceDetected) "ML Kit: Face Tracked" else "ML Kit: Searching",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Switch Camera Button
            IconButton(
                onClick = { useFrontCamera = !useFrontCamera },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xAA000000), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom Real-Time Guidance Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xD9000000))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        guidanceState.isAligned && guidanceState.isBlinkDetected -> Icons.Default.CheckCircle
                        guidanceState.isAligned -> Icons.Default.Visibility
                        guidanceState.isFaceDetected -> Icons.Default.Info
                        else -> Icons.Default.Face
                    },
                    contentDescription = null,
                    tint = guidanceState.statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = guidanceState.message,
                    color = guidanceState.statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Diagnostic indicators: Blink detection + Angles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "Liveness: ${if (guidanceState.isBlinkDetected) "Blink Verified" else "Blink Eyes"}",
                    color = if (guidanceState.isBlinkDetected) VerifiedGreen else Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Pose: ${if (Math.abs(guidanceState.headEulerY) < 15) "Straight" else "Turn Center"}",
                    color = if (Math.abs(guidanceState.headEulerY) < 15) VerifiedGreen else WarningAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraXFaceDetectionPreview(
    useFrontCamera: Boolean,
    onGuidanceUpdate: (FaceGuidanceState) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Configure ML Kit Face Detector with Liveness Classification
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // For eye open probabilities
            .setMinFaceSize(0.20f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    var hasBlinked by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            faceDetector.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                        faceDetector.process(image)
                            .addOnSuccessListener { faces ->
                                val state = evaluateFaceGuidance(
                                    faces = faces,
                                    imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width,
                                    imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height,
                                    previousBlink = hasBlinked
                                )
                                if (state.isBlinkDetected) {
                                    hasBlinked = true
                                }
                                onGuidanceUpdate(state)
                            }
                            .addOnFailureListener { e ->
                                Log.e("CameraFaceDetection", "ML Kit Face detection failed", e)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraFaceDetection", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Real-time heuristic evaluation of face position, size, angle, and blink liveness.
 */
private fun evaluateFaceGuidance(
    faces: List<Face>,
    imageWidth: Int,
    imageHeight: Int,
    previousBlink: Boolean
): FaceGuidanceState {
    if (faces.isEmpty()) {
        return FaceGuidanceState(
            message = "Position beneficiary in front of camera",
            statusColor = Color(0xFFF87171),
            isFaceDetected = false,
            isAligned = false,
            isBlinkDetected = previousBlink
        )
    }

    if (faces.size > 1) {
        return FaceGuidanceState(
            message = "Multiple faces detected — ensure single beneficiary",
            statusColor = WarningAmber,
            isFaceDetected = true,
            isAligned = false,
            isBlinkDetected = previousBlink
        )
    }

    val face = faces[0]
    val bounds = face.boundingBox

    // 1. Distance / Size check
    val faceAreaRatio = (bounds.width().toFloat() * bounds.height()) / (imageWidth.toFloat() * imageHeight)
    if (faceAreaRatio < 0.12f) {
        return FaceGuidanceState(
            message = "Too far — move closer to camera",
            statusColor = WarningAmber,
            isFaceDetected = true,
            isAligned = false,
            isBlinkDetected = previousBlink
        )
    }
    if (faceAreaRatio > 0.70f) {
        return FaceGuidanceState(
            message = "Too close — step back slightly",
            statusColor = WarningAmber,
            isFaceDetected = true,
            isAligned = false,
            isBlinkDetected = previousBlink
        )
    }

    // 2. Pose / Euler Angles check (head pitch & yaw)
    val eulerY = face.headEulerAngleY // Left-right turn
    val eulerZ = face.headEulerAngleZ // Tilt / roll
    if (Math.abs(eulerY) > 18.0f) {
        return FaceGuidanceState(
            message = if (eulerY > 0) "Turn head slightly to the left" else "Turn head slightly to the right",
            statusColor = WarningAmber,
            isFaceDetected = true,
            isAligned = false,
            headEulerY = eulerY,
            isBlinkDetected = previousBlink
        )
    }

    // 3. Eye Openness & Blink Liveness
    val leftEyeOpen = face.leftEyeOpenProbability ?: 0.9f
    val rightEyeOpen = face.rightEyeOpenProbability ?: 0.9f

    val isCurrentBlink = (leftEyeOpen < 0.30f && rightEyeOpen < 0.30f)
    val blinkVerified = previousBlink || isCurrentBlink

    return if (!blinkVerified) {
        FaceGuidanceState(
            message = "Face aligned! Please blink naturally to confirm liveness",
            statusColor = Color(0xFF60A5FA),
            isFaceDetected = true,
            isAligned = true,
            isBlinkDetected = false,
            leftEyeOpenProb = leftEyeOpen,
            rightEyeOpenProb = rightEyeOpen,
            headEulerY = eulerY,
            headEulerZ = eulerZ
        )
    } else {
        FaceGuidanceState(
            message = "Biometrics Verified! Ready for confirmation",
            statusColor = VerifiedGreen,
            isFaceDetected = true,
            isAligned = true,
            isBlinkDetected = true,
            leftEyeOpenProb = leftEyeOpen,
            rightEyeOpenProb = rightEyeOpen,
            headEulerY = eulerY,
            headEulerZ = eulerZ
        )
    }
}

@Composable
fun FaceGuideOvalOverlay(guidanceState: FaceGuidanceState) {
    val animatedBorderColor by animateColorAsState(
        targetValue = guidanceState.statusColor,
        animationSpec = tween(300),
        label = "borderColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulseLaser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val ovalWidth = size.width * 0.62f
        val ovalHeight = size.height * 0.72f
        val left = (size.width - ovalWidth) / 2f
        val top = (size.height - ovalHeight) / 2f

        // Draw Target Oval Guide
        drawOval(
            color = animatedBorderColor,
            topLeft = Offset(left, top),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(
                width = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 15f), 0f)
            )
        )

        // Draw corner brackets
        val bracketLen = 28.dp.toPx()
        val bracketStroke = 4.dp.toPx()
        // Top Left
        drawLine(animatedBorderColor, Offset(left, top + bracketLen), Offset(left, top), bracketStroke, StrokeCap.Round)
        drawLine(animatedBorderColor, Offset(left, top), Offset(left + bracketLen, top), bracketStroke, StrokeCap.Round)

        // Top Right
        drawLine(animatedBorderColor, Offset(left + ovalWidth - bracketLen, top), Offset(left + ovalWidth, top), bracketStroke, StrokeCap.Round)
        drawLine(animatedBorderColor, Offset(left + ovalWidth, top), Offset(left + ovalWidth, top + bracketLen), bracketStroke, StrokeCap.Round)

        // Bottom Left
        drawLine(animatedBorderColor, Offset(left, top + ovalHeight - bracketLen), Offset(left, top + ovalHeight), bracketStroke, StrokeCap.Round)
        drawLine(animatedBorderColor, Offset(left, top + ovalHeight), Offset(left + bracketLen, top + ovalHeight), bracketStroke, StrokeCap.Round)

        // Bottom Right
        drawLine(animatedBorderColor, Offset(left + ovalWidth - bracketLen, top + ovalHeight), Offset(left + ovalWidth, top + ovalHeight), bracketStroke, StrokeCap.Round)
        drawLine(animatedBorderColor, Offset(left + ovalWidth, top + ovalHeight - bracketLen), Offset(left + ovalWidth, top + ovalHeight), bracketStroke, StrokeCap.Round)

        // Scanning Laser Line when tracking
        if (guidanceState.isFaceDetected && !guidanceState.isBlinkDetected) {
            val yPos = top + (ovalHeight * laserY)
            drawLine(
                color = animatedBorderColor.copy(alpha = 0.85f),
                start = Offset(left + 20f, yPos),
                end = Offset(left + ovalWidth - 20f, yPos),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
