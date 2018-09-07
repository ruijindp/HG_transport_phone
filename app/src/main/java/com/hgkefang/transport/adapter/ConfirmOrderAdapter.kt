package com.hgkefang.transport.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hgkefang.transport.R
import com.hgkefang.transport.entity.EvenBusEven

/**
 * Create by admin on 2018/9/5
 * 确认订单
 */
class ConfirmOrderAdapter(private val result: ArrayList<EvenBusEven>) : RecyclerView.Adapter<ConfirmOrderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_confirm_order, parent, false))
    }

    override fun getItemCount(): Int {
        return result.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        result[position].let {
            holder.tvLinenCount.text = String.format("x%s", it.count)
            holder.tvLinenType.text = String.format("%s%s", it.son.tradition_name, it.son.tradition_spec)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvLinenType = view.findViewById(R.id.tvLinenType) as TextView
        var tvLinenCount = view.findViewById(R.id.tvLinenCount) as TextView
    }
}