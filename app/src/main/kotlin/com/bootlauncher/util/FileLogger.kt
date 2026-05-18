package com.bootlauncher.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {

    private const val TAG = "FileLogger"
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "bootlauncher.log"
    private const val MAX_LOG_SIZE = 512 * 1024L // 512KB

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var logFile: File? = null

    @Synchronized
    fun init(context: Context) {
        val dir = File(context.getExternalFilesDir(null), LOG_DIR)
        if (!dir.exists()) dir.mkdirs()
        logFile = File(dir, LOG_FILE)
        rotateIfNeeded()
        write("INFO", "FileLogger", "Logger initialized. Log file: ${logFile?.absolutePath}")
        Log.d(TAG, "Log file: ${logFile?.absolutePath}")
    }

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        write("DEBUG", tag, msg)
    }

    fun w(tag: String, msg: String, throwable: Throwable? = null) {
        Log.w(tag, msg, throwable)
        val fullMsg = if (throwable != null) "$msg\n${Log.getStackTraceString(throwable)}" else msg
        write("WARN", tag, fullMsg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        Log.e(tag, msg, throwable)
        val fullMsg = if (throwable != null) "$msg\n${Log.getStackTraceString(throwable)}" else msg
        write("ERROR", tag, fullMsg)
    }

    fun getLogFile(): File? = logFile

    @Synchronized
    private fun write(level: String, tag: String, msg: String) {
        val file = logFile ?: return
        try {
            FileWriter(file, true).use { writer ->
                writer.append("${dateFormat.format(Date())} $level/$tag: $msg\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    private fun rotateIfNeeded() {
        val file = logFile ?: return
        if (file.exists() && file.length() > MAX_LOG_SIZE) {
            val backup = File(file.parentFile, "${LOG_FILE}.bak")
            backup.delete()
            file.renameTo(backup)
        }
    }
}
