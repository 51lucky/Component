package com.xiaojinzi.component2.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaojinzi.component.anno.RouterAnno
import com.xiaojinzi.component2.R

@RouterAnno(hostAndPath = "component2/test")
class Component2TestAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.component2_test_act)
    }

}
