package com.example.music_v20.modulos.home.domain.extensions

import java.util.Locale

fun Long.toMinSeg(): String{
    if(this <= 0L) return "00:00"

    val segTotales = this / 1000

    val min = segTotales / 60
    val seg = segTotales % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, seg)
}