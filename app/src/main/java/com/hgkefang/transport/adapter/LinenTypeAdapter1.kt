package com.hgkefang.transport.adapter

import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.hgkefang.transport.R
import com.hgkefang.transport.entity.EvenBusEven
import com.hgkefang.transport.entity.RetData
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.imageResource

/**
 * Create by admin on 2018/9/26
 */
class LinenTypeAdapter1(private val retData: ArrayList<RetData>) : BaseExpandableListAdapter() {

    override fun getGroup(groupPosition: Int): Any {
        return retData[groupPosition]
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        var groupView: View? = convertView
        if (groupView == null) {
            groupView = LayoutInflater.from(parent?.context).inflate(R.layout.item_group, parent, false)
            groupView.tag = GroupHolder(groupView)
        }
        val groupHolder = groupView?.tag as GroupHolder
        groupHolder.tvGroupName.text = String.format("%s(x%s)", retData[groupPosition].tradition_name, retData[groupPosition].num)
        groupHolder.ivIndicator.imageResource = if (isExpanded) R.mipmap.ic_bottom else R.mipmap.ic_end
        return groupView
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return retData[groupPosition].son.size
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return retData[groupPosition].son[childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        var childView: View? = convertView
        if (childView == null) {
            childView = LayoutInflater.from(parent?.context).inflate(R.layout.item_linen_item, parent, false)
            childView.tag = ChildHolder(childView)
        }
        val childHolder = childView?.tag as ChildHolder
        val son = retData[groupPosition].son[childPosition]
        childHolder.tvLinenType.text = son.tradition_name
        childHolder.tvLinenSize.text = son.tradition_spec
        childHolder.etLinenCount.setText(if (son.count != 0) son.count.toString() else "")
        childHolder.etLinenCount.setOnFocusChangeListener { _, hasFocus ->
            childHolder.ivMinus.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
            childHolder.ivPlus.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
        }
        childHolder.etLinenCount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                EventBus.getDefault().post(EvenBusEven(son, if (TextUtils.isEmpty(s.toString())) 0 else s.toString().toInt()))
                son.count = if (TextUtils.isEmpty(s.toString())) 0 else s.toString().toInt()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
        childHolder.ivMinus.setOnClickListener(OnClickListener(childHolder))
        childHolder.ivPlus.setOnClickListener(OnClickListener(childHolder))
        return childView
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getGroupCount(): Int {
        return retData.size
    }

    private inner class OnClickListener(private val childHolder: ChildHolder) : View.OnClickListener {

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.ivMinus -> {
                    if (TextUtils.isEmpty(childHolder.etLinenCount.text.toString()) || childHolder.etLinenCount.text.toString() == "0") {
                        return
                    }
                    childHolder.etLinenCount.setText((childHolder.etLinenCount.text.toString().toInt() - 1).toString())
                }
                R.id.ivPlus -> {
                    if (TextUtils.isEmpty(childHolder.etLinenCount.text.toString())) {
                        childHolder.etLinenCount.setText((0 + 1).toString())
                        return
                    }
                    childHolder.etLinenCount.setText((childHolder.etLinenCount.text.toString().toInt() + 1).toString())
                }
            }
        }
    }

    inner class GroupHolder(view: View) {
        val tvGroupName = view.findViewById(R.id.tvLinenCategory) as TextView
        val ivIndicator = view.findViewById(R.id.ivIndicator) as ImageView
    }

    inner class ChildHolder(view: View) {
        val tvLinenType = view.findViewById(R.id.tvLinenType) as TextView
        val tvLinenSize = view.findViewById(R.id.tvLinenSize) as TextView
        val ivMinus = view.findViewById(R.id.ivMinus) as ImageView
        val ivPlus = view.findViewById(R.id.ivPlus) as ImageView
        val etLinenCount = view.findViewById(R.id.etLinenCount) as EditText
    }
}