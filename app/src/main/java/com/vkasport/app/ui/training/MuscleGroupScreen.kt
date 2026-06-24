package com.vkasport.app.ui.training


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.vkasport.app.data.model.MuscleGroup
import com.vkasport.app.ui.theme.*


@Composable
fun MuscleGroupScreen(

    onGroupSelected: (MuscleGroup) -> Unit

) {


    LazyVerticalGrid(

        columns = GridCells.Fixed(2),

        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        horizontalArrangement = Arrangement.spacedBy(12.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)

    ) {


        items(
            MuscleGroup.entries
        ) { group ->


            Box(

                modifier = Modifier
                    .height(90.dp)

                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(12.dp)
                    )

                    .background(
                        color = SurfaceLight,
                        shape = RoundedCornerShape(12.dp)
                    )

                    .clickable {

                        onGroupSelected(group)

                    },

                contentAlignment = Alignment.Center

            ){


                Text(

                    text = group.title,

                    fontSize = 16.sp,

                    fontWeight = FontWeight.Medium,

                    color = Black

                )


            }

        }


    }


}