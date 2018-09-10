package com.hgkefang.transport.adapter

import android.content.Intent
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hgkefang.transport.OrderDetailActivity
import com.hgkefang.transport.R
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.util.TimeUtil

/**
 * Create by admin on 2018/9/4
 * 订单
 */
class HistoryOrderAdapter(private val pageValue: Int,
                          private var result: ArrayList<RetData>,
                          private var onSelectListener: OnSelectListener) : RecyclerView.Adapter<HistoryOrderAdapter.ViewHolder>() {

    fun setNewData(result: ArrayList<RetData>) {
        this.result = result
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_history_order, parent, false))
    }

    override fun getItemCount(): Int {
        return result.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        result[position].let { it ->
            holder.tvOrderTime.text = TimeUtil.strTime2Date(it.tradition_addtime, "yyyy-MM-dd HH:mm:ss")
            holder.tvOrderStatus.text = MyApplication.name
            if (pageValue == 1) {
                holder.tvLeftValue.text = String.format("%s - %s", it.tradition_hotel_name, it.tradition_wash_name)
            } else {
                holder.tvLeftValue.text = String.format("%s - %s", it.tradition_wash_name, it.tradition_hotel_name)
            }
            var totalCount = 0
            if (it.tradition_data.contains("|")) {
                it.tradition_data.split("|").map {
                    totalCount += it.split("-")[1].toInt()
                }
            } else {
                totalCount = it.tradition_data.split("-")[1].toInt()
            }
            holder.tvOrderCount.text = totalCount.toString()
        }
        holder.rootView.setOnClickListener {
            val intent = Intent(it.context, OrderDetailActivity::class.java)
            intent.putExtra("retData", result[position])
            intent.putExtra("pageValue", pageValue)
            it.context.startActivity(intent)
        }
        holder.tvPrinterOrder.setOnClickListener {
            onSelectListener.onPrinter(result[position], position)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvOrderTime = view.findViewById(R.id.tvOrderTime) as TextView
        var tvOrderStatus = view.findViewById(R.id.tvOrderStatus) as TextView
        var tvLeftValue = view.findViewById(R.id.tvLeftValue) as TextView
        var tvMiddleValue = view.findViewById(R.id.tvMiddleValue) as TextView
        var tvOrderCount = view.findViewById(R.id.tvOrderCount) as TextView
        var tvPrinterOrder = view.findViewById(R.id.tvPrinterOrder) as TextView
        val rootView = view.findViewById(R.id.rootView) as CardView
    }


    interface OnSelectListener {
        fun onPrinter(retData: RetData, position: Int)
    }
}