package com.android.whatsapp.presentation.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.android.whatsapp.ui.theme.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

// onCapture returns the Uri of the captured photo.
// If chatId is provided (navigated from ConversationScreen),
// the caller sends it as a media message directly.
@Composable
fun CameraScreen(
    onBack   : () -> Unit,
    onCapture: (Uri) -> Unit   // caller decides what to do with the Uri
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing   by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode    by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var capturedUri  by remember { mutableStateOf<Uri?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(lensFacing, flashMode) {
        val provider = context.getCameraProvider()
        provider.unbindAll()
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val preview  = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val ic = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = ic
        runCatching { provider.bindToLifecycle(lifecycleOwner, selector, preview, ic) }
    }

    if (capturedUri != null) {
        CapturePreview(
            uri     = capturedUri!!,
            onRetake = { capturedUri = null },
            onSend   = {
                onCapture(capturedUri!!)
                capturedUri = null
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Camera preview
        androidx.compose.ui.viewinterop.AndroidView(
            factory  = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = onBack,
                modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            // Flash toggle
            IconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF  -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON   -> ImageCapture.FLASH_MODE_AUTO
                        else                         -> ImageCapture.FLASH_MODE_OFF
                    }
                },
                modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)
            ) {
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

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Spacer placeholder (left)
                Spacer(Modifier.size(48.dp))

                // Shutter button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(3.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            val ic  = imageCapture ?: return@clickable
                            val file = context.createMediaFile("jpg")
                            val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                            ic.takePicture(
                                opts,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                        capturedUri = out.savedUri ?: Uri.fromFile(file)
                                    }
                                    override fun onError(e: ImageCaptureException) { e.printStackTrace() }
                                }
                            )
                        }
                )

                // Flip camera
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// ── Capture preview — confirm or retake ──────────────────────

@Composable
private fun CapturePreview(uri: Uri, onRetake: () -> Unit, onSend: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model              = uri,
            contentDescription = "Preview",
            contentScale       = ContentScale.Fit,
            modifier           = Modifier.fillMaxSize()
        )
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick  = onRetake,
                modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Retake", tint = Color.White)
            }
        }
        // Bottom send button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
        }
    }

private fun Context.createMediaFile(extension: String): File {
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return File(externalCacheDir, "WA_${ts}.$extension")
}