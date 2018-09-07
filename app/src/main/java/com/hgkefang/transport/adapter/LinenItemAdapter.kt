package com.hgkefang.transport.adapter

import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.hgkefang.transport.R
import com.hgkefang.transport.entity.EvenBusEven
import com.hgkefang.transport.entity.Son
import org.greenrobot.eventbus.EventBus

/**
 * Create by admin on 2018/9/5
 */
class LinenItemAdapter(private val son: List<Son>) : RecyclerView.Adapter<LinenItemAdapter.ViewHolder>() {

    companion object {
        val addResult = ArrayList<EvenBusEven>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_linen_item, parent, false))
    }

    override fun getItemCount(): Int {
        return son.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        son[position].let {
            holder.tvLinenType.text = it.tradition_name
            holder.tvLinenSize.text = it.tradition_spec
            holder.etLinenCount.setOnFocusChangeListener { _, hasFocus ->
                holder.ivMinus.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
                holder.ivPlus.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
            }
            holder.etLinenCount.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    EventBus.getDefault().post(EvenBusEven(it, if (TextUtils.isEmpty(s.toString())) 0 else s.toString().toInt()))
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }
            })
            holder.ivMinus.setOnClickListener(OnClickListener(holder))
            holder.ivPlus.setOnClickListener(OnClickListener(holder))
        }
    }

    private inner class OnClickListener(private val holder: ViewHolder) : View.OnClickListener {

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.ivMinus -> {
                    if (TextUtils.isEmpty(holder.etLinenCount.text.toString()) || holder.etLinenCount.text.toString() == "0"){
                        return
                    }
                    holder.etLinenCount.setText((holder.etLinenCount.text.toString().toInt() - 1).toString())
                }
                R.id.ivPlus -> {
                    if (TextUtils.isEmpty(holder.etLinenCount.text.toString())){
                        holder.etLinenCount.setText((0 + 1).toString())
                        return
                    }
                    holder.etLinenCount.setText((holder.etLinenCount.text.toString().toInt() + 1).toString())
                }
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvLinenType = view.findViewById(R.id.tvLinenType) as TextView
        var tvLinenSize = view.findViewById(R.id.tvLinenSize) as TextView
        var etLinenCount = view.findViewById(R.id.etLinenCount) as EditText
        val ivMinus = view.findViewById(R.id.ivMinus) as ImageView
        val ivPlus = view.findViewById(R.id.ivPlus) as ImageView
    }
}