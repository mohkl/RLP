package com.rfm.rlp.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

enum class ImageFormat(val label: String) {
    JPEG("JPEG"),
    PNG("PNG"),
    WEBP("WebP"),
    HEIC("HEIC/HEIF"),
    GIF("GIF"),
    BMP("BMP"),
    UNKNOWN("غير معروفة")
}

class LoadedImage(
    /** صورة مُحلَّلة ومُصحَّح اتجاهها (قد تكون مصغّرة للتحليل فقط). */
    val bitmap: Bitmap,
    /** حجم الملف الأصلي بالبايت. */
    val sizeBytes: Int,
    val format: ImageFormat,
    /** الأبعاد الحقيقية للصورة الأصلية بعد تصحيح الاتجاه. */
    val width: Int,
    val height: Int,
    val hasMetadata: Boolean,
    val captureDateMillis: Long?,
    /** true إذا كان التاريخ المتاح هو تاريخ التعديل وليس تاريخ الالتقاط. */
    val dateIsModification: Boolean,
    val software: String?,
    val cameraMake: String?,
    val cameraModel: String?
)

object ImageLoader {

    private const val MAX_ANALYSIS_SIDE = 1280

    suspend fun load(context: Context, uri: Uri): LoadedImage = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("تعذّر فتح الملف المختار.")

        val format = detectFormat(bytes)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("الملف المختار ليس صورة صالحة.")
        }

        var orientation = ExifInterface.ORIENTATION_NORMAL
        var dateMillis: Long? = null
        var dateIsModification = false
        var software: String? = null
        var make: String? = null
        var model: String? = null

        try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.trim()
            make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
            model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()

            val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply { isLenient = false }
            fun parse(value: String?): Long? = try {
                if (value.isNullOrBlank()) null else parser.parse(value.trim())?.time
            } catch (e: Exception) {
                null
            }

            val original = parse(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
            val digitized = parse(exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED))
            val modified = parse(exif.getAttribute(ExifInterface.TAG_DATETIME))

            when {
                original != null -> dateMillis = original
                digitized != null -> dateMillis = digitized
                modified != null -> {
                    dateMillis = modified
                    dateIsModification = true
                }
            }
        } catch (e: Exception) {
            // لا توجد بيانات وصفية قابلة للقراءة
        }

        val hasMetadata = !software.isNullOrBlank() || !make.isNullOrBlank() ||
                !model.isNullOrBlank() || dateMillis != null

        val swap = orientation in 5..8
        val realWidth = if (swap) bounds.outHeight else bounds.outWidth
        val realHeight = if (swap) bounds.outWidth else bounds.outHeight

        var sample = 1
        val longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / sample > MAX_ANALYSIS_SIDE) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IllegalStateException("تعذّر قراءة الصورة. قد تكون الصيغة غير مدعومة.")

        val oriented = applyOrientation(decoded, orientation)

        // تسطيح الشفافية على خلفية بيضاء لضمان صورة معتمة للتحليل
        val flat = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(oriented, 0f, 0f, null)
        }

        LoadedImage(
            bitmap = flat,
            sizeBytes = bytes.size,
            format = format,
            width = realWidth,
            height = realHeight,
            hasMetadata = hasMetadata,
            captureDateMillis = dateMillis,
            dateIsModification = dateIsModification,
            software = software,
            cameraMake = make,
            cameraModel = model
        )
    }

    private fun detectFormat(bytes: ByteArray): ImageFormat {
        fun b(i: Int): Int = if (i < bytes.size) bytes[i].toInt() and 0xFF else -1
        return when {
            b(0) == 0xFF && b(1) == 0xD8 && b(2) == 0xFF -> ImageFormat.JPEG
            b(0) == 0x89 && b(1) == 0x50 && b(2) == 0x4E && b(3) == 0x47 -> ImageFormat.PNG
            b(0) == 0x47 && b(1) == 0x49 && b(2) == 0x46 -> ImageFormat.GIF
            b(0) == 0x42 && b(1) == 0x4D -> ImageFormat.BMP
            b(0) == 0x52 && b(1) == 0x49 && b(2) == 0x46 && b(3) == 0x46 &&
                    b(8) == 0x57 && b(9) == 0x45 && b(10) == 0x42 && b(11) == 0x50 -> ImageFormat.WEBP
            b(4) == 0x66 && b(5) == 0x74 && b(6) == 0x79 && b(7) == 0x70 -> ImageFormat.HEIC
            else -> ImageFormat.UNKNOWN
        }
    }

    private fun applyOrientation(src: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                m.postRotate(90f)
                m.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                m.postRotate(270f)
                m.postScale(-1f, 1f)
            }
            else -> return src
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }
}
