package com.hgkefang.transport.net

const val APP_PORT = "transport"

private const val ROOT_URL = "http://test.api.v5.hgkefang.com" //测试
//private const val ROOT_URL = "http://api.v5.hgkefang.com"    //正式

//登录
const val API_LOGIN = "$ROOT_URL/user-login.json"
//检查是否到期
const val API_CHECK_EXPIRE = "$ROOT_URL/common_check_user.json"
//获取布草类型
const val API_LINEN_TYPE = "$ROOT_URL/tradition_get_linen_type.json"
//获取拖鞋
const val API_SHOE_TYPE = "$ROOT_URL/tradition_get_slipper.json"
//提交订单
const val API_COMMIT_ORDER = "$ROOT_URL/tradition_submitorder.json"
//获取酒店信息
const val API_HOTEL_INFO = "$ROOT_URL/tradition_get_hotel.json"
//订单
const val API_ORDER = "$ROOT_URL/tradition_get_order.json"
