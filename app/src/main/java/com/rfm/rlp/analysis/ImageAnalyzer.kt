package com.rfm.rlp.analysis

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object ImageAnalyzer {

    suspend fun analyze(img: LoadedImage): AnalysisReport = withContext(Dispatchers.Default) {
        val bitmap = img.bitmap
        val grid = PixelGrid(bitmap)
        val input = InputImage.fromBitmap(bitmap, 0)

        // 1) عدّ الوجوه (دون الكنتورات حتى لا يقتصر الاكتشاف على وجه واحد)
        val countDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.05f)
                .build()
        )
        val allFaces = try {
            countDetector.process(input).await()
        } finally {
            countDetector.close()
        }

        // 2) تحليل تفصيلي للوجه الأبرز
        val mainDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build()
        )
        val mainFaces = try {
            mainDetector.process(input).await()
        } finally {
            mainDetector.close()
        }

        val face = mainFaces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
        val faceCount = maxOf(allFaces.size, mainFaces.size)
        val faceBox = face?.boundingBox

        // 3) قياسات الخلفية والإضاءة
        val bg = BackgroundChecks.measureBackground(grid, faceBox)
        val backgroundIsWhite = bg.samples > 0 && bg.whiteFraction >= 0.80f
        val light = faceBox?.let { BackgroundChecks.measureFace(grid, it) }

        // 4) تصنيف الصورة لاكتشاف النظارات والقبعات
        val labels = AccessoryChecks.detectLabels(bitmap, faceBox)

        val results = mutableListOf<CheckResult>()
        results += FormatChecks.run(img, grid, faceBox)
        results += FaceChecks.run(grid, faceCount, face, backgroundIsWhite)
        results += BackgroundChecks.run(bg, light)
        results += AccessoryChecks.run(img, labels)

        val fails = results.count { it.status == Status.FAIL }
        val reviews = results.count { it.status == Status.REVIEW }

        val finalStatus = when {
            fails > 0 -> Status.FAIL
            reviews > 0 -> Status.REVIEW
            else -> Status.PASS
        }

        val message = when (finalStatus) {
            Status.FAIL -> "الصورة غير مطابقة للمواصفات: $fails معيار غير مطابق" +
                    (if (reviews > 0) " و$reviews معيار يحتاج إلى مراجعة." else ".")
            Status.REVIEW -> "لا توجد مخالفات مؤكدة، لكن $reviews معيار يحتاج إلى مراجعة يدوية منك."
            Status.PASS -> "الصورة مطابقة لجميع المعايير التي تم فحصها."
        }

        AnalysisReport(results, finalStatus, message)
    }
}
