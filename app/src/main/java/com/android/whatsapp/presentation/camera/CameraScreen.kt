package com.android.whatsapp.presentation.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.android.whatsapp.ui.theme.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onCapture: (Uri) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing     by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode      by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isVideoMode    by remember { mutableStateOf(false) }
    var isRecording    by remember { mutableStateOf(false) }
    var capturedUri    by remember { mutableStateOf<Uri?>(null) }

    val previewView    = remember { PreviewView(context) }
    var imageCapture   by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture   by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording      by remember { mutableStateOf<Recording?>(null) }

    // Bind camera whenever lens or mode changes
    LaunchedEffect(lensFacing, isVideoMode) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        if (isVideoMode) {
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            val vc = VideoCapture.withOutput(recorder)
            videoCapture = vc
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
        } else {
            val ic = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .build()
            imageCapture = ic
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, ic)
        }
    }

    if (capturedUri != null) {
        // Preview captured photo
        CapturePreview(
            uri     = capturedUri!!,
            onRetake = { capturedUri = null },
            onSend   = { onCapture(capturedUri!!) }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Camera preview
        AndroidView(
            factory  = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            // Flash toggle (photo mode only)
            if (!isVideoMode) {
                IconButton(onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF  -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON   -> ImageCapture.FLASH_MODE_AUTO
                        else                         -> ImageCapture.FLASH_MODE_OFF
                    }
                }) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON   -> Icons.Default.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else                         -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo / Video toggle
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModeTab("PHOTO", !isVideoMode) { isVideoMode = false }
                ModeTab("VIDEO",  isVideoMode) { isVideoMode = true  }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Gallery placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface700),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                }

                // Shutter button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(3.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) Color.Red else Color.White)
                        .clickable {
                            if (isVideoMode) {
                                if (isRecording) {
                                    recording?.stop()
                                    isRecording = false
                                } else {
                                    val file = context.createMediaFile("mp4")
                                    val options = FileOutputOptions.Builder(file).build()
                                    recording = videoCapture?.output
                                        ?.prepareRecording(context, options)
                                        ?.start(ContextCompat.getMainExecutor(context)) { event ->
                                            if (event is VideoRecordEvent.Finalize && !event.hasError()) {
                                                capturedUri = event.outputResults.outputUri
                                            }
                                        }
                                    isRecording = true
                                }
                            } else {
                                val file = context.createMediaFile("jpg")
                                val options = ImageCapture.OutputFileOptions.Builder(file).build()
                                imageCapture?.takePicture(
                                    options,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            capturedUri = output.savedUri ?: Uri.fromFile(file)
                                        }
                                        override fun onError(e: ImageCaptureException) {
                                            e.printStackTrace()
                                        }
                                    }
                                )
                            }
                        }
                )

                // Flip camera
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                    }
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.Black else Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun CapturePreview(uri: Uri, onRetake: () -> Unit, onSend: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        coil3.compose.AsyncImage(
            model              = uri,
            contentDescription = "Preview",
            contentScale       = androidx.compose.ui.layout.ContentScale.Fit,
            modifier           = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick  = onRetake,
                modifier = Modifier.size(56.dp).background(Color.Black.copy(.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Retake", tint = Color.White)
            }
            IconButton(
                onClick  = onSend,
                modifier = Modifier.size(56.dp).background(Green500, CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
        }
    }

private fun Context.createMediaFile(extension: String): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return File(externalCacheDir, "WA_${timestamp}.$extension")
}