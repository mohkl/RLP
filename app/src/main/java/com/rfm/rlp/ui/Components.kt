package com.rfm.rlp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rfm.rlp.R
import com.rfm.rlp.analysis.Status
import com.rfm.rlp.ui.theme.RlpBlue
import com.rfm.rlp.ui.theme.StatusFail
import com.rfm.rlp.ui.theme.StatusPass
import com.rfm.rlp.ui.theme.StatusReview

fun Status.color(): Color = when (this) {
    Status.PASS -> StatusPass
    Status.FAIL -> StatusFail
    Status.REVIEW -> StatusReview
}

@Composable
fun RlpLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Image(
        painter = painterResource(id = R.drawable.ic_logo),
        contentDescription = "شعار RLP",
        modifier = modifier.size(size)
    )
}

@Composable
fun AppHeader(
    subtitle: String,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RlpBlue)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RlpLogo(size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "RLP",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(text = subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text("→ رجوع", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusPill(status: Status, modifier: Modifier = Modifier) {
    Text(
        text = "${status.symbol} ${status.label}",
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(status.color(), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
