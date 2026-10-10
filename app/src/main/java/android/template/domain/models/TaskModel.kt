package android.template.domain.models

data class TaskModel(
    val id: String,
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false
) {
    val titleForList: String get() = title.ifEmpty { description }
    val isActive: Boolean get() = !isCompleted
    val isEmpty: Boolean get() = title.isEmpty() || description.isEmpty()
}