package org.koitharu.kotatsu.list.domain

import androidx.collection.ArraySet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koitharu.kotatsu.core.ui.widgets.ChipsView
import org.koitharu.kotatsu.list.ui.model.QuickFilter
import org.koitharu.kotatsu.parsers.util.SuspendLazy

abstract class MangaListQuickFilter : QuickFilterListener {

	private val appliedFilter = MutableStateFlow<Set<ListFilterOption>>(emptySet())
	private val availableFilterOptions = SuspendLazy {
		getAvailableFilterOptions()
	}

	val appliedOptions
		get() = appliedFilter.asStateFlow()

	override fun setFilterOption(option: ListFilterOption, isApplied: Boolean) {
		appliedFilter.value = ArraySet(appliedFilter.value).also {
			if (isApplied) {
				it.add(option)
			} else {
				it.remove(option)
			}
		}
	}

	override fun toggleFilterOption(option: ListFilterOption) {
		appliedFilter.value = ArraySet(appliedFilter.value).also {
			if (option in it) {
				it.remove(option)
			} else {
				it.add(option)
			}
		}
	}

	override fun clearFilter() {
		appliedFilter.value = emptySet()
	}

	suspend fun filterItem(
		selectedOptions: Set<ListFilterOption>,
	) = QuickFilter(
		items = availableFilterOptions.tryGet().getOrNull()?.map { option ->
			ChipsView.ChipModel(
				title = option.titleText,
				titleResId = option.titleResId,
				icon = option.iconResId,
				isCheckable = true,
				isChecked = option in selectedOptions,
				data = option,
			)
		}.orEmpty(),
	)

	protected abstract suspend fun getAvailableFilterOptions(): List<ListFilterOption>
}
