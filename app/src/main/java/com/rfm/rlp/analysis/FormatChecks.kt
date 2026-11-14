package com.rfm.rlp.analysis

import android.graphics.Rect
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object FormatChecks {

    private const val GROUP = "أولاً: المقاس والصيغة"
    private const val MAX_BYTES = 240 * 1024

    fun run(img: LoadedImage, grid: PixelGrid, faceBox: Rect?): List<CheckResult> = listOf(
        dimensionCheck(img),
        formatCheck(img),
        sizeCheck(img),
        colorCheck(grid, faceBox),
        dateCheck(img)
    )

    private fun dimensionCheck(img: LoadedImage): CheckResult {
        val ok = img.width == 600 && img.height == 600
        return CheckResult(
            group = GROUP,
            title = "أبعاد الصورة (600 × 600 بكسل)",
            status = if (ok) Status.PASS else Status.FAIL,
            details = "أبعاد الصورة الحالية: ${img.width} × ${img.height} بكسل.",
            suggestion = if (ok) null else
                "اقصّ الصورة لتصبح مربعة ثم غيّر مقاسها إلى 600 × 600 بكسل بأي تطبيق تعديل أو موقع تغيير مقاس، دون تمديد الصورة أو تشويهها."
        )
    }

    private fun formatCheck(img: LoadedImage): CheckResult {
        val ok = img.format == ImageFormat.JPEG
        return CheckResult(
            group = GROUP,
            title = "صيغة الصورة (JPEG)",
            status = if (ok) Status.PASS else Status.FAIL,
            details = "صيغة الملف المكتشفة من محتواه: ${img.format.label}.",
            suggestion = if (ok) null else
                "حوّل الصورة إلى صيغة JPEG (امتداد jpg أو jpeg) عبر أي تطبيق أو موقع تحويل صور."
        )
    }

    private fun sizeCheck(img: LoadedImage): CheckResult {
        val kb = img.sizeBytes / 1024.0
        val ok = img.sizeBytes <= MAX_BYTES
        return CheckResult(
            group = GROUP,
            title = "حجم الملف (لا يتجاوز 240 كيلوبايت)",
            status = if (ok) Status.PASS else Status.FAIL,
            details = "حجم الملف الحالي: ${String.format(Locale.US, "%.1f", kb)} كيلوبايت.",
            suggestion = if (ok) null else
                "اضغط الصورة أو قلّل جودة JPEG تدريجياً حتى يصبح حجمها أقل من 240 كيلوبايت."
        )
    }

    private fun colorCheck(grid: PixelGrid, faceBox: Rect?): CheckResult {
        val title = "صورة ملونة"
        if (faceBox == null) {
            return CheckResult(
                GROUP, title, Status.REVIEW,
                "تعذّر قياس الألوان على الوجه لأنه لم يتم اكتشاف وجه.",
                "تأكد بنفسك أن الصورة ملونة وليست بالأبيض والأسود."
            )
        }
        val chroma = meanChroma(grid, faceBox)
        val chromaText = String.format(Locale.US, "%.1f", chroma)
        return when {
            chroma < 6f -> CheckResult(
                GROUP, title, Status.FAIL,
                "ألوان الوجه شبه معدومة (قيمة التشبّع $chromaText)، والصورة تبدو بالأبيض والأسود.",
                "استخدم صورة ملونة أصلية دون فلتر أبيض وأسود."
            )
            chroma < 14f -> CheckResult(
                GROUP, title, Status.REVIEW,
                "ألوان الوجه باهتة (قيمة التشبّع $chromaText)، وقد تكون الصورة ضعيفة الألوان.",
                "تأكد أن الصورة ملونة بألوان طبيعية ودون فلاتر."
            )
            else -> CheckResult(
                GROUP, title, Status.PASS,
                "الصورة ملونة (قيمة التشبّع $chromaText).",
                null
            )
        }
    }

    private fun meanChroma(grid: PixelGrid, area: Rect): Float {
        val l = max(0, area.left)
        val t = max(0, area.top)
        val r = min(grid.width, area.right)
        val b = min(grid.height, area.bottom)
        var sum = 0L
        var n = 0
        var y = t
        while (y < b) {
            var x = l
            while (x < r) {
                val rr = grid.red(x, y)
                val gg = grid.green(x, y)
                val bb = grid.blue(x, y)
                sum += max(rr, max(gg, bb)) - min(rr, min(gg, bb))
                n++
                x += 2
            }
            y += 2
        }
        return if (n == 0) 0f else sum.toFloat() / n
    }

    private fun dateCheck(img: LoadedImage): CheckResult {
        val title = "حداثة الصورة (لا تزيد عن 6 أشهر)"
        val millis = img.captureDateMillis
            ?: return CheckResult(
                GROUP, title, Status.REVIEW,
                "لا يوجد تاريخ التقاط داخل بيانات الصورة (ربما أُزيلت عند الإرسال أو المشاركة)، لذلك لا يمكن التأكد من حداثتها.",
                "تأكد بنفسك أن الصورة التُقطت خلال آخر 6 أشهر، أو التقط صورة جديدة."
            )

        val now = System.currentTimeMillis()
        val dateText = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(millis))
        val limit = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
        val source = if (img.dateIsModification)
            "تاريخ آخر تعديل للملف: $dateText (وهو ليس بالضرورة تاريخ الالتقاط)."
        else
            "تاريخ الالتقاط المسجّل في بيانات الصورة: $dateText."
        val caveat = " هذه المعلومة مأخوذة من بيانات الصورة الوصفية ويمكن تعديلها أو حذفها."

        return when {
            millis > now + 24L * 60 * 60 * 1000 -> CheckResult(
                GROUP, title, Status.REVIEW,
                "$source التاريخ في المستقبل، وهذا غير منطقي.$caveat",
                "تحقق من ضبط تاريخ الكاميرا أو التقط صورة جديدة."
            )
            millis < limit -> CheckResult(
                GROUP, title, Status.FAIL,
                "$source أقدم من 6 أشهر.$caveat",
                "التقط صورة حديثة خلال آخر 6 أشهر."
            )
            img.dateIsModification -> CheckResult(
                GROUP, title, Status.REVIEW,
                "$source لا يمكن الجزم بتاريخ الالتقاط الفعلي.$caveat",
                "تأكد بنفسك أن الصورة الأصلية حديثة."
            )
            else -> CheckResult(
                GROUP, title, Status.PASS,
                "$source الصورة حديثة وفق هذه البيانات.$caveat",
                null
            )
        }
    }
}
