package com.werner.aopanalyze;

import android.app.Activity;
import android.widget.Toast;

/**
 * @author CWQ
 * @date 2020/6/18
 */
public class TraceUtil {
    private final String TAG = "TraceUtil";

    /**
     * 当Activity执行了onCreate时触发
     *
     * @param activity
     */
    public static void onActivityCreate(Activity activity) {
        Toast.makeText(activity
                , activity.getClass().getName() + "call onCreate"
                , Toast.LENGTH_LONG).show();
    }


    public static void onActivityResume(Activity activity) {
        Toast.makeText(activity, activity.getClass().getName() + "call onResume", Toast.LENGTH_LONG).show();
    }


    public static void onActivityPause(Activity activity) {
        Toast.makeText(activity, activity.getClass().getName() + "call onPause", Toast.LENGTH_LONG).show();
    }


    /**
     * 当Activity执行了onDestroy时触发
     *
     * @param activity
     */
    public static void onActivityDestroy(Activity activity) {
        Toast.makeText(activity
                , activity.getClass().getName() + "call onDestroy"
                , Toast.LENGTH_LONG).show();
    }
}
