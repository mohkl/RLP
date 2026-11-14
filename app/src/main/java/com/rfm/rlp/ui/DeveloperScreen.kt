package com.rfm.rlp.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfm.rlp.ui.theme.RlpBlue
import com.rfm.rlp.ui.theme.RlpDarkBlue
import com.rfm.rlp.ui.theme.RlpLightBlue

private const val DEVELOPER_NAME = "rFM"
private const val WHATSAPP_NUMBER = "201027814323"
private const val WHATSAPP_DISPLAY = "+201027814323"
private const val INSTAGRAM_URL = "https://www.instagram.com/r5____f?psln=c2w4N2V0NTBjdzBs"

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "لا يوجد تطبيق قادر على فتح هذا الرابط", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun DeveloperScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        AppHeader(subtitle = "عن المطور", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            RlpLogo(size = 110.dp)

            Text(
                text = "تطبيق RLP",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RlpDarkBlue
            )
            Text(
                text = "فاحص الصور الشخصية بالذكاء الاصطناعي",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RlpLightBlue, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("اسم المطور", fontSize = 13.sp, color = Color.Gray)
                Text(DEVELOPER_NAME, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = RlpBlue)
                Text("واتساب: $WHATSAPP_DISPLAY", fontSize = 14.sp, color = RlpDarkBlue)
            }

            Button(
                onClick = {
                    val text = Uri.encode("مرحباً، أتواصل معك بخصوص تطبيق RLP")
                    openLink(context, "https://wa.me/$WHATSAPP_NUMBER?text=$text")
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("💬  تواصل عبر واتساب", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { openLink(context, INSTAGRAM_URL) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("📸  حسابي على إنستجرام", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
