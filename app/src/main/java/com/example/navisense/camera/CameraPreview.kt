package com.example.navisense.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.navisense.vision.DetectedObject
import com.example.navisense.vision.ObstacleAnalyzer
import com.example.navisense.vision.Zone
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    activeObstacleZone: String? = null,
    onObstacleDetected: (DetectedObject) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(
        modifier = modifier
            .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, ObstacleAnalyzer { obj ->
                                onObstacleDetected(obj)
                            })
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Perception grid sectors (LEFT | CENTER | RIGHT) & active zone highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val thirdWidth = width / 3f
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            // Draw vertical sector lines
            drawLine(
                color = Color(0x8000E5FF),
                start = Offset(thirdWidth, 0f),
                end = Offset(thirdWidth, height),
                strokeWidth = 2f,
                pathEffect = dashPathEffect
            )

            drawLine(
                color = Color(0x8000E5FF),
                start = Offset(thirdWidth * 2f, 0f),
                end = Offset(thirdWidth * 2f, height),
                strokeWidth = 2f,
                pathEffect = dashPathEffect
            )

            // Highlight active obstacle zone dynamically
            when (activeObstacleZone?.uppercase()) {
                "LEFT" -> {
                    drawRect(
                        color = Color(0x33FF5252),
                        topLeft = Offset(0f, 0f),
                        size = Size(thirdWidth, height)
                    )
                    drawRect(
                        color = Color(0xFFFF5252),
                        topLeft = Offset(0f, 0f),
                        size = Size(thirdWidth, height),
                        style = Stroke(width = 4f)
                    )
                }
                "CENTER" -> {
                    drawRect(
                        color = Color(0x33FF5252),
                        topLeft = Offset(thirdWidth, 0f),
                        size = Size(thirdWidth, height)
                    )
                    drawRect(
                        color = Color(0xFFFF5252),
                        topLeft = Offset(thirdWidth, 0f),
                        size = Size(thirdWidth, height),
                        style = Stroke(width = 4f)
                    )
                }
                "RIGHT" -> {
                    drawRect(
                        color = Color(0x33FF5252),
                        topLeft = Offset(thirdWidth * 2f, 0f),
                        size = Size(thirdWidth, height)
                    )
                    drawRect(
                        color = Color(0xFFFF5252),
                        topLeft = Offset(thirdWidth * 2f, 0f),
                        size = Size(thirdWidth, height),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}
