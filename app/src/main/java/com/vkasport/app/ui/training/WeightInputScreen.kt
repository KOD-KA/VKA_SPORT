package com.vkasport.app.ui.training


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vkasport.app.ui.components.VkaButton
import com.vkasport.app.ui.components.VkaCard


@Composable
fun WeightInputScreen(

    onNext: (Float) -> Unit

) {


    var weight by remember {

        mutableStateOf("")

    }



    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),

        verticalArrangement =
            Arrangement.Center

    ) {



        VkaCard {



            Column(

                verticalArrangement =
                    Arrangement.spacedBy(12.dp),

                modifier =
                    Modifier.fillMaxWidth()

            ) {



                Text(

                    text = "Введите вес",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge

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



                VkaButton(

                    text = "ДАЛЕЕ",

                    onClick = {

                        weight.toFloatOrNull()?.let {

                            onNext(it)

                        }

                    }

                )

            }

        }

    }

}