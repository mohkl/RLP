package com.rfm.rlp.analysis

import android.graphics.Rect
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class BgStats(
    val samples: Int,
    val whiteFraction: Float,
    val darkFraction: Float,
    val leftLuminance: Float,
    val rightLuminance: Float,
    val meanLuminance: Float
)

data class FaceLight(
    val meanLuminance: Float,
    val leftLuminance: Float,
    val rightLuminance: Float,
    val brightFraction: Float,
    val darkFraction: Float
)

object BackgroundChecks {

    private const val GROUP = "ثالثاً: الخلفية والإضاءة"

    /** يقيس الخلفية من الشريط العلوي والجانبين العلويين خارج منطقة الوجه. */
    fun measureBackground(grid: PixelGrid, face: Rect?): BgStats {
        val w = grid.width
        val h = grid.height
        val excluded = face?.let {
            Rect(
                it.left - it.width() / 4,
                it.top - it.height() / 4,
                it.right + it.width() / 4,
                it.bottom + it.height() / 4
            )
        }

        var n = 0
        var white = 0
        var dark = 0
        var sumAll = 0f
        var sumLeft = 0f
        var nLeft = 0
        var sumRight = 0f
        var nRight = 0

        val maxY = (h * 0.45f).toInt()
        var y = 0
        while (y < maxY) {
            var x = 0
            while (x < w) {
                val inBand = y < h * 0.06f || x < w * 0.12f || x >= w * 0.88f
                if (inBand && (excluded == null || !excluded.contains(x, y))) {
                    val r = grid.red(x, y)
                    val g = grid.green(x, y)
                    val b = grid.blue(x, y)
                    val mn = min(r, min(g, b))
                    val mx = max(r, max(g, b))
                    val lum = grid.luminance(x, y)
                    n++
                    if (mn >= 228 && mx - mn <= 14) white++
                    if (lum < 212f) dark++
                    sumAll += lum
                    if (x < w / 2) {
                        sumLeft += lum
                        nLeft++
                    } else {
                        sumRight += lum
                        nRight++
                    }
                }
                x += 2
            }
            y += 2
        }

        if (n == 0) return BgStats(0, 0f, 0f, 0f, 0f, 0f)
        return BgStats(
            samples = n,
            whiteFraction = white.toFloat() / n,
            darkFraction = dark.toFloat() / n,
            leftLuminance = if (nLeft == 0) sumAll / n else sumLeft / nLeft,
            rightLuminance = if (nRight == 0) sumAll / n else sumRight / nRight,
            meanLuminance = sumAll / n
        )
    }

