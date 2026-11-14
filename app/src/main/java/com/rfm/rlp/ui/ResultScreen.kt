package com.rfm.rlp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfm.rlp.analysis.AnalysisReport
import com.rfm.rlp.analysis.CheckResult
import com.rfm.rlp.analysis.LoadedImage
import com.rfm.rlp.analysis.Status
import com.rfm.rlp.ui.theme.RlpBlue
import com.rfm.rlp.ui.theme.RlpDarkBlue
import com.rfm.rlp.ui.theme.RlpLightBlue

@Composable
fun ResultScreen(
    image: LoadedImage?,
    report: AnalysisReport?,
    onBack: () -> Unit,
    onNewImage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        AppHeader(subtitle = "نتيجة الفحص", onBack = onBack)

        if (report == null) {
            Text(
                text = "لا توجد نتيجة لعرضها.",
                modifier = Modifier.padding(24.dp),
                color = RlpDarkBlue
            )
            return@Column
        }

        val grouped = report.results.groupBy { it.group }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { FinalCard(report, image) }

            item { SummaryRow(report) }

            grouped.forEach { (group, list) ->
                item(key = group) {
                    Text(
                        text = group,
                        color = RlpDarkBlue,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(list) { result -> CheckCard(result) }
            }

            item {
                Text(
                    text = "تنبيه: النتيجة استرشادية تعتمد على الذكاء الاصطناعي والرؤية الحاسوبية، وقد تختلف عن قرار الجهة المستلمة للصورة. " +
                            "الحالات المعلّمة بـ ⚠️ تحتاج إلى مراجعتك أنت للتأكد منها.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            item {
                Button(
                    onClick = onNewImage,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("🔁  فحص صورة أخرى", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FinalCard(report: AnalysisReport, image: LoadedImage?) {
    val color = report.finalStatus.color()
    val title = when (report.finalStatus) {
        Status.PASS -> "الصورة مطابقة"
        Status.FAIL -> "الصورة غير مطابقة"
        Status.REVIEW -> "الصورة تحتاج إلى مراجعة"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        border = BorderStroke(2.dp, color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (image != null) {
                val preview = remember(image) { image.bitmap.asImageBitmap() }
                Image(
                    bitmap = preview,
                    contentDescription = "الصورة المفحوصة",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("النتيجة النهائية", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "${report.finalStatus.symbol} $title",
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = report.finalMessage,
                    color = RlpDarkBlue,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(report: AnalysisReport) {
    val pass = report.results.count { it.status == Status.PASS }
    val review = report.results.count { it.status == Status.REVIEW }
    val fail = report.results.count { it.status == Status.FAIL }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RlpLightBlue, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("✅ مطابق: $pass", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Status.PASS.color())
        Text("⚠️ مراجعة: $review", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Status.REVIEW.color())
        Text("❌ غير مطابق: $fail", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Status.FAIL.color())
    }
}

@Composable
private fun CheckCard(result: CheckResult) {
    val color = result.status.color()
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, color.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(result.status.symbol, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = result.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = RlpDarkBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            StatusPill(result.status)

            Text(
                text = result.details,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = Color(0xFF263238)
            )

            if (result.suggestion != null) {
                Text(
                    text = "💡 الحل المقترح: ${result.suggestion}",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = RlpBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RlpLightBlue, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                )
            }
        }
    }
}
