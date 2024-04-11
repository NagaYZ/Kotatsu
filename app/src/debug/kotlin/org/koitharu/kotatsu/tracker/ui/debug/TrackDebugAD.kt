package org.koitharu.kotatsu.tracker.ui.debug

import android.graphics.Color
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.drawableStart
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.source
import org.koitharu.kotatsu.databinding.ItemTrackDebugBinding
import org.koitharu.kotatsu.tracker.data.TrackEntity
import com.google.android.material.R as materialR

fun trackDebugAD(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	clickListener: OnListItemClickListener<TrackDebugItem>,
) = adapterDelegateViewBinding<TrackDebugItem, TrackDebugItem, ItemTrackDebugBinding>(
	{ layoutInflater, parent -> ItemTrackDebugBinding.inflate(layoutInflater, parent, false) },
) {
	val indicatorNew = ContextCompat.getDrawable(context, R.drawable.ic_new)

	itemView.setOnClickListener { v ->
		clickListener.onItemClick(item, v)
	}

	bind {
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.manga.coverUrl)?.run {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			source(item.manga.source)
			enqueueWith(coil)
		}
		binding.textViewTitle.text = item.manga.title
		binding.textViewSummary.text = buildSpannedString {
			item.lastCheckTime?.let {
				append(
					DateUtils.getRelativeDateTimeString(
						context,
						it.toEpochMilli(),
						DateUtils.MINUTE_IN_MILLIS,
						DateUtils.WEEK_IN_MILLIS,
						0,
					),
				)
			}
			if (item.lastResult == TrackEntity.RESULT_FAILED) {
				append(" - ")
				bold {
					color(context.getThemeColor(materialR.attr.colorError, Color.RED)) {
						append(getString(R.string.error))
					}
				}
			}
		}
		binding.textViewTitle.drawableStart = if (item.newChapters > 0) {
			indicatorNew
		} else {
			null
		}
	}
}
