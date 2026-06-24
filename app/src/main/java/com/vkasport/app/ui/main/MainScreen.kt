package com.vkasport.app.ui.main


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vkasport.app.navigation.AppNavigation
import com.vkasport.app.navigation.BottomNavItem
import com.vkasport.app.ui.components.VkaBottomBar
import com.vkasport.app.viewmodel.WorkoutViewModel



@Composable
fun MainScreen(

    viewModel: WorkoutViewModel

) {


    val navController =
        rememberNavController()



    val items =
        listOf(

            BottomNavItem.Training,

            BottomNavItem.Records,

            BottomNavItem.Calendar,

            BottomNavItem.Info

        )



    Scaffold(


        containerColor =
            MaterialTheme.colorScheme.background,


        bottomBar = {



            val navBackStackEntry by

            navController
                .currentBackStackEntryAsState()



            val currentRoute =

                navBackStackEntry
                    ?.destination
                    ?.route



            VkaBottomBar(

                items = items,

                currentRoute = currentRoute

            ) { item ->



                navController.navigate(

                    item.route

                ) {


                    popUpTo(

                        navController.graph
                            .startDestinationId

                    )


                    launchSingleTop = true

                }

            }


        }


    ) { padding ->



        AppNavigation(

            viewModel = viewModel,

            navController = navController,

            modifier =
                Modifier.padding(padding)

        )

    }

}