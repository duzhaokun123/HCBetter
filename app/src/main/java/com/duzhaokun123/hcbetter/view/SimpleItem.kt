package com.duzhaokun123.hcbetter.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.duzhaokun123.hcbetter.R
import com.duzhaokun123.hcbetter.XposedInit.Companion.moduleRes
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit

class SimpleItem @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        LayoutInflater.from(context).inflate(moduleRes.getLayout(R.layout.view_simple_itme), this)
    }
    private val tvTitle = findViewWithTag<TextView>("tv_title")
    var title: CharSequence
        get() = tvTitle.text
        set(value) {
            tvTitle.text = value
        }

    private val tvDesc = findViewWithTag<TextView>("tv_desc")
    var desc: CharSequence
        get() = tvDesc.text
        set(value) {
            tvDesc.text = value
        }
}