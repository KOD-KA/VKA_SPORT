package com.vkasport.app.ui.components


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vkasport.app.navigation.BottomNavItem



@Composable
fun VkaBottomBar(

    items: List<BottomNavItem>,

    currentRoute: String?,

    onItemClick: (BottomNavItem) -> Unit

) {


    NavigationBar(


        containerColor =
            MaterialTheme.colorScheme.surface,


        tonalElevation = 0.dp

    ) {



        items.forEach { item ->



            val selected =
                currentRoute == item.route



            NavigationBarItem(


                selected = selected,


                onClick = {

                    onItemClick(item)

                },



                icon = {


                    Icon(

                        imageVector = item.icon,

                        contentDescription = item.title,

                        tint =

                            if (selected) {

                                MaterialTheme
                                    .colorScheme
                                    .primary

                            } else {

                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                            }

                    )


                },



                label = {


                    Text(

                        text = item.title,

                        style =
                            if (selected)

                                MaterialTheme
                                    .typography
                                    .bodyMedium

                            else

                                MaterialTheme
                                    .typography
                                    .bodySmall

                    )


                }

            )

        }


    }

}