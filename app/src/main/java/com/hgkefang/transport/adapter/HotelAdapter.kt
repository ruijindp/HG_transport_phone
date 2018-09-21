package com.hgkefang.transport.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import com.hgkefang.transport.R
import com.hgkefang.transport.entity.RetData

/**
 * Create by admin on 2018/9/7
 */
class HotelAdapter(private val results: List<RetData>) : BaseAdapter() {

    override fun getCount(): Int {
        return results.size
    }

    override fun getItem(position: Int): Any {
        return results[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view : View
        if (convertView == null) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_named_entity, parent, false)
            view.tag = ViewHolder(view)
        } else{
            view = convertView
        }
        val holder = view.tag as ViewHolder
        holder.tv.text = results[position].tradition_hotel_name
        return view
    }

    internal class ViewHolder(view: View) {
        var tv: TextView = view.findViewById(R.id.tvItem)
    }

}
