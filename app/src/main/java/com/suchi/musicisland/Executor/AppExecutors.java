package com.suchi.musicisland.Executor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {
    public static final Executor DB = Executors.newSingleThreadExecutor();
}
