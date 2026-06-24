package com.vkasport.app.ui.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vkasport.app.viewmodel.ExerciseSetViewModel

@Composable
fun ExerciseSetsScreen(
    viewModel: ExerciseSetViewModel
) {

    val sets by viewModel.sets.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        LazyColumn {

            items(sets) { set ->

                SetItem(
                    set = set
                )
            }
        }
    }
}