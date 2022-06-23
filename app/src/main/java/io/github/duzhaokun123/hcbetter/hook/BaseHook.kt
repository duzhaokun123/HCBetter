package io.github.duzhaokun123.hcbetter.hook

abstract class BaseHook(val mClassLoader: ClassLoader) {
    abstract fun startHook()
//    open fun lateInitHook() {}
}