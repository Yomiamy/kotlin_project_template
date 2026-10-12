package android.template.presentation.addedittask

import android.template.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun AddEditTaskScreen(
    taskId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text(stringResource(if (taskId == null) R.string.add_task else R.string.edit_task))
            Button(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
        }
    }
}