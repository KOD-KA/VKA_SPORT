package com.vkasport.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.vkasport.app.navigation.AppNavigation
import com.vkasport.app.navigation.BottomNavItem
import com.vkasport.app.ui.components.VkaBottomBar
import com.vkasport.app.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: WorkoutViewModel
) {

    val items = listOf(
        BottomNavItem.Training,
        BottomNavItem.Records,
        BottomNavItem.Calendar,
        BottomNavItem.Info
    )

    // ИЗМЕНЕНО: раньше здесь был NavController с 4 route. Теперь состояние
    // текущей вкладки хранит PagerState — он же используется и для свайпа,
    // и для подсветки нижней навигации, так что оба способа переключения
    // (тап и свайп) всегда синхронизированы между собой.
    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            VkaBottomBar(
                items = items,
                selectedIndex = pagerState.currentPage
            ) { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            }
        }
    ) { padding ->

        AppNavigation(
            viewModel = viewModel,
            pagerState = pagerState,
            modifier = Modifier.padding(padding)
        )
    }
}