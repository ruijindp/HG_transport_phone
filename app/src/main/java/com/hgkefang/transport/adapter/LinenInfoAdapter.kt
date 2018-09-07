package com.hgkefang.transport.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hgkefang.transport.R
import com.hgkefang.transport.entity.RetData

/**
 * Create by admin on 2018/9/5
 * 布草清单
 */
class LinenInfoAdapter(
        linenData: String,
        private val result: List<RetData>) : RecyclerView.Adapter<LinenInfoAdapter.ViewHolder>() {

    private var type: ArrayList<String> = ArrayList()

    init {
        if (linenData.contains("|")) {
            type = linenData.split("|").toList() as ArrayList<String>
        } else {
            type.add(linenData)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_linen_list, parent, false))
    }

    override fun getItemCount(): Int {
        return type.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvLinenCount.text = String.format("x%s", type[position].split("-")[1])
        for (retData in result) {
            retData.son.map {
                if (it.id == type[position].split("-")[0]){
                    holder.tvLinenName.text = String.format("%s%s", it.tradition_name, it.tradition_spec)
                }
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvLinenName = view.findViewById(R.id.tvLinenName) as TextView
        var tvLinenCount = view.findViewById(R.id.tvLinenCount) as TextView
    }
}