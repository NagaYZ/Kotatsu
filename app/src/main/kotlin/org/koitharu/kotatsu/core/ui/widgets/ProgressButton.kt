package org.koitharu.kotatsu.core.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.withStyledAttributes
import androidx.core.widget.TextViewCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.getThemeColor
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.core.util.ext.setTextAndVisible
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import com.google.android.material.R as materialR

class ProgressButton @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : LinearLayoutCompat(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

	private val textViewTitle = TextView(context)
	private val textViewSubtitle = TextView(context)
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	private var progress = 0f
	private var colorBase = context.getThemeColor(materialR.attr.colorSecondaryContainer)
	private var colorProgress = context.getThemeColor(materialR.attr.colorPrimary)
	private var progressAnimator: ValueAnimator? = null

	var title: CharSequence?
		get() = textViewTitle.textAndVisible
		set(value) {
			textViewTitle.textAndVisible = value
		}

	var subtitle: CharSequence?
		get() = textViewSubtitle.textAndVisible
		set(value) {
			textViewSubtitle.textAndVisible = value
		}

	init {
		orientation = VERTICAL
		outlineProvider = OutlineProvider()
		clipToOutline = true

		context.withStyledAttributes(attrs, R.styleable.ProgressButton, defStyleAttr) {
			val textAppearanceFallback = androidx.appcompat.R.style.TextAppearance_AppCompat
			TextViewCompat.setTextAppearance(
				textViewTitle,
				getResourceId(R.styleable.ProgressButton_titleTextAppearance, textAppearanceFallback),
			)
			TextViewCompat.setTextAppearance(
				textViewSubtitle,
				getResourceId(R.styleable.ProgressButton_subtitleTextAppearance, textAppearanceFallback),
			)
			textViewTitle.text = getText(R.styleable.ProgressButton_title)
			textViewSubtitle.text = getText(R.styleable.ProgressButton_subtitle)
			colorBase = getColor(R.styleable.ProgressButton_baseColor, colorBase)
			colorProgress = getColor(R.styleable.ProgressButton_progressColor, colorProgress)
			progress = getInt(R.styleable.ProgressButton_android_progress, 0).toFloat() /
				getInt(R.styleable.ProgressButton_android_max, 100).toFloat()
		}

		addView(textViewTitle, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
		addView(
			textViewSubtitle,
			LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also { lp ->
				lp.topMargin = context.resources.resolveDp(2)
			},
		)

		paint.style = Paint.Style.FILL
		paint.color = colorProgress
		applyGravity()
		setWillNotDraw(false)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		canvas.drawColor(colorBase)
		canvas.drawRect(0f, 0f, width * progress, height.toFloat(), paint)
	}

	override fun setGravity(gravity: Int) {
		super.setGravity(gravity)
		if (childCount != 0) {
			applyGravity()
		}
	}

	override fun onAnimationUpdate(animation: ValueAnimator) {
		progress = animation.animatedValue as Float
		invalidate()
	}

	fun setTitle(@StringRes titleResId: Int) {
		textViewTitle.setTextAndVisible(titleResId)
	}

	fun setSubtitle(@StringRes titleResId: Int) {
		textViewSubtitle.setTextAndVisible(titleResId)
	}

	fun setProgress(value: Float, animate: Boolean) {
		progressAnimator?.cancel()
		if (animate) {
			progressAnimator = ValueAnimator.ofFloat(progress, value).apply {
				duration = context.getAnimationDuration(android.R.integer.config_shortAnimTime)
				interpolator = AccelerateDecelerateInterpolator()
				addUpdateListener(this@ProgressButton)
				start()
			}
		} else {
			progressAnimator = null
			progress = value
			invalidate()
		}
	}

	private fun applyGravity() {
		val value = (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) or Gravity.CENTER_VERTICAL
		textViewTitle.gravity = value
		textViewSubtitle.gravity = value
	}

	private class OutlineProvider : ViewOutlineProvider() {

		override fun getOutline(view: View, outline: Outline) {
			outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
		}
	}
}
