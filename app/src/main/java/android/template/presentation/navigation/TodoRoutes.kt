package android.template.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object TasksRoute : NavKey


/** taskId == null 為新增；非 null 為編輯該 task。 */
@Serializable
data class AddEditTaskRoute(val taskId: String? = null) : NavKey