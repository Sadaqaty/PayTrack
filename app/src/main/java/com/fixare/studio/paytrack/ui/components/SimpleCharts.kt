package com.fixare.studio.paytrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.fixare.studio.paytrack.ui.theme.ErrorRed
import com.fixare.studio.paytrack.ui.theme.SuccessGreen

@Composable
fun IncomeExpenseBarChart(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val total = (income + expense).coerceAtLeast(1.0)
    val incomeHeightRatio = (income / total).toFloat()
    val expenseHeightRatio = (expense / total).toFloat()

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Income vs Expense (This Month)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Income Bar
            BarItem(
                value = income,
                ratio = incomeHeightRatio,
                color = SuccessGreen,
                label = "Income"
            )

            // Expense Bar
            BarItem(
                value = expense,
                ratio = expenseHeightRatio,
                color = ErrorRed,
                label = "Expense"
            )
        }
    }
}

@Composable
fun BarItem(
    value: Double,
    ratio: Float,
    color: Color,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(40.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barHeight = size.height * ratio
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, size.height - barHeight),
                    size = Size(size.width, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = "${value.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
