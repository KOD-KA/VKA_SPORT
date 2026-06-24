package com.vkasport.app.ui.training


import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vkasport.app.ui.components.VkaButton
import com.vkasport.app.ui.components.VkaCard
import androidx.compose.material3.MaterialTheme



@Composable
fun AddSetScreen(

    onAddSet: (Float, Int) -> Unit

) {


    var weight by remember {

        mutableStateOf("")

    }


    var reps by remember {

        mutableStateOf("")

    }



    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)

    ) {



        VkaCard {


            Text(

                text = "Добавление подхода",

                style =
                    MaterialTheme.typography.titleLarge

            )


            Spacer(
                modifier = Modifier.height(16.dp)
            )



            OutlinedTextField(

                value = weight,

                onValueChange = {

                    weight = it

                },

                label = {

                    Text("Вес (кг)")

                },

                modifier =
                    Modifier.fillMaxWidth()

            )



            Spacer(
                modifier = Modifier.height(12.dp)
            )



            OutlinedTextField(

                value = reps,

                onValueChange = {

                    reps = it

                },

                label = {

                    Text("Повторения")

                },

                modifier =
                    Modifier.fillMaxWidth()

            )



            Spacer(
                modifier = Modifier.height(20.dp)
            )



            VkaButton(

                text = "ДОБАВИТЬ ПОДХОД",

                onClick = {


                    val weightValue =
                        weight.toFloatOrNull()
                            ?: return@VkaButton



                    val repsValue =
                        reps.toIntOrNull()
                            ?: return@VkaButton



                    onAddSet(

                        weightValue,

                        repsValue

                    )


                    weight = ""

                    reps = ""

                }

            )

        }

    }

}