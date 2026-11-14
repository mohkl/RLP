package com.rfm.rlp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rfm.rlp.analysis.LoadedImage
import com.rfm.rlp.ui.theme.RlpBlue
import com.rfm.rlp.ui.theme.RlpDarkBlue
import com.rfm.rlp.ui.theme.RlpLightBlue
import com.rfm.rlp.ui.theme.StatusFail
import java.util.Locale

@Composable
fun HomeScreen(
    image: LoadedImage?,
    isLoading: Boolean,
    isAnalyzing: Boolean,
    error: String?,
    onPickImage: (Uri) -> Unit,
    onStart: () -> Unit,
    onDeveloper: () -> Unit,
    onDismissError: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPickImage(uri)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        AppHeader(subtitle = "فاحص الصور الشخصية")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "اختر صورتك الشخصية من المعرض، وسيقوم التطبيق بفحصها وفق المعايير المطلوبة وعرض نتيجة كل معيار بشكل منفصل.",
                color = RlpDarkBlue,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            // معاينة الصورة
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(RlpLightBlue)
                    .border(2.dp, RlpBlue.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (image == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🖼️", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("لم يتم اختيار صورة بعد", color = RlpDarkBlue, fontSize = 15.sp)
                    }
                } else {
                    val preview = remember(image) { image.bitmap.asImageBitmap() }
                    Image(
                        bitmap = preview,
                        contentDescription = "معاينة الصورة",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(color = RlpBlue)
                }
            }

            if (image != null) {
                val kb = String.format(Locale.US, "%.1f", image.sizeBytes / 1024.0)
                Text(
                    text = "الأبعاد: ${image.width} × ${image.height} بكسل  •  الحجم: $kb كيلوبايت  •  الصيغة: ${image.format.label}",
                    color = RlpDarkBlue,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }

            if (error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusFail.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "❌ $error",
                        color = StatusFail,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismissError) { Text("إغلاق") }
                }
            }

            OutlinedButton(
                onClick = { picker.launch("image/*") },
                enabled = !isAnalyzing && !isLoading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("📷  اختيار صورة من المعرض", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onStart,
                enabled = image != null && !isAnalyzing && !isLoading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("جارٍ فحص الصورة…", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("🔍  بدء الفحص", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ملخص المعايير
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RlpLightBlue, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("المعايير التي يفحصها التطبيق", color = RlpDarkBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("• المقاس 600 × 600 بكسل، وصيغة JPEG، وحجم لا يتجاوز 240 كيلوبايت", fontSize = 13.sp, lineHeight = 20.sp)
                Text("• صورة ملونة وحديثة (لا تزيد عن 6 أشهر)", fontSize = 13.sp, lineHeight = 20.sp)
                Text("• النظر إلى الكاميرا بتعبير محايد دون ابتسامة", fontSize = 13.sp, lineHeight = 20.sp)
                Text("• الرأس يشغل من 50% إلى 69% من طول الصورة", fontSize = 13.sp, lineHeight = 20.sp)
                Text("• خلفية بيضاء سادة وإضاءة متوازنة دون ظلال", fontSize = 13.sp, lineHeight = 20.sp)
                Text("• لا نظارات ولا قبعات (الحجاب الديني مسموح) ولا فلاتر أو تعديلات", fontSize = 13.sp, lineHeight = 20.sp)
            }

            Text(
                text = "نصيحة: اختر الصورة من تطبيق المعرض أو الملفات مباشرة، فبعض التطبيقات (مثل واتساب) تضغط الصورة وتحذف بياناتها الوصفية.",
                color = Color.Gray,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            TextButton(
                onClick = onDeveloper,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("👨‍💻  عن المطور وطرق التواصل", color = RlpBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}
