package android.template.core.extensions

import androidx.navigation3.runtime.NavKey

/** 堆疊頂端已是同一個 route 就不重複 push（防止連點造成重複畫面）。 */
internal fun MutableList<NavKey>.push(route: NavKey) {
    if (lastOrNull() != route) add(route)
}