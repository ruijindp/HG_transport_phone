package com.hgkefang.transport.util

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by Administrator
 */
class ThreadPool private constructor() {
    /**
     * java线程池
     */
    private var threadPoolExecutor: ThreadPoolExecutor? = null

    init {
        /*
      线程池缓存队列
     */
        val mWorkQueue = ArrayBlockingQueue<Runnable>(CORE_POOL_SIZE)
        val threadFactory = ThreadFactoryBuilder("ThreadPool")
        threadPoolExecutor = ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_COUNTS, ALIVETIME, TimeUnit.SECONDS, mWorkQueue, threadFactory)
    }

    fun addTask(runnable: Runnable?) {
        if (runnable == null) {
            throw NullPointerException("addTask(Runnable runnable)传入参数为空")
        }
        if (threadPoolExecutor != null && threadPoolExecutor!!.activeCount < MAX_POOL_COUNTS) {
            threadPoolExecutor!!.execute(runnable)
        }
    }

    fun stopThreadPool() {
        if (threadPoolExecutor != null) {
            threadPoolExecutor!!.shutdown()
            threadPoolExecutor = null
        }
    }

    companion object {

        private var threadPool: ThreadPool? = null

        /**
         * 最大线程数
         */
        private const val MAX_POOL_COUNTS = 20

        /**
         * 线程存活时间
         */
        private const val ALIVETIME = 200L

        /**
         * 核心线程数
         */
        private const val CORE_POOL_SIZE = 20

        val instantiation: ThreadPool
            get() {
                if (threadPool == null) {
                    threadPool = ThreadPool()
                }
                return threadPool!!
            }
    }
}
