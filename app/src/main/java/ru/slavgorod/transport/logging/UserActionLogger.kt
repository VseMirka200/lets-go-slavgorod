package ru.slavgorod.transport.logging

import androidx.annotation.StringRes
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import timber.log.Timber

internal object UserActionLogger {

    private const val TAG = "UserAction"

    fun screenOpened(screenName: String) {
        log(R.string.log_screen_opened, screenName)
    }

    fun screenOpened(@StringRes screenNameResId: Int, vararg args: Any) {
        log(R.string.log_screen_opened, AppText.get(screenNameResId, *args))
    }

    fun action(action: String) {
        log(R.string.log_action, action)
    }

    fun action(@StringRes actionResId: Int, vararg args: Any) {
        log(R.string.log_action, AppText.get(actionResId, *args))
    }

    fun menuOpened(menuName: String) {
        log(R.string.log_menu_opened, menuName)
    }

    fun preferenceChanged(name: String, value: String) {
        log(R.string.log_preference_changed, name, value)
    }

    fun preferenceChanged(@StringRes nameResId: Int, value: String) {
        log(R.string.log_preference_changed, AppText.get(nameResId), value)
    }

    fun settingsEntryOpened(entryName: String) {
        log(R.string.log_settings_entry_opened, entryName)
    }

    fun routeOpened(routeId: String, routeNumber: String? = null) {
        val routeInfo = routeNumber?.let { "$it ($routeId)" } ?: routeId
        log(R.string.log_route_opened, routeInfo)
    }

    fun routeSearchChanged(query: String) {
        val label =
            query.trim().takeIf(String::isNotBlank) ?: AppText.get(R.string.log_search_cleared)
        log(R.string.log_route_search_changed, label)
    }

    fun routePinned(routeId: String, pinned: Boolean) {
        log(
            R.string.log_route_state,
            routeId,
            if (pinned) AppText.get(R.string.log_route_pinned) else AppText.get(R.string.log_route_unpinned)
        )
    }

    fun routeRefreshRequested(source: String) {
        log(R.string.log_refresh_requested, source)
    }

    fun filtersOpened(routeName: String) {
        log(R.string.log_filters_opened, routeName)
    }

    fun filterSelected(filterName: String, value: String) {
        log(R.string.log_filter_selected, filterName, value)
    }

    fun filterCleared(filterName: String) {
        log(R.string.log_filter_cleared, filterName)
    }

    fun scheduleSectionToggled(sectionName: String, collapsed: Boolean) {
        log(R.string.log_schedule_section_toggled, sectionName, stateLabel(!collapsed))
    }

    fun scheduleUpdateResult(result: String) {
        log(R.string.log_schedule_update_result, result)
    }

    fun scheduleUpdateResult(@StringRes resultResId: Int, vararg args: Any) {
        log(R.string.log_schedule_update_result, AppText.get(resultResId, *args))
    }

    fun scheduleCacheSaved(routeCount: Int) {
        log(R.string.log_schedule_cache_saved, routeCount)
    }

    fun scheduleCacheLoaded(savedAtLabel: String? = null) {
        if (savedAtLabel.isNullOrBlank()) {
            log(R.string.log_schedule_cache_loaded)
        } else {
            log(R.string.log_schedule_cache_loaded_with_date, savedAtLabel)
        }
    }

    fun logExportResult(success: Boolean) {
        log(
            R.string.log_export_result,
            if (success) AppText.get(R.string.log_state_saved) else AppText.get(R.string.log_state_failed)
        )
    }

    private fun log(@StringRes templateResId: Int, vararg args: Any) {
        Timber.tag(TAG).i(AppText.get(templateResId, *args))
    }

    private fun stateLabel(expanded: Boolean): String {
        return if (expanded) AppText.get(R.string.log_state_expanded) else AppText.get(R.string.log_state_collapsed)
    }
}
