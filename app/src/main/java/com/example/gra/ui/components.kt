package com.example.gra.ui  // 根据你的实际包名

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DateSelector(
    context: Context,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var openDatePicker by remember { mutableStateOf(false) }

    if (openDatePicker) {
        val today = LocalDate.now()
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (!newDate.isAfter(today)) {
                    onDateSelected(newDate)
                }
                openDatePicker = false
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    Text(
        text = "📅 ${selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
        modifier = Modifier.clickable { openDatePicker = true }
    )
}
