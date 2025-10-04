package com.aln.ultiwear.view.screens

import android.Manifest.permission.CAMERA
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.util.UUID


@Composable
fun QuickTradeScreen() {
    // checks if qrcode scanner should be shown
    var showScanner by remember { mutableStateOf(false) }
    // stores the sessionID obtained from scanning
    var scannedSessionId by remember { mutableStateOf<String?>(null) }
    // stores the bitmap of the generated qrcode
    var generatedQrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // ensure the user has granted camera access before showing any camera-related functionality
    CameraPermissionWrapper {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            if (showScanner) {
                // display camera preview (qrcode scanner) and close button
                CameraPreviewView { qr ->
                    scannedSessionId = qr
                    showScanner = false
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showScanner = false }) {
                    Text("Close Scanner")
                }

                // if a qrcode has been scanned successfully, display the session id (debug)
                scannedSessionId?.let { sessionId ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanned Session ID: $sessionId")
                }

            } else {
                // open qrcode scanner
                Button(
                    onClick = { showScanner = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Scan QR Code")
                }
                // generate qrcode
                Button(
                    onClick = {
                        val sessionId = UUID.randomUUID().toString()
                        // convert sessionId to qrcode bitmap
                        generatedQrBitmap = generateQrCode(sessionId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Generate QR Code")
                }

                // display the generated qrcode
                generatedQrBitmap?.let { bitmap ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPermissionWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // check if the app has camera access
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // request permissions from the user
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

    // runs when the composable enters composition
    LaunchedEffect(Unit) {
        // if the camera permission is not granted, it
        // launches the permission request dialog
        if (!hasPermission) {
            launcher.launch(CAMERA)
        }
    }

    if (hasPermission) {
        // if has permission, shows the content passed to the wrapper
        content()
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("Camera permission required")
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewView(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // shows the camera feed
    val previewView = remember { PreviewView(context) }

    // wraps previewView in a composable
    AndroidView(
        factory = {
            previewView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    )

    LaunchedEffect(previewView) {
        previewView.post {
            // get a camera provider asynchronously
            val cameraProviderFuture = ProcessCameraProvider
                .getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // a Preview object is created and attached to the PreviewView
                // so the live camera feed is visible
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // get the object responsible for recognizing QR codes from camera frames
                val barcodeScanner = BarcodeScanning.getClient()

                // imageAnalysis processes each frame from the camera.
                val imageAnalysis = ImageAnalysis.Builder()
                    // tells the analyzer to discard older frames if it can’t keep up.
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // set a frame analyzer that gets called for each frame
                imageAnalysis.setAnalyzer(
                    // ensure that the analyzer runs on the main thread.
                    ContextCompat.getMainExecutor(context)
                ) { imageProxy ->
                    //  get actual camera frame as an Image object
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        // convert the mediaImage into an InputImage
                        val inputImage =
                            InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                        // send the InputImage the barcodeScanner for analysis
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let {
                                        onQrScanned(it)
                                        break
                                    }
                                }
                            }
                            .addOnFailureListener { }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        // if the frame is null, close the imageProxy immediately to free resources
                        imageProxy.close()
                    }
                }

                try {
                    // clear any previously active camera bindings
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
}


private fun generateQrCode(text: String, size: Int = 512): Bitmap {
    // setup parameters to pass to the qrcode encoder
    val hints = hashMapOf<EncodeHintType, Any>()
    hints[EncodeHintType.MARGIN] = 1
    // convert text into a bitMatrix
    val bitMatrix = MultiFormatWriter().encode(
        text,
        BarcodeFormat.QR_CODE,
        size,
        size,
        hints
    )
    // create an empty bitmap
    val bitmap = createBitmap(size, size)
    // draw the qrcode pixel by pixel
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return bitmap
}
