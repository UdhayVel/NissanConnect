package com.mappls.app.navigation.demo.car.extensions;
import android.os.Handler;
import android.os.Looper;

public class ThreadUtils {
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on the main thread
            runnable.run();
        } else {
            // Post to the main thread
            mainHandler.post(runnable);
        }
    }
}