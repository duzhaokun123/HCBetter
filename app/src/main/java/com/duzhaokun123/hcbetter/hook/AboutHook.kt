package com.duzhaokun123.hcbetter.hook

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.view.Menu
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.duzhaokun123.hcbetter.BuildConfig
import com.duzhaokun123.hcbetter.TestActivity
import com.duzhaokun123.hcbetter.XposedInit
import com.duzhaokun123.hcbetter.utils.*
import com.duzhaokun123.hcbetter.view.SimpleItem

class AboutHook(mClassLoader: ClassLoader) : BaseHook(mClassLoader) {
    @SuppressLint("SetTextI18n")
    override fun startHook() {
        "com.guoshi.httpcanary.ui.settings.SettingsActivity".hookAfterMethod(mClassLoader, "onResume") {
            val self = it.thisObject as Activity
            val sl = self.findViewByClass(ScrollView::class.java, true)[0]
            if (sl.tag != "hooked1") sl.tag =
                "hooked1" else return@hookAfterMethod
            ((sl as ViewGroup).getChildAt(0) as ViewGroup).apply {
                addView(SimpleItem(self).apply {
                    title = "About HC Better"
                    desc = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
                    setOnClickListener {
                        AlertDialog.Builder(self)
                            .setTitle("about HC Better")
                            .setMessage("HC Better version: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) from ${XposedInit.modulePath}${if (isBuiltIn) "(BuiltIn)" else ""}\n" +
                                    "HttpCanary version: ${getPackageVersion(self.packageName)} (${if (is64) "64" else "32"}bit)\n" +
                                    "SDK: ${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT}); Phone: ${Build.BRAND} ${Build.MODEL}\n" +
                                    "Config: ${sPrefs.all}")
                            .show()
                    }
                })
                addView(Button(self).apply {
                    text = "test"
                    setOnClickListener {
                        self.startActivity(Intent(self, TestActivity::class.java))
                    }
                })
            }
        }
        "com.guoshi.httpcanary.ui.others.AboutActivity".hookAfterMethod(mClassLoader, "onCreateOptionsMenu", Menu::class.java) {
            val self = it.thisObject as Activity
            val tv = self.findViewByClass(TextView::class.java, true).find { (it as TextView).text.startsWith("HttpCanary v") }!!
            if (tv.tag != "hooked1") tv.tag =
                "hooked1" else return@hookAfterMethod
            (tv as TextView).apply {
                text = "$text\nHC Better v${BuildConfig.VERSION_NAME}"
            }
        }
    }
}