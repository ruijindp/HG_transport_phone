package com.hgkefang.transport.entity

import java.io.Serializable

data class ObjectResult(
        val message: String,
        val errMsg: ErrMsg,
        val retData: RetData
)

data class CommonResult(
        val message: String,
        val errMsg: ErrMsg,
        val retData: ArrayList<RetData>
)

data class ErrMsg(
        val code: Int,
        val resource: String

)

data class RetData(
        val token: String,
        val time: String,
        val name: String,

        val id: String,
        val tradition_name: String,
        val tradition_pid: String,
        val tradition_spec: Any,
        val num: Int,
        val son: List<Son>,

        val tradition_hotel_id: String,
        val tradition_state: String,
        val tradition_wash_id: String,
        val tradition_hotel_name: String,
        val wash_name: String,
        val floor_name: String?,

        val tradition_ordernumber: String,
        val tradition_data: String,
        val tradition_hotelid: String,
        val tradition_price: String,
        val tradition_addtime: String,
        val tradition_action: String,
        val tradition_transport_id: String,
        val tradition_order_type: String,
        val tradition_wash_name: String,

        val slipper: Int,

        val version: Version
) : Serializable

data class Son(
        val id: String,
        val tradition_name: String,
        val tradition_pid: String,
        val tradition_spec: String
)

data class Version(
    val code: String,
    val name: String,
    val url: String,
    val content: String
)