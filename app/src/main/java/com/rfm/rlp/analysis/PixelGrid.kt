package com.rfm.rlp.analysis

import android.graphics.Bitmap

/** وصول سريع لبكسلات الصورة مع قص الإحداثيات تلقائياً داخل الحدود. */
class PixelGrid(bitmap: Bitmap) {

    val width: Int = bitmap.width
    val height: Int = bitmap.height

    private val pixels = IntArray(width * height).also {
        bitmap.getPixels(it, 0, width, 0, 0, width, height)
    }

    private fun index(x: Int, y: Int): Int {
        val cx = x.coerceIn(0, width - 1)
        val cy = y.coerceIn(0, height - 1)
        return cy * width + cx
    }

    fun red(x: Int, y: Int): Int = (pixels[index(x, y)] shr 16) and 0xFF

    fun green(x: Int, y: Int): Int = (pixels[index(x, y)] shr 8) and 0xFF

    fun blue(x: Int, y: Int): Int = pixels[index(x, y)] and 0xFF

    fun luminance(x: Int, y: Int): Float {
        val p = pixels[index(x, y)]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    /** هل البكسل قريب من الأبيض (كل القنوات >= الحد الأدنى)؟ */
    fun isWhiteLike(x: Int, y: Int, minChannel: Int): Boolean {
        val p = pixels[index(x, y)]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        return r >= minChannel && g >= minChannel && b >= minChannel
    }
}
