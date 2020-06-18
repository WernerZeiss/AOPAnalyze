package com.werner.aopanalyze

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author CWQ
 * @date 2020/6/16
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_tv_jump.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}