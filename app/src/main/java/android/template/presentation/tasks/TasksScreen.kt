package android.template.presentation.tasks

import android.template.R
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Text(stringResource(R.string.add_task))
            }
        }
    ) { innerPadding ->
        Text(
            text = stringResource(R.string.tasks_title),
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
        )
    }
}