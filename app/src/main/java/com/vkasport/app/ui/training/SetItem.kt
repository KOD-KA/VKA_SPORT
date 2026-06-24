package com.vkasport.app.ui.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vkasport.app.data.local.entity.ExerciseSetEntity

@Composable
fun SetItem(
    set: ExerciseSetEntity
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = "Подход №${set.setNumber}"
            )

            Text(
                text = "Вес: ${set.weight} кг"
            )

            Text(
                text = "Повторения: ${set.reps}"
            )
        }
    }
}