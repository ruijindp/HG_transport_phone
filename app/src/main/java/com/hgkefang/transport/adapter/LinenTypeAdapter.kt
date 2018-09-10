package com.hgkefang.transport.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hgkefang.transport.R
import com.hgkefang.transport.entity.RetData

/**
 * Create by admin on 2018/9/4
 */
class LinenTypeAdapter(private val retData: List<RetData>) : RecyclerView.Adapter<LinenTypeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_linen, parent, false))
    }

    override fun getItemCount(): Int {
        return retData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        retData[position].let {
            holder.tvLinenCategory.text = it.tradition_name
            holder.rvItemContent.post {
                holder.rvItemContent.adapter = LinenItemAdapter(it.son)
            }
        }
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvLinenCategory = view.findViewById(R.id.tvLinenCategory) as TextView
        val rvItemContent = view.findViewById(R.id.rvItemContent) as RecyclerView
    }
}