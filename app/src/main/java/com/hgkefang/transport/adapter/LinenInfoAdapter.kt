package com.hgkefang.transport.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bronze.kutil.widget.SimpleHolder
import com.hgkefang.transport.R
import com.hgkefang.transport.entity.RetData
import kotlinx.android.synthetic.main.item_linen_list.view.*

/**
 * Create by admin on 2018/9/5
 * 布草清单
 */
class LinenInfoAdapter(
        linenData: String,
        private val result: List<RetData>) : RecyclerView.Adapter<SimpleHolder>() {

    private var type: ArrayList<String> = ArrayList()

    init {
        if (linenData.contains("|")) {
            type = linenData.split("|").toList() as ArrayList<String>
        } else {
            type.add(linenData)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleHolder {
        return SimpleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_linen_list, parent, false))
    }

    override fun getItemCount(): Int {
        return type.size
    }

    override fun onBindViewHolder(holder: SimpleHolder, position: Int) {
        with(holder.itemView){
            tvLinenCount.text = String.format("x%s", type[position].split("-")[1])
            for (retData in result) {
                retData.son.map {
                    if (it.id == type[position].split("-")[0]){
                        tvLinenName.text = String.format("%s-%s", it.tradition_name, it.tradition_spec)
                    }
                }
            }
        }
    }
}