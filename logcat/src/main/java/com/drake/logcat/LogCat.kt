/*
 * Copyright (C) 2018 Drake, https://github.com/liangjingkanji
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.drake.logcat

import android.util.Log
import com.drake.logcat.LogCat.Type.*
import com.drake.logcat.LogCat.enabled
import com.drake.logcat.LogCat.logHooks
import com.drake.logcat.LogCat.tag
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.math.min


@Suppress("MemberVisibilityCanBePrivate")
/**
 * @property tag 默认日志标签
 * @property enabled 日志全局开关
 * @property logHooks 日志拦截器
 */
object LogCat {

    enum class Type {
        VERBOSE, DEBUG, INFO, WARN, ERROR, WTF
    }

    /** 日志默认标签 */
    var tag = "日志"

    /** 是否启用日志 */
    var enabled = true

    /** 日志是否显示代码位置 */
    var traceEnabled = true

    /** 日志的Hook钩子 */
    val logHooks by lazy { ArrayList<LogHook>() }

    /**
     * @param enabled 是否启用日志
     * @param tag 日志默认标签
     */
    fun setDebug(enabled: Boolean, tag: String = this.tag) {
        this.enabled = enabled
        this.tag = tag
    }

    //<editor-fold desc="Hook">
    /**
     * 添加日志拦截器
     */
    fun addHook(hook: LogHook) {
        logHooks.add(hook)
    }

    /**
     * 删除日志拦截器
     */
    fun removeHook(hook: LogHook) {
        logHooks.remove(hook)
    }
    //</editor-fold>

    // <editor-fold desc="Log">

    @JvmOverloads
    @JvmStatic
    fun v(
        msg: String?,
        tag: String = this.tag,
        tr: Throwable? = null,
        occurred: Throwable? = Exception()
    ) {

        print(VERBOSE, msg, tag, tr, occurred)
    }

    @JvmOverloads
    @JvmStatic
    fun i(
        msg: String?,
        tag: String = this.tag,
        tr: Throwable? = null,
        occurred: Throwable? = Exception()
    ) {
        print(INFO, msg, tag, tr, occurred)
    }

    @JvmOverloads
    @JvmStatic
    fun d(
        msg: String?,
        tag: String = this.tag,
        tr: Throwable? = null,
        occurred: Throwable? = Exception()
    ) {
        print(DEBUG, msg, tag, tr, occurred)
    }

    @JvmOverloads
    @JvmStatic
    fun w(
        msg: String?,
        tag: String = this.tag,
        tr: Throwable? = null,
        occurred: Throwable? = Exception()
    ) {
        print(WARN, msg, tag, tr, occurred)
    }

    @JvmOverloads
    @JvmStatic
    fun e(
        msg: String?,
        tag: String = this.tag,
        tr: Throwable? = null,
        occurred: Throwable? = Exception()
    ) {
        print(ERROR, msg, tag, tr, occurred)
    }

    @JvmOverloads
    @JvmStatic
    fun wtf(
        msg: String?,
        tag: String = this.tag,
        tr: Throwable? = null,
        occurred: Throwable? = Exception()
    ) {
        print(WTF, msg, tag, tr, occurred)
    }

    /**
     * 输出日志
     * 如果[msg]和[occurred]为空或者[tag]为空将不会输出日志, 拦截器
     *
     * @param type 日志等级
     * @param msg 日志信息
     * @param tag 日志标签
     * @param occurred 日志异常
     */
    private fun print(
        type: Type = INFO,
        msg: String? = null,
        tag: String = this.tag,
        tr: Throwable? = null,
        occurred: Throwable? = Exception()
    ) {
        if (!enabled || msg == null) return

        val info = LogInfo(type, msg, tag, tr, occurred)
        for (logHook in logHooks) {
            logHook.hook(info)
            if (info.msg == null) return
        }

        var adjustMsg = msg
        if (traceEnabled && occurred != null) {
            occurred.stackTrace.getOrNull(1)?.run {
                adjustMsg += " ($fileName:$lineNumber)"
            }
        }
        val max = 3800
        val length = adjustMsg.length
        if (length > max) {
            synchronized(this) {
                var startIndex = 0
                var endIndex = max
                while (startIndex < length) {
                    endIndex = min(length, endIndex)
                    val substring = adjustMsg.substring(startIndex, endIndex)
                    log(type, substring, tag, tr)
                    startIndex += max
                    endIndex += max
                }
            }
        } else {
            log(type, adjustMsg, tag, tr)
        }
    }

    /**
     * Json格式输出Log
     *
     * @param msg JSON
     * @param tag     标签
     * @param url     地址
     */
    @JvmOverloads
    @JvmStatic
    fun json(
        msg: String?,
        tag: String = this.tag,
        url: String? = null,
        type: Type = INFO,
        occurred: Throwable? = Exception()
    ) {
        if (!enabled || msg == null) return

        if (msg.isNullOrBlank()) {
            val adjustMsg = if (url.isNullOrBlank()) msg else url
            print(type, adjustMsg, tag)
            return
        }

        val tokener = JSONTokener(msg)
        val obj = try {
            tokener.nextValue()
        } catch (e: Exception) {
            "Parse json error"
        }

        var adjustMsg = when (obj) {
            is JSONObject -> {
                obj.toString(2)
            }
            is JSONArray -> {
                obj.toString(2)
            }
            else -> obj.toString()
        }
        if (!url.isNullOrBlank()) adjustMsg = "$url\n$adjustMsg"

        print(type, adjustMsg, tag, occurred)
    }

    private fun log(type: Type, msg: String, tag: String, tr: Throwable?) {
        when (type) {
            VERBOSE -> Log.v(tag, msg, tr)
            DEBUG -> Log.d(tag, msg, tr)
            INFO -> Log.i(tag, msg, tr)
            WARN -> Log.w(tag, msg, tr)
            ERROR -> Log.e(tag, msg, tr)
            WTF -> Log.wtf(tag, msg, tr)
        }
    }
    // </editor-fold>
}