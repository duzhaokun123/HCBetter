package com.duzhaokun123.hcbetter

import android.app.Application
import android.app.Instrumentation
import android.content.res.Resources
import android.content.res.XModuleResources
import android.os.Build
import com.duzhaokun123.hcbetter.hook.*
import com.duzhaokun123.hcbetter.utils.*
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.text.SimpleDateFormat
import java.util.*

class XposedInit : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        lateinit var modulePath: String
        lateinit var moduleRes: Resources
        private val hookers = ArrayList<BaseHook>()
        lateinit var classesList: List<String>
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
        moduleRes = XModuleResources.createInstance(modulePath, null)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName !in HTTP_CANARY_PACKAGE_NAME
            &&  runCatching { Class.forName("com.guoshi.httpcanary.ui.SplashActivity") }.isFailure) return

        Instrumentation::class.java.hookBeforeMethod(
            "callApplicationOnCreate",
            Application::class.java
        ) {
            // Hook main process and download process
            @Suppress("DEPRECATION")
            when {
                !lpparam.processName.contains(":") -> {
                    if (sPrefs.getBoolean("save_log", BuildConfig.DEBUG) ) {
                        startLog()
                    }
                    Log.d("HttpCanary process launched ...")
                    Log.d("HC Better version: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) from $modulePath${if (isBuiltIn) "(BuiltIn)" else ""}")
                    Log.d("HttpCanary version: ${getPackageVersion(lpparam.packageName)} (${if (is64) "64" else "32"}bit)")
                    Log.d("SDK: ${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT}); Phone: ${Build.BRAND} ${Build.MODEL}")
                    Log.d("Config: ${sPrefs.all}")
                    Log.toast(
                        "HC Better enabled"
                    )
                    classesList = lpparam.classLoader.allClassesList { it }
                    runNewThread {
                        getJks(lpparam.classLoader)
                    }
                    startHook(HomeHook(lpparam.classLoader))
                    startHook(CoreRunningHook(lpparam.classLoader))
                    startHook(InstallCAHook(lpparam.classLoader))
                    startHook(GrpcPreviewHook(lpparam.classLoader))
                    startHook(AboutHook(lpparam.classLoader))
                }
            }
        }
    }

    private fun startHook(hooker: BaseHook) {
        try {
            hookers.add(hooker)
            hooker.startHook()
        } catch (e: Throwable) {
            Log.e(e)
            Log.toast("error: ${e.message}, some feature may unavailable")
        }
    }

    private fun startLog() = try {
        logFile.delete()
        logFile.createNewFile()
        val cmd = arrayOf(
            "logcat",
            "-T",
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
            "-f",
            logFile.absolutePath
        )
        Runtime.getRuntime().exec(cmd)
    } catch (e: Throwable) {
        Log.e(e)
        null
    }
}