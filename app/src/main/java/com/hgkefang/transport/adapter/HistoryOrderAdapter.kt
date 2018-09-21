package com.hgkefang.transport.adapter

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bronze.kutil.widget.SimpleHolder
import com.hgkefang.transport.OrderDetailActivity
import com.hgkefang.transport.R
import com.hgkefang.transport.app.MyApplication
import com.hgkefang.transport.entity.RetData
import com.hgkefang.transport.util.TimeUtil
import kotlinx.android.synthetic.main.item_history_order.view.*

/**
 * Create by admin on 2018/9/4
 * 订单
 */
class HistoryOrderAdapter(private val pageValue: Int,
                          private var result: ArrayList<RetData>,
                          private var onSelectListener: OnSelectListener) : RecyclerView.Adapter<SimpleHolder>() {

    fun setNewData(result: ArrayList<RetData>) {
        this.result = result
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleHolder {
        return SimpleHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_history_order, parent, false))
    }

    override fun getItemCount(): Int {
        return result.size
    }

    override fun onBindViewHolder(holder: SimpleHolder, position: Int) {
        val retData = result[position]
        with(holder.itemView) {
            tvOrderTime.text = TimeUtil.strTime2Date(retData.tradition_addtime, "yyyy-MM-dd HH:mm:ss")
            tvOrderStatus.text = MyApplication.name
            if (pageValue == 1) {
                tvLeftValue.text = String.format("%s - %s", retData.tradition_hotel_name, retData.tradition_wash_name)
            } else {
                tvLeftValue.text = String.format("%s - %s", retData.tradition_wash_name, retData.tradition_hotel_name)
            }
            tvMiddleValue.text = retData.tradition_hotel_name
            var totalCount = 0
            if (retData.tradition_data.contains("|")) {
                retData.tradition_data.split("|").forEach {
                    totalCount += it.split("-")[1].toInt()
                }
            } else {
                totalCount = retData.tradition_data.split("-")[1].toInt()
            }
            tvOrderCount.text = totalCount.toString()
            rootView.setOnClickListener {
                val intent = Intent(it.context, OrderDetailActivity::class.java)
                intent.putExtra("retData", result[position])
                intent.putExtra("pageValue", pageValue)
                it.context.startActivity(intent)
            }
            tvPrinterOrder.setOnClickListener {
                onSelectListener.onPrinter(result[position], position)
            }
        }
    }

    interface OnSelectListener {
        fun onPrinter(retData: RetData, position: Int)
    }
}