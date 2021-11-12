package com.duzhaokun123.hcbetter.hook

import com.duzhaokun123.hcbetter.utils.hookAfterMethod

class CoreRunningHook(mClassLoader: ClassLoader) : BaseHook(mClassLoader) {
    companion object {
        var running = false
    }
    override fun startHook() {
        "com.guoshi.httpcanary.AppService".hookAfterMethod(mClassLoader, "onCreate") {
            running = true
        }
        "com.guoshi.httpcanary.AppService".hookAfterMethod(mClassLoader, "onDestroy") {
            running = false
        }
    }
}