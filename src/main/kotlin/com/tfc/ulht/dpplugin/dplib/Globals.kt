package com.tfc.ulht.dpplugin.dplib

var BASE_URL = "http://deisi.ulusofona.pt/drop-project/"
const val REFRESH_INTERVAL = 5000L

fun String.addSuffix(suffix: String): String = if (this.endsWith(suffix) || this.isBlank()) this else this + suffix

fun String.checkAndAddPrefix(testPrefix: String, prefix: String) =
    if (this.startsWith(testPrefix) || this.isBlank()) this else prefix + this

fun String.checkAndAddPrefix(testPrefixes: List<String>, prefix: String) =
    if (true in testPrefixes.map { this.startsWith(it) }) this else prefix + this