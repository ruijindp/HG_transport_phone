package com.hgkefang.transport.view

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.PopupWindow
import com.hgkefang.transport.R
import com.hgkefang.transport.adapter.HotelAdapter
import com.hgkefang.transport.entity.RetData
import java.util.*

class NamedEntityPopup private constructor(private val parent: ViewGroup) : PopupWindow(parent.context), AdapterView.OnItemClickListener {

    private val lvNamedEntities: ListView
    var lastAnchor: View? = null

    private var entityPopupSelectListener: EntityPopupSelectListener? = null

    init {
        if (density == 0f) {
            density = this.parent.resources.displayMetrics.density
        }
        val rootView = LayoutInflater.from(this.parent.context)
                .inflate(R.layout.popup_named_entity, this.parent, false)
        lvNamedEntities = rootView.findViewById(R.id.lvNamedEntities)
        lvNamedEntities.onItemClickListener = this
        isOutsideTouchable = true
        isFocusable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        height = ViewGroup.LayoutParams.WRAP_CONTENT//lv margin is 8dp
        contentView = rootView
    }

    override fun showAsDropDown(anchor: View) {
        width = (anchor.width + 16 * density).toInt()
        lastAnchor = anchor
        super.showAsDropDown(anchor, (-8 * density).toInt(), (-8 * density).toInt())
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        dismiss()
        if (entityPopupSelectListener != null) {
            entityPopupSelectListener!!.selectStringAt(this, position)
        }
    }

    fun setEntityPopupSelectListener(EntityPopupSelectListener: EntityPopupSelectListener) {
        this.entityPopupSelectListener = EntityPopupSelectListener
    }

    fun setEntities(entities: ArrayList<RetData>) {
        val strings = ArrayList<String>()
        for (e in entities) {
            strings.add(e.tradition_hotel_name)
        }
//        lvNamedEntities.adapter = ArrayAdapter(parent.context, R.layout.item_named_entity, strings)
        lvNamedEntities.adapter = HotelAdapter(entities)
        val height = ((strings.size * 40 + 24) * density).toInt()//lv margin is 8dp
        setHeight(if (height > 320 * density) (320 * density).toInt() else height)
    }

    interface EntityPopupSelectListener {
        fun selectStringAt(popup: NamedEntityPopup, index: Int)
    }

    companion object {
        fun create(parent: ViewGroup): NamedEntityPopup {
            return NamedEntityPopup(parent)
        }

        private var density: Float = 0.toFloat()
    }
}
