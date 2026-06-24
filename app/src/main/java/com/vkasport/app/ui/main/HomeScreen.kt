package com.vkasport.app.ui.main


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vkasport.app.ui.components.VkaButton
import com.vkasport.app.ui.components.VkaCard
import com.vkasport.app.ui.components.SpaceMedium
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import androidx.compose.ui.unit.dp


@Composable
fun HomeScreen(

    viewModel: TrainingSessionViewModel,

    onStartTraining: () -> Unit

) {


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)

    ) {


        Text(
            text = "VKA SPORT"
        )


        Spacer(
            modifier =
                Modifier.height(SpaceMedium)
        )


        VkaCard {


            Text(
                text = "Последняя тренировка"
            )


            Text(
                text =
                    "Следи за прогрессом"
            )

        }


        Spacer(
            modifier =
                Modifier.height(SpaceMedium)
        )


        VkaButton(

            text = "НАЧАТЬ ТРЕНИРОВКУ",

            onClick = onStartTraining

        )

    }

}