package dev.vive.kdelauncher.domain.usecase

object LocalHeuristics {
    private val exact = mapOf(
        "com.whatsapp"                              to Pair("social",       "message-circle"),
        "org.telegram.messenger"                    to Pair("social",       "message-circle"),
        "com.instagram.android"                     to Pair("social",       "heart"),
        "com.discord"                               to Pair("social",       "users"),
        "com.spotify.music"                         to Pair("media",        "music"),
        "com.google.android.youtube"                to Pair("media",        "play-circle"),
        "com.netflix.mediaclient"                   to Pair("media",        "video"),
        "com.google.android.gm"                     to Pair("productivity", "mail"),
        "org.mozilla.fenix"                         to Pair("browsers",     "globe"),
        "com.brave.browser"                         to Pair("browsers",     "shield"),
        "com.termux"                                to Pair("development",  "terminal"),
        "org.connectbot"                            to Pair("development",  "terminal"),
        "org.fdroid.fdroid"                         to Pair("development",  "package-2"),
        "com.aurora.store"                          to Pair("development",  "package-2"),
        "app.revanced.manager.flutter"              to Pair("development",  "code-2"),
        "dev.imranr.obtainium.fdroid"               to Pair("development",  "git-branch"),
        "com.supercell.clashofclans"                to Pair("games",        "swords"),
        "com.miHoYo.GenshinImpact"                  to Pair("games",        "gamepad-2"),
        "com.valvesoftware.android.steam.community" to Pair("games",        "gamepad-2"),
        "com.google.android.apps.maps"              to Pair("travel",       "map-pin"),
        "com.ubercab"                               to Pair("travel",       "car"),
        "com.bcp.innovacxion.yapeapp"               to Pair("finance",      "wallet"),
        "com.paypal.android.p2pmobile"              to Pair("finance",      "banknote"),
        "com.coinbase.android"                      to Pair("finance",      "coins"),
        "org.monero.wallet"                         to Pair("finance",      "shield-dollar"),
        "com.kunzisoft.keepass.libre"               to Pair("utilities",    "shield-check"),
        "org.kde.kdeconnect_tp"                     to Pair("utilities",    "layers"),
        "org.localsend.localsend_app"               to Pair("utilities",    "hard-drive"),
        "com.adobe.lrmobile"                        to Pair("creativity",   "aperture"),
        "com.canva.editor"                          to Pair("creativity",   "palette"),
        "com.simplemobiletools.gallery.pro"         to Pair("creativity",   "image"),
    )

    fun classify(packageName: String): Pair<String, String>? = exact[packageName]
}
