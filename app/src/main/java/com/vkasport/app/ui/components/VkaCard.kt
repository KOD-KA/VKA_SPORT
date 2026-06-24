package com.vkasport.app.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun VkaCard(

    modifier: Modifier = Modifier,

    content: @Composable ColumnScope.() -> Unit

) {

    Column(

        modifier = modifier
            .fillMaxWidth()

            // расстояние между карточками
            .padding(vertical = 4.dp)

            .background(

                color = MaterialTheme.colorScheme.surface,

                shape = RoundedCornerShape(12.dp)

            )

            // содержимое внутри карточки
            .padding(12.dp),

        content = content

    )

}