package com.autokm.app

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Schreibt unbehandelte Absturzursachen (auch OutOfMemoryError etc., nicht nur Exception)
 * in eine lokale Datei, damit der letzte Absturz nach einem Neustart auf dem Screen
 * angezeigt werden kann - ohne ADB/Logcat-Zugriff auf das Gerät.
 */
object CrashLogger {
    private const val DATEINAME = "last_crash.txt"

    fun installieren(context: Context) {
        val appContext = context.applicationContext
        val vorherigerHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(appContext.filesDir, DATEINAME).writeText(sw.toString())
            } catch (_: Throwable) {
                // nichts weiter tun, wir wollen den ursprünglichen Handler auf jeden Fall noch aufrufen
            }
            vorherigerHandler?.uncaughtException(thread, throwable)
        }
    }

    fun letzterAbsturz(context: Context): String? {
        val datei = File(context.applicationContext.filesDir, DATEINAME)
        return if (datei.exists()) datei.readText() else null
    }

    fun loeschen(context: Context) {
        File(context.applicationContext.filesDir, DATEINAME).delete()
    }
}
