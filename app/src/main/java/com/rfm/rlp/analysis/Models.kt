package com.rfm.rlp.analysis

enum class Status(val symbol: String, val label: String) {
    PASS("✅", "مطابق"),
    FAIL("❌", "غير مطابق"),
    REVIEW("⚠️", "يحتاج إلى مراجعة")
}

data class CheckResult(
    val group: String,
    val title: String,
    val status: Status,
    val details: String,
    val suggestion: String? = null
)

data class AnalysisReport(
    val results: List<CheckResult>,
    val finalStatus: Status,
    val finalMessage: String
)

/** يعيد أسوأ حالة بين الحالات المعطاة: غير مطابق ثم يحتاج مراجعة ثم مطابق. */
fun worstOf(vararg statuses: Status): Status = when {
    statuses.any { it == Status.FAIL } -> Status.FAIL
    statuses.any { it == Status.REVIEW } -> Status.REVIEW
    else -> Status.PASS
}
