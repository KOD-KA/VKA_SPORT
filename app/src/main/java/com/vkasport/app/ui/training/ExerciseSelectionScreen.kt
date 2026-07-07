package com.vkasport.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.model.ExerciseLibrary
import com.vkasport.app.data.model.MuscleGroup
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.Gold
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White

@Composable
fun ExerciseSelectionScreen(
    muscleGroup: MuscleGroup,
    alreadyAdded: List<String> = emptyList(),
    customExercises: List<String> = emptyList(),
    lastGroupExercises: List<String> = emptyList(),
    onBack: () -> Unit,
    onRepeatLastGroup: () -> Unit,
    onAddCustomExercise: (String) -> Unit,
    onExerciseSelected: (String) -> Unit
) {
    val libraryExercises = ExerciseLibrary.exercises.filter { it.muscleGroup == muscleGroup }
    var addingCustom by remember(muscleGroup) { mutableStateOf(false) }
    var customText by remember(muscleGroup) { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {

        // ===== ШАПКА С КНОПКОЙ НАЗАД =====
        Column(
            modifier = Modifier.fillMaxWidth().background(Black).padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("ВЫБЕРИТЕ УПРАЖНЕНИЯ", color = White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(muscleGroup.title, color = White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        }

        // ===== СПИСОК УПРАЖНЕНИЙ =====
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Повторить как в прошлый раз ────────────────────────────
            if (lastGroupExercises.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SoftGray, RoundedCornerShape(14.dp))
                            .clickable { onRepeatLastGroup() }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("↻", color = Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Повторить как в прошлый раз",
                                color = Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = lastGroupExercises.joinToString(", "),
                            fontSize = 12.sp,
                            color = DarkGray
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Black, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Добавить все (${lastGroupExercises.size}) →",
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(2.dp)) }
            }

            items(libraryExercises) { exercise ->
                val isAdded = exercise.name in alreadyAdded
                ExerciseListItem(name = exercise.name, isAdded = isAdded, isCustom = false) {
                    onExerciseSelected(exercise.name)
                }
            }

            // ── Свои упражнения ──────────────────────────────────────
            if (customExercises.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "СВОИ УПРАЖНЕНИЯ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(customExercises) { name ->
                    val isAdded = name in alreadyAdded
                    ExerciseListItem(name = name, isAdded = isAdded, isCustom = true) {
                        onExerciseSelected(name)
                    }
                }
            }

            // ── Добавить своё упражнение ─────────────────────────────
            item {
                if (!addingCustom) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Black, RoundedCornerShape(12.dp))
                            .clickable { addingCustom = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Text("Добавить своё упражнение", color = White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(12.dp)).padding(14.dp)
                    ) {
                        Text("Название упражнения", fontSize = 12.sp, color = DarkGray, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { customText = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = {
                                        if (customText.isNotBlank()) {
                                            onAddCustomExercise(customText.trim())
                                            customText = ""
                                            addingCustom = false
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Black, unfocusedBorderColor = DarkGray.copy(alpha = 0.3f),
                                    cursorColor = Black, focusedTextColor = Black, unfocusedTextColor = Black
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (customText.isNotBlank()) Black else DarkGray.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = customText.isNotBlank()) {
                                        onAddCustomExercise(customText.trim())
                                        customText = ""
                                        addingCustom = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Отмена", fontSize = 12.sp, color = DarkGray,
                            modifier = Modifier.clickable { addingCustom = false; customText = "" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseListItem(
    name: String,
    isAdded: Boolean,
    isCustom: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = if (isAdded) SoftGray else White, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, color = Black, fontSize = 15.sp, fontWeight = if (isAdded) FontWeight.Medium else FontWeight.Normal)
            if (isCustom) {
                Spacer(Modifier.width(6.dp))
                Text("★", color = Gold, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        if (isAdded) {
            Box(
                modifier = Modifier.size(28.dp).background(Black, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) { Text("✓", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        } else {
            Box(
                modifier = Modifier.size(28.dp).background(SoftGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) { Text("+", color = DarkGray, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
        }
    }
}