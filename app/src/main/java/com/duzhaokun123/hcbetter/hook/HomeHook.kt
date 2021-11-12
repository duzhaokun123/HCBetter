package com.duzhaokun123.hcbetter.hook

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Icon
import android.util.Rational
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ImageButton
import com.duzhaokun123.hcbetter.R
import com.duzhaokun123.hcbetter.XposedInit.Companion.moduleRes
import com.duzhaokun123.hcbetter.utils.*

class HomeHook(mClassLoader: ClassLoader) : BaseHook(mClassLoader) {
    companion object {
        const val ACTION_ENTER_PIP = "com.duzhaokun123.hcbetter.ENTER_PIP"
        const val ACTION_CALL_FLOAT = "com.duzhaokun123.hcbetter.CALL_FLOAT"

        val enterPiPReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val activity = context as Activity
                val rational = Rational(activity.window.decorView.width, activity.window.decorView.height)
                val action = RemoteAction(Icon.createWithResource("android", android.R.drawable.stat_sys_warning), "", "", PendingIntent.getBroadcast(activity, 114_4, Intent(
                    ACTION_CALL_FLOAT)
                , PendingIntent.FLAG_IMMUTABLE))
                activity.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(rational).setActions(
                    listOf(action)).build())
            }
        }
        val callFloatReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val activity = context as Activity
                activity.findViewByClass("com.guoshi.httpcanary.ui.ServiceButton".findClass(activity::class.java.classLoader))[0].callOnClick()
            }
        }
    }

    override fun startHook() {
        "com.guoshi.httpcanary.ui.HomeFragment".findClass(mClassLoader)
            .findMethodsByReturnAndArgs(null, Menu::class.java, MenuInflater::class.java).forEach {
                it.hookBeforeMethod { param ->
                    val menu = param.args[0] as Menu
                    menu.add("enter PiP").apply {
                        icon = moduleRes.getDrawable(R.drawable.ic_picture_in_picture)
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        setOnMenuItemClickListener {
                            currentContext.sendBroadcast(Intent(ACTION_ENTER_PIP))
                            true
                        }
                    }
                }
            }
        "com.guoshi.httpcanary.ui.HomeActivity".hookBeforeMethod(
            mClassLoader,
            "onResume"
        ) {
            val self = it.thisObject as Activity
            self.registerReceiver(enterPiPReceiver, IntentFilter(ACTION_ENTER_PIP))
            if (self.window.decorView.tag != "hooked") {
                self.window.decorView.tag = "hooked"
                self.registerReceiver(callFloatReceiver, IntentFilter(ACTION_CALL_FLOAT))
            }
            self.window.apply{
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                statusBarColor = Color.TRANSPARENT
            }
        }
        "com.guoshi.httpcanary.ui.HomeActivity".hookBeforeMethod(
            mClassLoader,
            "onPause"
        ) {
            val self = it.thisObject as Activity
            self.unregisterReceiver(enterPiPReceiver)
        }
        "com.guoshi.httpcanary.ui.HomeActivity".hookBeforeMethod(
            mClassLoader,
            "onDestroy"
        ) {
            val self = it.thisObject as Activity
            self.unregisterReceiver(callFloatReceiver)
        }
    }
}