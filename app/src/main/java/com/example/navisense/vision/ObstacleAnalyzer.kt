package com.example.navisense.vision

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

enum class ProximityLevel {
    FAR,
    MODERATE,
    CLOSE
}

class ObstacleAnalyzer(
    private val onObstacleDetected: (DetectedObject) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAlertTime = 0L
    private val alertCooldownMs = 3000L

    private val detectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableClassification()
        .enableMultipleObjects()
        .build()

    private val objectDetector = ObjectDetection.getClient(detectorOptions)

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastAlertTime) < alertCooldownMs) {
            image.close()
            return
        }

        val mediaImage = image.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                image.imageInfo.rotationDegrees
            )

            objectDetector.process(inputImage)
                .addOnSuccessListener { detectedObjects ->
                    if (detectedObjects.isNotEmpty()) {
                        val primaryObj = detectedObjects.maxByOrNull {
                            it.boundingBox.width() * it.boundingBox.height()
                        }

                        if (primaryObj != null) {
                            val box = primaryObj.boundingBox
                            val frameWidth = image.width.toFloat()
                            val frameHeight = image.height.toFloat()

                            val boxArea = (box.width() * box.height()).toFloat()
                            val frameArea = frameWidth * frameHeight

                            val proximity = classifyProximity(boxArea, frameArea)
                            val zone = determineZone(box.centerX().toFloat(), frameWidth)

                            val rawLabel = primaryObj.labels.firstOrNull()?.text ?: "Obstacle"
                            val confidence = primaryObj.labels.firstOrNull()?.confidence ?: 0.0f

                            lastAlertTime = currentTime
                            onObstacleDetected(
                                DetectedObject(
                                    label = rawLabel,
                                    zone = zone,
                                    confidence = confidence,
                                    proximity = proximity,
                                    boundingBox = box
                                )
                            )
                        }
                    }
                }
                .addOnCompleteListener {
                    image.close()
                }
        } else {
            image.close()
        }
    }

    companion object {
        fun classifyProximity(boxArea: Float, frameArea: Float): ProximityLevel {
            if (frameArea <= 0f) return ProximityLevel.FAR
            val areaRatio = boxArea / frameArea
            return when {
                areaRatio > 0.30f -> ProximityLevel.CLOSE
                areaRatio > 0.12f -> ProximityLevel.MODERATE
                else -> ProximityLevel.FAR
            }
        }

        fun determineZone(centerX: Float, frameWidth: Float): Zone {
            if (frameWidth <= 0f) return Zone.CENTER
            return when {
                centerX < (frameWidth / 3f) -> Zone.LEFT
                centerX > (2f * frameWidth / 3f) -> Zone.RIGHT
                else -> Zone.CENTER
            }
        }
    }
}
