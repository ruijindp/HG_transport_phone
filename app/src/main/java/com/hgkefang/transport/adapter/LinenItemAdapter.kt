package com.hgkefang.transport.adapter

import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bronze.kutil.widget.SimpleHolder
import com.hgkefang.transport.R
import com.hgkefang.transport.entity.EvenBusEven
import com.hgkefang.transport.entity.Son
import kotlinx.android.synthetic.main.item_linen_item.view.*
import org.greenrobot.eventbus.EventBus

/**
 * Create by admin on 2018/9/5
 */
class LinenItemAdapter(private val son: List<Son>) : RecyclerView.Adapter<SimpleHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleHolder {
        return SimpleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_linen_item, parent, false))
    }

    override fun getItemCount(): Int {
        return son.size
    }

    override fun onBindViewHolder(holder: SimpleHolder, position: Int) {
        val result = son[position]
        with(holder.itemView) {
            tvLinenType.text = result.tradition_name
            tvLinenSize.text = result.tradition_spec
            etLinenCount.setText(if (result.count != 0) result.count.toString() else "")
            etLinenCount.setOnFocusChangeListener { _, hasFocus ->
                ivMinus.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
                ivPlus.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
            }
            etLinenCount.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    EventBus.getDefault().post(EvenBusEven(result, if (TextUtils.isEmpty(s.toString())) 0 else s.toString().toInt()))
                    result.count = if (TextUtils.isEmpty(s.toString())) 0 else s.toString().toInt()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }
            })
            ivMinus.setOnClickListener(OnClickListener(this))
            ivPlus.setOnClickListener(OnClickListener(this))
        }
    }

    private inner class OnClickListener(private val itemView: View) : View.OnClickListener {

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.ivMinus -> {
                    if (TextUtils.isEmpty(itemView.etLinenCount.text.toString()) || itemView.etLinenCount.text.toString() == "0") {
                        return
                    }
                    itemView.etLinenCount.setText((itemView.etLinenCount.text.toString().toInt() - 1).toString())
                }
                R.id.ivPlus -> {
                    if (TextUtils.isEmpty(itemView.etLinenCount.text.toString())) {
                        itemView.etLinenCount.setText((0 + 1).toString())
                        return
                    }
                    itemView.etLinenCount.setText((itemView.etLinenCount.text.toString().toInt() + 1).toString())
                }
            }
        }
    }
}