    /** يقيس الإضاءة على الجزء الأوسط من الوجه. */
    fun measureFace(grid: PixelGrid, face: Rect): FaceLight? {
        val left = (face.left + face.width() * 0.15f).toInt().coerceIn(0, grid.width - 1)
        val right = (face.right - face.width() * 0.15f).toInt().coerceIn(0, grid.width)
        val top = (face.top + face.height() * 0.2f).toInt().coerceIn(0, grid.height - 1)
        val bottom = (face.bottom - face.height() * 0.1f).toInt().coerceIn(0, grid.height)
        if (right - left < 4 || bottom - top < 4) return null

        val mid = (left + right) / 2
        var n = 0
        var sum = 0f
        var sumL = 0f
        var nL = 0
        var sumR = 0f
        var nR = 0
        var bright = 0
        var dark = 0

        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val lum = grid.luminance(x, y)
                n++
                sum += lum
                if (lum > 245f) bright++
                if (lum < 45f) dark++
                if (x < mid) {
                    sumL += lum
                    nL++
                } else {
                    sumR += lum
                    nR++
                }
                x += 2
            }
            y += 2
        }
        if (n < 20 || nL == 0 || nR == 0) return null
        return FaceLight(
            meanLuminance = sum / n,
            leftLuminance = sumL / nL,
            rightLuminance = sumR / nR,
            brightFraction = bright.toFloat() / n,
            darkFraction = dark.toFloat() / n
        )
    }

    fun run(bg: BgStats, light: FaceLight?): List<CheckResult> = listOf(
        whiteCheck(bg),
        lightingCheck(light),
        shadowCheck(bg, light)
    )

    private fun pct(value: Float): Int = (value * 100).toInt()

    private fun whiteCheck(bg: BgStats): CheckResult {
        val title = "خلفية بيضاء سادة"
        if (bg.samples == 0) {
            return CheckResult(
                GROUP, title, Status.REVIEW,
                "تعذّر أخذ عينات كافية من الخلفية.",
                "تأكد بنفسك أن الخلفية بيضاء سادة."
            )
        }
        val fix = "ضع الشخص أمام حائط أبيض أو ورقة بيضاء كبيرة دون أي رسومات أو أجسام، ثم التقط الصورة مجدداً."
        return when {
            bg.whiteFraction >= 0.95f && bg.meanLuminance >= 238f -> CheckResult(
                GROUP, title, Status.PASS,
                "الخلفية بيضاء سادة (نسبة اللون الأبيض ${pct(bg.whiteFraction)}%).",
                null
            )
            bg.whiteFraction >= 0.80f -> CheckResult(
                GROUP, title, Status.REVIEW,
                "الخلفية قريبة من الأبيض لكن بها تدرجات أو بقع (نسبة اللون الأبيض ${pct(bg.whiteFraction)}%).",
                fix
            )
            else -> CheckResult(
                GROUP, title, Status.FAIL,
                "الخلفية ليست بيضاء سادة (نسبة اللون الأبيض ${pct(bg.whiteFraction)}% فقط).",
                fix
            )
        }
    }

    private fun lightingCheck(light: FaceLight?): CheckResult {
        val title = "إضاءة متوازنة"
        if (light == null) {
            return CheckResult(
                GROUP, title, Status.REVIEW,
                "تعذّر تقييم الإضاءة لأنه لم يتم اكتشاف وجه.",
                "تأكد بنفسك أن الإضاءة متوازنة وأن الوجه غير مظلم أو لامع."
            )
        }
        val mean = light.meanLuminance
        val meanText = String.format(Locale.US, "%.0f", mean)
        val fix = "التقط الصورة في ضوء النهار أو بإضاءة ناعمة أمام الوجه، وتجنب الضوء القوي المباشر والأماكن المعتمة."
        return when {
            mean < 60f || mean > 225f || light.brightFraction > 0.25f || light.darkFraction > 0.30f ->
                CheckResult(
                    GROUP, title, Status.FAIL,
                    "الإضاءة غير متوازنة: متوسط سطوع الوجه $meanText من 255، " +
                            "نسبة المناطق شديدة السطوع ${pct(light.brightFraction)}% والمظلمة ${pct(light.darkFraction)}%.",
                    fix
                )
            mean in 90f..200f && light.brightFraction < 0.08f && light.darkFraction < 0.10f ->
                CheckResult(
                    GROUP, title, Status.PASS,
                    "الإضاءة متوازنة (متوسط سطوع الوجه $meanText من 255).",
                    null
                )
            else -> CheckResult(
                GROUP, title, Status.REVIEW,
                "الإضاءة مقبولة لكنها قريبة من الحدود (متوسط سطوع الوجه $meanText من 255).",
                fix
            )
        }
    }

    private fun shadowCheck(bg: BgStats, light: FaceLight?): CheckResult {
        val title = "عدم وجود ظلال على الوجه أو الخلفية"

        val faceStatus: Status
        val faceNote: String
        if (light == null) {
            faceStatus = Status.REVIEW
            faceNote = "تعذّر تقييم ظلال الوجه لأنه لم يتم اكتشاف وجه."
        } else {
            val hi = max(light.leftLuminance, light.rightLuminance)
            val lo = min(light.leftLuminance, light.rightLuminance)
            val ratio = if (hi <= 0f) 1f else lo / hi
            faceStatus = when {
                ratio >= 0.85f -> Status.PASS
                ratio >= 0.72f -> Status.REVIEW
                else -> Status.FAIL
            }
            faceNote = when (faceStatus) {
                Status.PASS -> "جانبا الوجه متساويان في الإضاءة (نسبة التوازن ${pct(ratio)}%)، ولا توجد مؤشرات على ظلال."
                else -> "يوجد فرق في إضاءة جانبي الوجه (نسبة التوازن ${pct(ratio)}%)، وهذا يدل على وجود ظل أو إضاءة جانبية."
            }
        }

        val bgStatus: Status
        val bgNote: String
        if (bg.samples == 0 || bg.whiteFraction < 0.80f) {
            bgStatus = Status.REVIEW
            bgNote = "تعذّر تقييم ظلال الخلفية لأن الخلفية ليست بيضاء سادة."
        } else {
            val diff = abs(bg.leftLuminance - bg.rightLuminance)
            bgStatus = when {
                bg.darkFraction <= 0.02f && diff <= 8f -> Status.PASS
                bg.darkFraction <= 0.08f && diff <= 16f -> Status.REVIEW
                else -> Status.FAIL
            }
            bgNote = when (bgStatus) {
                Status.PASS -> "لم تُرصد ظلال على الخلفية."
                else -> "رُصدت مناطق أغمق في الخلفية (نسبتها ${pct(bg.darkFraction)}%)، وقد تكون ظلالاً أو تدرجات إضاءة."
            }
        }

        val status = worstOf(faceStatus, bgStatus)
        return CheckResult(
            group = GROUP,
            title = title,
            status = status,
            details = "$faceNote\n$bgNote",
            suggestion = if (status == Status.PASS) null else
                "اجعل مصدر الضوء أمام الوجه مباشرة (مثل نافذة)، وابتعد عن الحائط بمسافة نصف متر تقريباً لتجنب ظل الرأس على الخلفية."
        )
    }
}
