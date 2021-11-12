package com.duzhaokun123.hcbetter.utils

import android.app.Activity
import android.app.AndroidAppHelper
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import com.duzhaokun123.hcbetter.XposedInit
import kotlinx.coroutines.*
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.File
import java.lang.reflect.Method
import java.security.KeyStore
import kotlin.coroutines.CoroutineContext

val HTTP_CANARY_PACKAGE_NAME = listOf("com.guoshi.httpcanary.premium", "com.guoshi.httpcanary")

val currentContext by lazy { AndroidAppHelper.currentApplication() as Context }

@Suppress("DEPRECATION")
val sPrefs
    get() = currentContext.getSharedPreferences("hcbetter", Context.MODE_MULTI_PROCESS)!!

val isBuiltIn
    get() = XposedInit.modulePath.endsWith("so")

val is64
    get() = currentContext.applicationInfo.nativeLibraryDir.contains("64")

fun getVersionCode(packageName: String) = try {
    @Suppress("DEPRECATION")
    systemContext.packageManager.getPackageInfo(packageName, 0).versionCode
} catch (e: Throwable) {
    Log.e(e)
    null
} ?: 0

fun getPackageVersion(packageName: String) = try {
    systemContext.packageManager.getPackageInfo(packageName, 0).run {
        @Suppress("DEPRECATION")
        String.format("${packageName}@%s(%s)", versionName, getVersionCode(packageName))
    }
} catch (e: Throwable) {
    Log.e(e)
    "(unknown)"
}

val systemContext: Context
    get() {
        val activityThread = "android.app.ActivityThread".findClassOrNull(null)
            ?.callStaticMethod("currentActivityThread")!!
        return activityThread.callMethodAs("getSystemContext")
    }

val logFile by lazy { File(currentContext.externalCacheDir, "log.txt") }

fun getId(name: String) = currentContext.resources.getIdentifier(name, "id", currentContext.packageName)

fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) {
        return this.bitmap
    }
    val bitmap = if (this.intrinsicWidth <= 0 || this.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }
    val canvas = Canvas(bitmap)
    this.draw(canvas)
    return bitmap
}

fun ViewGroup.findViewByClass(type: Class<*>, allowParentMatch: Boolean = false): List<View> {
//    Log.d("for ${this::class.java.name} find ${type.name}, allowParentMatch: $allowParentMatch")
    val re = mutableListOf<View>()
    (0 until childCount).forEach { i ->
        val v = this.getChildAt(i)
//        Log.d("find view ${v::class.java.name}")
        if (v::class.java == type) re.add(v)
        else if (allowParentMatch && type.isAssignableFrom(v::class.java)) re.add(v)
        if (v is ViewGroup) re.addAll(v.findViewByClass(type, allowParentMatch))
    }
//    Log.d("found all")
    return re
}

fun Activity.findViewByClass(type: Class<*>, allowParentMatch: Boolean = false) = (this.window.decorView as ViewGroup).findViewByClass(type, allowParentMatch)

fun runMain(block: suspend CoroutineScope.() -> Unit) =
    GlobalScope.launch(Dispatchers.Main, block = block)

fun runIO(block: suspend CoroutineScope.() -> Unit) =
    GlobalScope.launch(Dispatchers.IO, block = block)

fun runNewThread(block: () -> Unit) =
    Thread(block).start()

fun View.getOnClick() =  this.callMethod("getListenerInfo")!!
    .getObjectFieldAs<View.OnClickListener>("mOnClickListener")

fun View.addOnClickAfter(after: View.OnClickListener) {
    val onClick = this.getOnClick()
    this.setOnClickListener {
        onClick.onClick(it)
        after.onClick(it)
    }
}

fun getJks(classLoader: ClassLoader): Any {
    val sslClasses =  XposedInit.classesList
        .filter { it.startsWith("com.github.megatronking.netbare.ssl") }
        .map { it.findClass(classLoader) }
    val jksClass =
        sslClasses.find { it.constructors.find { it.parameterCount ==  8} != null }!!
    val jks = jksClass.new(
        currentContext,
        "HttpCanary",
        "HttpCanary".toCharArray(),
        "HttpCanary Root CA",
        "HttpCanary",
        "HttpCanary",
        "HttpCanary Root CA",
        "HttpCanary")
    if (File(currentContext.cacheDir, "HttpCanary.pem").exists()) {
        Log.toast("keystore generated")
        return jks
    }
    var generateRootMethod: Method? = null
    sslClasses.forEach {
        if (generateRootMethod == null)
            generateRootMethod = it.findMethodsByReturnAndArgs(KeyStore::class.java, jksClass).takeIf { it.isNotEmpty() }?.get(0)
    }
    val keyStore = generateRootMethod!!.invoke(null, jks) as KeyStore
    val os = File(currentContext.cacheDir, "HttpCanary.p12").outputStream()
    keyStore.store(os, "HttpCanary".toCharArray())
    val cert = keyStore.getCertificate("HttpCanary")
    val sw = File(currentContext.cacheDir, "HttpCanary.pem").writer()
    val pm = JcaPEMWriter(sw)
    pm.writeObject(cert)
    pm.flush()
    os.close()
    sw.close()
    pm.close()
    Log.toast("generate keystore succeed")
    return jks
}