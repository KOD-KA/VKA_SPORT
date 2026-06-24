package com.vkasport.app.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.ui.theme.*


@Composable
fun VkaButton(

    text: String,

    modifier: Modifier = Modifier,

    enabled: Boolean = true,

    onClick: () -> Unit

) {


    Box(

        modifier = modifier
            .height(44.dp)
            .widthIn(
                min = 220.dp,
                max = 320.dp
            )
            .background(
                color = Black,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled){
                onClick()
            }
            .padding(horizontal = 24.dp),

        contentAlignment = Alignment.Center

    ){


        Text(

            text = text,

            color = White,

            fontSize = 14.sp,

            fontWeight = FontWeight.Medium

        )

    }

}