package com.hgkefang.transport.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bronze.kutil.widget.SimpleHolder
import com.hgkefang.transport.R
import com.hgkefang.transport.entity.RetData
import kotlinx.android.synthetic.main.item_linen.view.*

/**
 * Create by admin on 2018/9/4
 */
class LinenTypeAdapter(private val retData: List<RetData>) : RecyclerView.Adapter<SimpleHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleHolder {
        return SimpleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_linen, parent, false))
    }

    override fun getItemCount(): Int {
        return retData.size
    }

    override fun onBindViewHolder(holder: SimpleHolder, position: Int) {
        val result = retData[position]
        with(holder.itemView) {
            tvLinenCategory.text = String.format("%s(x%s)", result.tradition_name, result.num)
            rvItemContent.post { rvItemContent.adapter = LinenItemAdapter(result.son) }
        }
    }
}