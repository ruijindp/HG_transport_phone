package com.hgkefang.transport.util

import java.util.concurrent.ThreadFactory

/**
 * Created by Administrator
 */
class ThreadFactoryBuilder(private val name: String) : ThreadFactory {
    private val counter: Int = 1

    override fun newThread(runnable: Runnable): Thread {
        val thread = Thread(runnable, name)
        thread.name = "ThreadFactoryBuilder_" + name + "_" + counter
        return thread
    }
}
