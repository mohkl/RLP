package com.rfm.rlp.analysis

import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object FaceChecks {

    private const val GROUP = "ثانياً: تعبير الوجه"

    fun run(
        grid: PixelGrid,
        faceCount: Int,
        face: Face?,
        backgroundIsWhite: Boolean
    ): List<CheckResult> {
        val results = mutableListOf<CheckResult>()
        results += countCheck(faceCount)

        if (face == null) {
            val msg = "تعذّر هذا الفحص لأنه لم يتم اكتشاف وجه واضح في الصورة."
            val fix = "التقط صورة يظهر فيها الوجه كاملاً وواضحاً ومواجهاً للكاميرا."
            results += CheckResult(GROUP, "النظر مباشرة إلى الكاميرا", Status.REVIEW, msg, fix)
            results += CheckResult(GROUP, "تعبير وجه محايد دون ابتسامة", Status.REVIEW, msg, fix)
            results += CheckResult(GROUP, "حجم الرأس (50% إلى 69% من طول الصورة)", Status.REVIEW, msg, fix)
            return results
        }

        results += gazeCheck(grid, face)
        results += expressionCheck(face)
        results += headSizeCheck(grid, face, backgroundIsWhite)
        return results
    }

    private fun points(face: Face, type: Int): List<PointF> =
        face.getContour(type)?.points ?: emptyList()

    private fun countCheck(count: Int): CheckResult {
        val title = "اكتشاف الوجه ووجود شخص واحد فقط"
        return when (count) {
            0 -> CheckResult(
                GROUP, title, Status.FAIL,
                "لم يتم اكتشاف أي وجه في الصورة.",
                "استخدم صورة يظهر فيها وجهك كاملاً وبوضوح، ومواجهاً للكاميرا."
            )
            1 -> CheckResult(GROUP, title, Status.PASS, "تم اكتشاف وجه واحد في الصورة.", null)
            else -> CheckResult(
                GROUP, title, Status.FAIL,
                "تم اكتشاف $count وجوه في الصورة.",
                "يجب أن تحتوي الصورة على شخص واحد فقط. اقصّ الصورة أو التقط صورة جديدة."
            )
        }
    }

    // ---------- اتجاه النظر ----------
    private fun gazeCheck(grid: PixelGrid, face: Face): CheckResult {
        val title = "النظر مباشرة إلى الكاميرا"
        val yaw = abs(face.headEulerAngleY)
        val pitch = abs(face.headEulerAngleX)
        val roll = abs(face.headEulerAngleZ)

        var status = Status.PASS
        val notes = mutableListOf<String>()

        notes += "زوايا الرأس: يمين/يسار ${yaw.roundToInt()}°، أعلى/أسفل ${pitch.roundToInt()}°، ميلان ${roll.roundToInt()}°."

        if (yaw > 18f || pitch > 20f || roll > 15f) {
            status = Status.FAIL
            notes += "الرأس غير موجّه نحو الكاميرا بشكل مباشر."
        } else if (yaw > 8f || pitch > 12f || roll > 8f) {
            status = worstOf(status, Status.REVIEW)
            notes += "الرأس مائل قليلاً عن الاتجاه المباشر للكاميرا."
        }

        val openProbs = listOfNotNull(face.leftEyeOpenProbability, face.rightEyeOpenProbability)
        val minOpen = openProbs.minOrNull()
        if (minOpen != null) {
            if (minOpen < 0.3f) {
                status = Status.FAIL
                notes += "إحدى العينين أو كلتيهما تبدو مغلقة."
            } else if (minOpen < 0.6f) {
                status = worstOf(status, Status.REVIEW)
                notes += "العينان غير مفتوحتين بشكل كامل على ما يبدو."
            }
        }

        val offsets = listOfNotNull(
            irisOffset(grid, points(face, FaceContour.LEFT_EYE)),
            irisOffset(grid, points(face, FaceContour.RIGHT_EYE))
        )
        if (offsets.isNotEmpty()) {
            val avg = offsets.map { abs(it - 0.5f) }.average().toFloat()
            if (avg > 0.20f) {
                status = worstOf(status, Status.REVIEW)
                notes += "موضع بؤبؤ العين يبدو بعيداً عن المنتصف (تقدير تقريبي)."
            } else {
                notes += "موضع بؤبؤ العين قريب من المنتصف (تقدير تقريبي)."
            }
        } else {
            status = worstOf(status, Status.REVIEW)
            notes += "تعذّر تقدير موضع بؤبؤ العين."
        }

        notes += "ملاحظة: اتجاه النظر يُقدَّر تقريبياً من وضع الرأس والعينين."

        return CheckResult(
            group = GROUP,
            title = title,
            status = status,
            details = notes.joinToString("\n"),
            suggestion = if (status == Status.PASS) null else
                "اجلس مواجهاً للكاميرا مباشرة، وارفع الهاتف إلى مستوى العينين، وانظر إلى عدسة الكاميرا وعيناك مفتوحتان."
        )
    }

    /** يعيد موضع مركز الجزء الداكن (البؤبؤ) داخل العين كنسبة بين 0 و1. */
    private fun irisOffset(grid: PixelGrid, eye: List<PointF>): Float? {
        if (eye.size < 6) return null
        val minX = eye.minOf { it.x }.toInt()
        val maxX = eye.maxOf { it.x }.toInt()
        val minY = eye.minOf { it.y }
        val maxY = eye.maxOf { it.y }
        val ew = maxX - minX
        val eh = maxY - minY
        if (ew < 10 || eh < 3f) return null

        val y0 = (minY + eh * 0.25f).toInt()
        val y1 = (maxY - eh * 0.25f).toInt()
        if (y1 < y0) return null

        var sum = 0f
        var n = 0
        for (y in y0..y1) {
            for (x in minX..maxX) {
                sum += grid.luminance(x, y)
                n++
            }
        }
        if (n == 0) return null

        val threshold = sum / n * 0.75f
        var totalWeight = 0f
        var weightedX = 0f
        for (y in y0..y1) {
            for (x in minX..maxX) {
                val lum = grid.luminance(x, y)
                if (lum < threshold) {
                    val weight = threshold - lum
                    totalWeight += weight
                    weightedX += weight * (x - minX)
                }
            }
        }
        if (totalWeight <= 0f) return null
        return (weightedX / totalWeight) / ew
    }

    // ---------- التعبير ----------
    private fun expressionCheck(face: Face): CheckResult {
        val title = "تعبير وجه محايد وطبيعي دون ابتسامة"
        var status = Status.PASS
        val notes = mutableListOf<String>()

        val smile = face.smilingProbability
        if (smile == null) {
            status = Status.REVIEW
            notes += "تعذّر تحديد وجود ابتسامة."
        } else {
            val p = (smile * 100).roundToInt()
            when {
                smile < 0.2f -> notes += "احتمال وجود ابتسامة منخفض ($p%)، والتعبير يبدو محايداً."
                smile < 0.5f -> {
                    status = Status.REVIEW
                    notes += "قد توجد ابتسامة خفيفة (احتمالها $p%)."
                }
                else -> {
                    status = Status.FAIL
                    notes += "الوجه يبتسم على الأرجح (احتمال الابتسامة $p%)."
                }
            }
        }

        val upper = points(face, FaceContour.UPPER_LIP_BOTTOM)
        val lower = points(face, FaceContour.LOWER_LIP_TOP)
        if (upper.isNotEmpty() && lower.isNotEmpty()) {
            val gap = lower.map { it.y }.average() - upper.map { it.y }.average()
            val relative = gap / face.boundingBox.height().toDouble()
            if (relative > 0.05) {
                status = worstOf(status, Status.REVIEW)
                notes += "الفم يبدو مفتوحاً قليلاً."
            }
        }

        return CheckResult(
            group = GROUP,
            title = title,
            status = status,
            details = notes.joinToString("\n"),
            suggestion = if (status == Status.PASS) null else
                "أبقِ الفم مغلقاً بشكل طبيعي دون ابتسامة، وحافظ على ملامح وجه هادئة ومحايدة."
        )
    }

    // ---------- حجم الرأس ----------
    private fun headSizeCheck(grid: PixelGrid, face: Face, backgroundIsWhite: Boolean): CheckResult {
        val title = "حجم الرأس (50% إلى 69% من طول الصورة)"
        val box = face.boundingBox
        val h = grid.height

        val facePoints = points(face, FaceContour.FACE)
        val chinY = if (facePoints.isNotEmpty()) facePoints.maxOf { it.y } else box.bottom.toFloat()

        var topY: Float? = null
        if (backgroundIsWhite) {
            val cx = box.centerX()
            val half = (box.width() * 0.25f).toInt()
            val x0 = max(0, cx - half)
            val x1 = min(grid.width - 1, cx + half)
            val yEnd = min(box.top + (box.height() * 0.1f).toInt(), h)
            var y = 0
            while (y < yEnd && topY == null) {
                var nonWhite = 0
                var total = 0
                var x = x0
                while (x <= x1) {
                    if (!grid.isWhiteLike(x, y, 215)) nonWhite++
                    total++
                    x += 2
                }
                if (total > 0 && nonWhite.toFloat() / total >= 0.4f) topY = y.toFloat()
                y++
            }
        }

        val exact = topY != null
        val top = topY ?: (box.top - box.height() * 0.30f)
        val ratio = (chinY - top) / h
        val percent = (ratio * 100).roundToInt()

        var status = when {
            ratio in 0.50f..0.69f -> Status.PASS
            ratio in 0.46f..0.73f -> Status.REVIEW
            else -> Status.FAIL
        }

        val notes = mutableListOf<String>()
        notes += "الرأس (من أعلى الشعر إلى الذقن) يشغل تقريباً $percent% من طول الصورة."
        if (!exact) {
            if (status == Status.PASS) status = Status.REVIEW
            notes += "هذا تقدير تقريبي لأن الخلفية غير بيضاء، فلم يمكن تحديد أعلى الرأس بدقة."
        }

        val suggestion = when {
            status == Status.PASS -> null
            ratio < 0.50f -> "قرّب الكاميرا من الوجه أو اقصّ الصورة حول الرأس والكتفين لتصبح نسبة الرأس بين 50% و69%."
            ratio > 0.69f -> "ابتعد قليلاً عن الكاميرا أو أبقِ مساحة أكبر حول الرأس لتصبح نسبة الرأس بين 50% و69%."
            else -> "تأكد بنفسك من نسبة الرأس إلى الصورة (50% إلى 69%) وأن الخلفية بيضاء."
        }

        return CheckResult(GROUP, title, status, notes.joinToString("\n"), suggestion)
    }
}
