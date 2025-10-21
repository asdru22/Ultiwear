package com.aln.ultiwear.view.screens

import android.graphics.Bitmap
import android.graphics.Color
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aln.ultiwear.viewModel.ManualTradeViewModel
import com.aln.ultiwear.viewModel.TradeSessionViewModel
import com.aln.ultiwear.viewModel.WardrobeViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.launch


@Composable
fun QuickTradeScreen(
    manualTradeViewModel: ManualTradeViewModel,
    wardrobeViewModel: WardrobeViewModel
) {

    val viewModel: TradeSessionViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TradeSessionViewModel(
                    wardrobeViewModel = wardrobeViewModel,
                    manualTradeViewModel = manualTradeViewModel
                )
            }
        }
    )

    var showScanner by remember { mutableStateOf(false) }
    var qrDialogVisible by remember { mutableStateOf(false) }
    var generatedQrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sessionState by viewModel.sessionState.collectAsState()

    when {
        sessionState?.isReady == true -> {
            sessionState?.let { state ->
                TradeSessionScreen(
                    sessionId = state.sessionId,
                    viewModel = viewModel,
                    wardrobeViewModel = wardrobeViewModel
                )
            } ?: run {
                // if the session is delete/ends show the initial screen
                LaunchedEffect(Unit) {
                    Toast.makeText(
                        context,
                        "Trade completed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // qrcode scanner
        showScanner -> {
            CameraPreviewView(
                onQrScanned = { qr ->
                    showScanner = false
                    coroutineScope.launch {
                        try {
                            viewModel.joinTradeSession(qr)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Error joining session: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                closeScanner = {
                    showScanner = false
                }
            )
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showScanner = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Scan QR Code")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val sessionId = viewModel.createTradeSession()
                                generatedQrBitmap = generateQrCode(sessionId)
                                qrDialogVisible = true
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Error creating session: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Start Trade Session")
                }
            }
        }
    }

    LaunchedEffect(sessionState) {
        if (sessionState?.isReady == true) {
            qrDialogVisible = false
        }
    }

    // dialog that shows the generated qrcode
    if (qrDialogVisible && generatedQrBitmap != null) {
        AlertDialog(
            onDismissRequest = { qrDialogVisible = false },
            title = { Text("Scan to Join") },
            text = {
                Image(
                    bitmap = generatedQrBitmap!!.asImageBitmap(),
                    contentDescription = "Trade Session QR",
                    modifier = Modifier.size(250.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { qrDialogVisible = false }) {
                    Text("Close")
                }
            }
        )
    }

}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewView(onQrScanned: (String) -> Unit, closeScanner: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // camera feed
        AndroidView(
            factory = {
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // close button overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = { closeScanner() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "Close Scanner",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }

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

                // imageAnalysis processes each frame from the camera
                val imageAnalysis = ImageAnalysis.Builder()
                    // tells the analyzer to discard older frames if it can’t keep up
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // set a frame analyzer that gets called for each frame
                imageAnalysis.setAnalyzer(
                    // ensure that the analyzer runs on the main thread
                    ContextCompat.getMainExecutor(context)
                ) { imageProxy ->
                    //  get actual camera frame as an Image object
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        // convert the mediaImage into an InputImage
                        val inputImage = InputImage.fromMediaImage(
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
                // run the code on the main (UI) thread
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
