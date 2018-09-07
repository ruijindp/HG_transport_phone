package com.hgkefang.transport.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.hgkefang.transport.fragment.HistoryOrderFragment

/**
 * Create by admin on 2018/9/4
 */


class OrderPageAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> HistoryOrderFragment.getInstance(0)
            1 -> HistoryOrderFragment.getInstance(1)
            2 -> HistoryOrderFragment.getInstance(2)
            3 -> HistoryOrderFragment.getInstance(3)
            4 -> HistoryOrderFragment.getInstance(4)
            else -> {
                HistoryOrderFragment.getInstance(0)
            }
        }
    }

    override fun getCount(): Int {
        return 5
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "全部"
            1 -> "送净"
            2 -> "收脏"
            3 -> "重污"
            4 -> "返洗"
            else -> ""
        }
    }
}
