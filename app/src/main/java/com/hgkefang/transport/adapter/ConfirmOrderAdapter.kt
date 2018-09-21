package com.hgkefang.transport.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bronze.kutil.widget.SimpleHolder
import com.hgkefang.transport.R
import com.hgkefang.transport.entity.EvenBusEven
import kotlinx.android.synthetic.main.item_confirm_order.view.*

/**
 * Create by admin on 2018/9/5
 * 确认订单
 */
class ConfirmOrderAdapter(private val result: ArrayList<EvenBusEven>) : RecyclerView.Adapter<SimpleHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleHolder {
        return SimpleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_confirm_order, parent, false))
    }

    override fun getItemCount(): Int {
        return result.size
    }

    override fun onBindViewHolder(holder: SimpleHolder, position: Int) {
        val retData = result[position]
        with(holder.itemView) {
            tvLinenCount.text = String.format("x%s", retData.count)
            tvLinenType.text = String.format("%s-%s", retData.son.tradition_name, retData.son.tradition_spec)
        }
    }
}