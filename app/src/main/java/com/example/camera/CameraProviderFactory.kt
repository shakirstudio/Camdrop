package com.example.camera

import android.content.Context
import com.example.model.CameraBrand

class CameraProviderFactory(private val context: Context) {
    fun createProvider(brand: CameraBrand): CameraProvider {
        return when (brand) {
            CameraBrand.SONY -> SonyCameraProvider(context)
            CameraBrand.CANON -> CanonCameraProvider(context)
            CameraBrand.NIKON -> NikonCameraProvider(context)
            else -> SonyCameraProvider(context)
        }
    }
}
