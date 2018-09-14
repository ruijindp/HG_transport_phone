package com.hgkefang.transport.entity

/**
 * Create by admin on 2018/9/6
 */
data class EvenBusEven(
        val son: Son,
        var count: Int
)

data class OrderEvent(
        val retData: RetData,
        val position : Int
)
