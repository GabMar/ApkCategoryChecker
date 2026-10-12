// SPDX-License-Identifier: MIT
package com.apkcategorychecker.framework

/** A fact about how a detected framework is used, e.g. the JavaScript engine of a React Native app. */
class Detail(
    val key: String,
    private val value: (ApkInspection) -> String?,
) {
    fun find(apk: ApkInspection): String? = value(apk)
}

/** A detail whose value is the first option whose signal is found, e.g. `"Hermes" to nativeLib("libhermes.so")`. */
fun detail(
    key: String,
    vararg options: Pair<String, Signal>,
) = Detail(key) { apk -> options.firstOrNull { (_, signal) -> signal.find(apk) != null }?.first }

class Framework(
    val name: String,
    private val signals: List<Signal>,
    /** True if all the signals must be found, false if any of them is enough. */
    private val requireAll: Boolean,
    /** Names of the frameworks this one may be built on, in order of preference: the detected one is the base framework. */
    val runsOn: List<String> = emptyList(),
    private val details: List<Detail> = emptyList(),
) {
    /** The evidence found in the APK, or null if the framework is not detected. */
    fun detect(apk: ApkInspection): List<String>? =
        if (requireAll) {
            // Stop at the first missing signal: the next ones may be expensive (e.g. the resource table).
            signals.map { it.find(apk) ?: return null }
        } else {
            signals.mapNotNull { it.find(apk) }.ifEmpty { null }
        }

    /** The details found, for a detected framework. */
    fun details(apk: ApkInspection): Map<String, String> =
        details.mapNotNull { detail -> detail.find(apk)?.let { detail.key to it } }.toMap()

    override fun toString() = name
}

object Frameworks {
    /**
     * Any app with at least [threshold] web resource files; disabled when [threshold] is 0.
     * Not part of the rules, as the threshold comes from the command line.
     */
    fun undefinedWebApp(threshold: Int) =
        Framework(
            "Undefined_Web_App",
            listOf(
                Signal { apk ->
                    val count = apk.countFiles(".html") + apk.countFiles(".js") + apk.countFiles(".css")
                    if (threshold > 0 && count >= threshold) "$count web resource files (threshold $threshold)" else null
                },
            ),
            requireAll = false,
        )
}
