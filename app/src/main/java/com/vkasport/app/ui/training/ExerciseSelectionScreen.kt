package com.vkasport.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.model.ExerciseLibrary
import com.vkasport.app.data.model.MuscleGroup
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White

@Composable
fun ExerciseSelectionScreen(
    muscleGroup: MuscleGroup,
    alreadyAdded: List<String> = emptyList(),
    onExerciseSelected: (String) -> Unit
) {
    val exercises = ExerciseLibrary.exercises.filter { it.muscleGroup == muscleGroup }

    Column(modifier = Modifier.fillMaxSize()) {

        // ===== ШАПКА =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Black)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "ВЫБЕРИТЕ УПРАЖНЕНИЯ",
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = muscleGroup.title,
                color = White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }

        // ===== СПИСОК УПРАЖНЕНИЙ =====
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(exercises) { exercise ->
                val isAdded = exercise.name in alreadyAdded
                ExerciseListItem(
                    name = exercise.name,
                    isAdded = isAdded,
                    onClick = { onExerciseSelected(exercise.name) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseListItem(
    name: String,
    isAdded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isAdded) SoftGray else White,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = if (isAdded) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(12.dp))

        if (isAdded) {
            // Уже добавлено — зелёная галочка
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Black, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Добавить
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(SoftGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = DarkGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}