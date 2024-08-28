package org.koitharu.kotatsu.details.ui.pager.chapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ancestors
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseFragment
import org.koitharu.kotatsu.core.ui.list.ListSelectionController
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.util.PagerNestedScrollHelper
import org.koitharu.kotatsu.core.util.RecyclerViewScrollCallback
import org.koitharu.kotatsu.core.util.ext.dismissParentDialog
import org.koitharu.kotatsu.core.util.ext.findAppCompatDelegate
import org.koitharu.kotatsu.core.util.ext.findParentCallback
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.databinding.FragmentChaptersBinding
import org.koitharu.kotatsu.details.ui.DetailsViewModel
import org.koitharu.kotatsu.details.ui.adapter.ChaptersAdapter
import org.koitharu.kotatsu.details.ui.adapter.ChaptersSelectionDecoration
import org.koitharu.kotatsu.details.ui.model.ChapterListItem
import org.koitharu.kotatsu.details.ui.withVolumeHeaders
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.reader.ui.ReaderActivity.IntentBuilder
import org.koitharu.kotatsu.reader.ui.ReaderNavigationCallback
import org.koitharu.kotatsu.reader.ui.ReaderState
import kotlin.math.roundToInt

class ChaptersFragment :
	BaseFragment<FragmentChaptersBinding>(),
	OnListItemClickListener<ChapterListItem> {

	private val viewModel by activityViewModels<DetailsViewModel>()

	private var chaptersAdapter: ChaptersAdapter? = null
	private var selectionController: ListSelectionController? = null

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentChaptersBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentChaptersBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		chaptersAdapter = ChaptersAdapter(this)
		selectionController = ListSelectionController(
			appCompatDelegate = checkNotNull(findAppCompatDelegate()),
			decoration = ChaptersSelectionDecoration(binding.root.context),
			registryOwner = this,
			callback = ChaptersSelectionCallback(viewModel, binding.recyclerViewChapters),
		)
		viewModel.isChaptersInGridView.observe(viewLifecycleOwner) { chaptersInGridView ->
			binding.recyclerViewChapters.layoutManager = if (chaptersInGridView) {
				GridLayoutManager(context, ChapterGridSpanHelper.getSpanCount(binding.recyclerViewChapters)).apply {
					spanSizeLookup = ChapterGridSpanHelper.SpanSizeLookup(binding.recyclerViewChapters)
				}
			} else {
				LinearLayoutManager(context)
			}
		}
		with(binding.recyclerViewChapters) {
			addItemDecoration(TypedListSpacingDecoration(context, true))
			checkNotNull(selectionController).attachToRecyclerView(this)
			setHasFixedSize(true)
			PagerNestedScrollHelper(this).bind(viewLifecycleOwner)
			adapter = chaptersAdapter
			ChapterGridSpanHelper.attach(this)
		}
		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.chapters
			.map { it.withVolumeHeaders(requireContext()) }
			.flowOn(Dispatchers.Default)
			.observe(viewLifecycleOwner, this::onChaptersChanged)
		viewModel.isChaptersEmpty.observe(viewLifecycleOwner) {
			binding.textViewHolder.isVisible = it
		}
		viewModel.onSelectChapter.observeEvent(viewLifecycleOwner, ::onSelectChapter)
	}

	override fun onDestroyView() {
		chaptersAdapter = null
		selectionController = null
		super.onDestroyView()
	}

	override fun onItemClick(item: ChapterListItem, view: View) {
		if (selectionController?.onItemClick(item.chapter.id) == true) {
			return
		}
		val listener = findParentCallback(ReaderNavigationCallback::class.java)
		if (listener != null && listener.onChapterSelected(item.chapter)) {
			dismissParentDialog()
		} else {
			startActivity(
				IntentBuilder(view.context)
					.manga(viewModel.manga.value ?: return)
					.state(ReaderState(item.chapter.id, 0, 0))
					.build(),
			)
		}
	}

	override fun onItemLongClick(item: ChapterListItem, view: View): Boolean {
		return selectionController?.onItemLongClick(item.chapter.id) ?: false
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	private fun onChaptersChanged(list: List<ListModel>) {
		val adapter = chaptersAdapter ?: return
		if (adapter.itemCount == 0) {
			val position = list.indexOfFirst { it is ChapterListItem && it.isCurrent } - 1
			if (position > 0) {
				val offset = (resources.getDimensionPixelSize(R.dimen.chapter_list_item_height) * 0.6).roundToInt()
				adapter.setItems(
					list,
					RecyclerViewScrollCallback(requireViewBinding().recyclerViewChapters, position, offset),
				)
			} else {
				adapter.items = list
			}
		} else {
			adapter.items = list
		}
	}

	private suspend fun onSelectChapter(chapterId: Long) {
		if (!isResumed) {
			view?.ancestors?.firstNotNullOfOrNull { it as? ViewPager2 }?.setCurrentItem(0, true)
		}
		val position = withContext(Dispatchers.Default) {
			val predicate: (ListModel) -> Boolean = { x -> x is ChapterListItem && x.chapter.id == chapterId }
			val items = chaptersAdapter?.observeItems()?.firstOrNull { it.any(predicate) }
			items?.indexOfFirst(predicate) ?: -1
		}
		if (position >= 0) {
			selectionController?.onItemLongClick(chapterId)
			val lm = (viewBinding?.recyclerViewChapters?.layoutManager as? LinearLayoutManager)
			if (lm != null) {
				val offset = resources.getDimensionPixelOffset(R.dimen.chapter_list_item_height)
				lm.scrollToPositionWithOffset(position, offset)
			}
		}
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		requireViewBinding().progressBar.isVisible = isLoading
	}
}
