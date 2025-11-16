package com.example.myapplication.utils;

import android.content.Context;

public class DevicePrefs {

    private static final String PREFS_NAME = "device_prefs";
    private static final String KEY_DEVICE_IP = "device_ip";
    private static final String KEY_DEVICE_NAME = "device_name";

    public static void guardarIP(Context ctx, String ip) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DEVICE_IP, ip)
                .apply();
    }

    public static String obtenerIP(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DEVICE_IP, null);
    }

    public static void guardarNombre(Context ctx, String name) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DEVICE_NAME, name)
                .apply();
    }

    public static String obtenerNombre(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DEVICE_NAME, "Mi Alacena");
    }

    public static void BorrarIP(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_DEVICE_IP)
                .remove(KEY_DEVICE_NAME)
                .apply();
    }
}
