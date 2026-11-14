package com.rfm.rlp.analysis

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object AccessoryChecks {

    private const val GROUP = "رابعاً: الممنوعات"

    private val glassesNames = setOf("glasses", "sunglasses", "goggles", "eyewear")

    private val hatNames = setOf(
        "hat", "cap", "fedora", "sun hat", "cowboy hat", "beanie",
        "baseball cap", "helmet", "hard hat", "visor"
    )

    private val editingSoftwareKeywords = listOf(
        "photoshop", "lightroom", "gimp", "snapseed", "facetune", "picsart",
        "canva", "meitu", "b612", "snow", "vsco", "retouch", "airbrush",
        "youcam", "beautyplus", "beauty", "pixlr", "fotor", "afterlight",
        "capcut", "remini", "faceapp"
    )

    /** يعيد null عند فشل التصنيف. */
    suspend fun detectLabels(bitmap: Bitmap, faceBox: Rect?): List<ImageLabel>? {
        return try {
            val crop = cropAround(bitmap, faceBox)
            val labeler = ImageLabeling.getClient(
                ImageLabelerOptions.Builder().setConfidenceThreshold(0.4f).build()
            )
            try {
                labeler.process(InputImage.fromBitmap(crop, 0)).await()
            } finally {
                labeler.close()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cropAround(bitmap: Bitmap, box: Rect?): Bitmap {
        if (box == null) return bitmap
        val padX = (box.width() * 0.6f).toInt()
        val padTop = (box.height() * 0.9f).toInt()
        val padBottom = (box.height() * 0.4f).toInt()
        val l = max(0, box.left - padX)
        val t = max(0, box.top - padTop)
        val r = min(bitmap.width, box.right + padX)
        val b = min(bitmap.height, box.bottom + padBottom)
        if (r - l < 32 || b - t < 32) return bitmap
        return Bitmap.createBitmap(bitmap, l, t, r - l, b - t)
    }

    fun run(img: LoadedImage, labels: List<ImageLabel>?): List<CheckResult> = listOf(
        glassesCheck(labels),
        hatCheck(labels),
        editingCheck(img)
    )

    private fun confidenceOf(labels: List<ImageLabel>, names: Set<String>): Float =
        labels.filter { it.text.lowercase(Locale.US) in names }
            .maxOfOrNull { it.confidence } ?: 0f

    private fun glassesCheck(labels: List<ImageLabel>?): CheckResult {
        val title = "عدم وجود نظارات"
        val fix = "اخلع النظارة (الطبية أو الشمسية) والتقط الصورة مجدداً."
        if (labels == null) {
            return CheckResult(
                GROUP, title, Status.REVIEW,
                "تعذّر تشغيل فحص النظارات على هذا الجهاز.",
                "تأكد بنفسك أن الصورة خالية من النظارات."
            )
        }
        val c = confidenceOf(labels, glassesNames)
        val p = (c * 100).toInt()
        return when {
            c >= 0.80f -> CheckResult(
                GROUP, title, Status.FAIL,
                "رُصدت نظارات على الأرجح (درجة الثقة $p%).",
                fix
            )
            c >= 0.50f -> CheckResult(
                GROUP, title, Status.REVIEW,
                "قد توجد نظارات في الصورة (درجة الثقة $p%).",
                "تأكد بنفسك؛ وإن كانت هناك نظارات فاخلعها والتقط الصورة مجدداً."
            )
            else -> CheckResult(
                GROUP, title, Status.PASS,
                "لم تُرصد نظارات. هذا الفحص تقديري، وقد لا تُكتشف النظارات الشفافة الرفيعة.",
                null
            )
        }
    }

    private fun hatCheck(labels: List<ImageLabel>?): CheckResult {
        val title = "عدم وجود قبعة (الحجاب الديني مسموح)"
        val fix = "اخلع القبعة أو الغطاء غير الديني. الحجاب الديني مسموح بشرط أن يظهر الوجه كاملاً."
        if (labels == null) {
            return CheckResult(
                GROUP, title, Status.REVIEW,
                "تعذّر تشغيل فحص القبعات على هذا الجهاز.",
                "تأكد بنفسك أن الصورة خالية من القبعات."
            )
        }
        val c = confidenceOf(labels, hatNames)
        val p = (c * 100).toInt()
        return when {
            c >= 0.85f -> CheckResult(
                GROUP, title, Status.FAIL,
                "رُصدت قبعة على الأرجح (درجة الثقة $p%).",
                fix
            )
            c >= 0.50f -> CheckResult(
                GROUP, title, Status.REVIEW,
                "قد يوجد غطاء للرأس في الصورة (درجة الثقة $p%). التطبيق لا يميّز دائماً بين القبعة والحجاب الديني.",
                "إن كان غطاء الرأس حجاباً دينياً فهو مسموح، وإن كان قبعة فاخلعها."
            )
            else -> CheckResult(
                GROUP, title, Status.PASS,
                "لم تُرصد قبعة. هذا الفحص تقديري.",
                null
            )
        }
    }

    private fun editingCheck(img: LoadedImage): CheckResult {
        val title = "عدم وجود فلاتر أو تعديلات تجميلية أو فوتوشوب"
        val limit = "لا يستطيع أي تطبيق إثبات سلامة الصورة من التعديل بشكل قاطع."
        val software = img.software

        if (!software.isNullOrBlank()) {
            val lower = software.lowercase(Locale.US)
            if (editingSoftwareKeywords.any { lower.contains(it) }) {
                return CheckResult(
                    GROUP, title, Status.REVIEW,
                    "وُجد اسم برنامج في بيانات الصورة: «$software»، وقد يدل على أنها عُدّلت. هذا مؤشر وليس دليلاً قاطعاً.",
                    "استخدم الصورة الأصلية من الكاميرا دون فلاتر أو تعديل."
                )
            }
        }

        if (!img.hasMetadata) {
            return CheckResult(
                GROUP, title, Status.REVIEW,
                "لا توجد بيانات وصفية في الصورة (ربما أُزيلت عند الإرسال أو الحفظ أو التعديل)، لذلك لا يمكن الحكم على التعديل. $limit",
                "استخدم الصورة الأصلية المباشرة من الكاميرا دون فلاتر أو تعديلات تجميلية."
            )
        }

        return CheckResult(
            GROUP, title, Status.PASS,
            "لم تُرصد مؤشرات تعديل في بيانات الصورة. $limit والفلاتر التجميلية داخل كاميرا الهاتف قد لا تظهر في البيانات.",
            null
        )
    }
}
