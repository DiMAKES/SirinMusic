package ru.rainedev.sirinmusic.ui.qr

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ограничиваем декодирование больших снимков, не загружая оригинал целиком в память. */
internal suspend fun readQrImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@withContext null
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true,
            )).text
        } finally { bitmap.recycle() }
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { null }
}
