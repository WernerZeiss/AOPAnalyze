package com.werner.aopanalyze

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * @author CWQ
 * @date 2020/6/16
 */
class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}