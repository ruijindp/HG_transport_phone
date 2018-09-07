package com.hgkefang.transport.util;

import android.support.annotation.NonNull;

import java.util.concurrent.ThreadFactory;

/**
 * Created by Administrator
 */
public class ThreadFactoryBuilder1 implements ThreadFactory {

    private String name;
    private int counter;

    public ThreadFactoryBuilder1(String name) {
        this.name = name;
        counter = 1;
    }

    @Override
    public Thread newThread(@NonNull Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setName("ThreadFactoryBuilder_" + name + "_" + counter);
        return thread;
    }
}
