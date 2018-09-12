package com.hgkefang.transport.util

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.support.annotation.ColorRes
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Create by admin on 2018/9/11
 */
class PageLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class State {
        EMPTY_TYPE, ERROR_TYPE, CONTENT_TYPE, LOADING_TYPE
    }

    private var loadingView: View? = null
    private var emptyView: View? = null
    private var errorView: View? = null
    private var contentView: View? = null
    private var currentState = State.CONTENT_TYPE

    private fun showView(type: State){
        if (Looper.myLooper() == Looper.getMainLooper()){
            changeView(type)
        } else{
            post { changeView(type) }
        }
    }

    private fun changeView(type: State) {
        currentState = type
        loadingView?.visibility = if (type == State.LOADING_TYPE) View.VISIBLE else View.GONE
        contentView?.visibility = if (type == State.CONTENT_TYPE) View.VISIBLE else View.GONE
        errorView?.visibility = if (type == State.ERROR_TYPE) View.VISIBLE else View.GONE
        emptyView?.visibility = if (type == State.EMPTY_TYPE) View.VISIBLE else View.GONE
    }

    fun showLoadingView() {
        showView(State.LOADING_TYPE)
    }

    fun showErrorView() {
        showView(State.ERROR_TYPE)
    }

    fun showEmptyView() {
        showView(State.EMPTY_TYPE)
    }

    fun showContentView() {
        showView(State.CONTENT_TYPE)
    }

    class Builder(private var context: Context) {
        private var pageLayout: PageLayout = PageLayout(context)
        private var inflater: LayoutInflater = LayoutInflater.from(context)
        private lateinit var tvEmpty: TextView
        private lateinit var tvError: TextView
        private lateinit var tvLoading: TextView
        private var onRetryClickListener: OnRetryClickListener? = null

        private fun initDefault() {
            if (pageLayout.emptyView == null) {
                setDefaultEmpty()
            }
            if (pageLayout.errorView == null) {
                setDefaultError()
            }
            if (pageLayout.loadingView == null) {
                setDefaultLoading()
            }
        }

        private fun setDefaultLoading() {

        }

        private fun setDefaultError() {

        }

        private fun setDefaultEmpty() {

        }

        fun setLoadingView(@LayoutRes loadingLayout: Int): Builder {
            inflater.inflate(loadingLayout, pageLayout, false).apply {
                pageLayout.loadingView = this
                pageLayout.addView(this)
            }
            return this
        }

        fun setErrorView(@LayoutRes errorLayout: Int,
                         @IdRes errorId: Int,
                         onRetryClickListener: OnRetryClickListener): Builder {
            inflater.inflate(errorLayout, pageLayout, false).apply {
                pageLayout.errorView = this
                pageLayout.addView(this)
                tvError = findViewById(errorId)
                tvError.setOnClickListener { onRetryClickListener.onRetry() }
            }
            return this
        }

        fun setErrorView(errorView: View): Builder {
            pageLayout.errorView = errorView
            pageLayout.addView(errorView)
            return this
        }

        fun setEmptyView(@LayoutRes emptyLayout: Int, @IdRes emptyId: Int): Builder {
            inflater.inflate(emptyLayout, pageLayout, false).apply {
                tvEmpty = findViewById(emptyId)
                pageLayout.emptyView = this
                pageLayout.addView(this)
            }
            return this
        }

        fun setDefaultLoadingText(text: String): Builder {
            tvLoading.text = text
            return this
        }

        fun setDefaultEmptyText(text: String): Builder {
            tvEmpty.text = text
            return this
        }

        fun setDefaultErrorText(text: String): Builder {
            tvError.text = text
            return this
        }

        fun setDefaultErrorTextColor(@ColorRes color: Int):Builder {
            tvError.setTextColor(ContextCompat.getColor(context, color))
            return this
        }

        fun setDefaultLoadingTextColor(@ColorRes color: Int): Builder {
            tvLoading.setTextColor(ContextCompat.getColor(context, color))
            return this
        }

        fun setEmptyDrawable(resId: Int): Builder {
            setTopDrawables(tvEmpty, resId)
            return this
        }

        fun setErrorDrawable(resId: Int): Builder {
            setTopDrawables(tvError, resId)
            return this
        }

        private fun setTopDrawables(textView: TextView, resId: Int) {
            if (resId == 0) {
                textView.setCompoundDrawables(null, null, null, null)
            }
            val drawable = context.resources.getDrawable(resId)
            drawable.setBounds(0, 0, drawable.minimumWidth, drawable.minimumHeight)
            textView.setCompoundDrawables(null, drawable, null, null)
            textView.compoundDrawablePadding = 20
        }

        fun initPage(targetView: Any): Builder {
            var content: ViewGroup? = null
            when (targetView) {
                is Activity -> {
                    context = targetView
                    content = (context as Activity).findViewById(android.R.id.content)
                }
                is Fragment -> {
                    context = targetView.activity!!
                    content = targetView.view?.parent as ViewGroup
                }
                is View -> {
                    context = targetView.context
                    content = targetView.parent as ViewGroup
                }
            }
            val childCount = content?.childCount
            var index = 0
            val oldContent: View
            if (targetView is View) {
                oldContent = targetView
                childCount?.let {
                    for (i in 0 until childCount) {
                        if (content!!.getChildAt(i) === oldContent) {
                            index = i
                            break
                        }
                    }
                }
            } else {
                oldContent = content!!.getChildAt(0)
            }
            pageLayout.contentView = oldContent
            pageLayout.removeAllViews()
            content?.removeView(oldContent)
            val layoutParams = oldContent.layoutParams
            content?.addView(pageLayout, index, layoutParams)
            pageLayout.addView(oldContent)
            initDefault()
            return this
        }

        fun setOnRetryListener(onRetryClickListener: OnRetryClickListener): Builder {
            this.onRetryClickListener = onRetryClickListener
            return this
        }

        fun create() = pageLayout
    }

    interface OnRetryClickListener {
        fun onRetry()
    }
}