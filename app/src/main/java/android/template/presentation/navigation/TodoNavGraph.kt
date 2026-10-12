package android.template.presentation.navigation

import android.template.core.extensions.push
import android.template.presentation.addedittask.AddEditTaskScreen
import android.template.presentation.tasks.TasksScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

@Composable
fun TodoNavGraph(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(TasksRoute)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<TasksRoute> {
                TasksScreen(
                    onAddTask = { backStack.push(AddEditTaskRoute()) },
                    onEditTask = { taskId -> backStack.push(AddEditTaskRoute(taskId)) },
                )
            }
            entry<AddEditTaskRoute> { route ->
                AddEditTaskScreen(
                    taskId = route.taskId,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

