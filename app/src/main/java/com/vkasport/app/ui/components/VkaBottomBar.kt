package com.vkasport.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.navigation.BottomNavItem
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White

/**
 * Нижняя навигация в едином стиле приложения: белый фон, тонкий разделитель
 * сверху, у выбранной вкладки — чёрный кружок под иконкой с плавной
 * анимацией и жирная подпись. У остальных — просто серая иконка и подпись.
 */
@Composable
fun VkaBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(White)) {

        HorizontalDivider(color = SoftGray, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                BottomBarItem(
                    item = item,
                    selected = selected,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val circleColor by animateColorAsState(
        targetValue = if (selected) Black else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(200),
        label = "bottomBarCircle"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) White else DarkGray,
        animationSpec = tween(200),
        label = "bottomBarIcon"
    )
    val circleSize by animateDpAsState(
        targetValue = if (selected) 44.dp else 36.dp,
        animationSpec = tween(200),
        label = "bottomBarCircleSize"
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .background(circleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Black else DarkGray
        )
    }
}