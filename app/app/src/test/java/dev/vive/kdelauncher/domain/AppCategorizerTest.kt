package dev.vive.kdelauncher.domain

import dev.vive.kdelauncher.data.model.AppCategory
import dev.vive.kdelauncher.data.model.AppCategorizer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AppCategorizerTest : FunSpec({

    context("development apps") {
        test("com.termux -> DEVELOPMENT") {
            AppCategorizer.categorize("com.termux", -1) shouldBe AppCategory.DEVELOPMENT
        }
        test("com.github.android -> DEVELOPMENT") {
            AppCategorizer.categorize("com.github.android", -1) shouldBe AppCategory.DEVELOPMENT
        }
        test("com.google.android.apps.code -> DEVELOPMENT") {
            AppCategorizer.categorize("com.google.android.apps.code", -1) shouldBe AppCategory.DEVELOPMENT
        }
        test("com.jetbrains.rider -> DEVELOPMENT") {
            AppCategorizer.categorize("com.jetbrains.rider", -1) shouldBe AppCategory.DEVELOPMENT
        }
    }

    context("graphics apps") {
        test("com.google.android.apps.photos -> GRAPHICS") {
            AppCategorizer.categorize("com.google.android.apps.photos", -1) shouldBe AppCategory.GRAPHICS
        }
        test("package containing 'gallery' -> GRAPHICS") {
            AppCategorizer.categorize("com.example.gallery.app", -1) shouldBe AppCategory.GRAPHICS
        }
        test("com.niksoftware.snapseed -> GRAPHICS") {
            AppCategorizer.categorize("com.niksoftware.snapseed", -1) shouldBe AppCategory.GRAPHICS
        }
    }

    context("internet apps") {
        test("com.android.chrome -> INTERNET") {
            AppCategorizer.categorize("com.android.chrome", -1) shouldBe AppCategory.INTERNET
        }
        test("com.whatsapp -> INTERNET") {
            AppCategorizer.categorize("com.whatsapp", -1) shouldBe AppCategory.INTERNET
        }
        test("com.discord -> INTERNET") {
            AppCategorizer.categorize("com.discord", -1) shouldBe AppCategory.INTERNET
        }
    }

    context("game apps") {
        test("package containing 'game' -> GAMES") {
            AppCategorizer.categorize("com.example.game.app", -1) shouldBe AppCategory.GAMES
        }
        test("package containing 'play' -> GAMES") {
            AppCategorizer.categorize("com.google.android.play.games", -1) shouldBe AppCategory.GAMES
        }
    }

    context("multimedia apps") {
        test("com.spotify.music -> MULTIMEDIA") {
            AppCategorizer.categorize("com.spotify.music", -1) shouldBe AppCategory.MULTIMEDIA
        }
        test("com.google.android.youtube -> MULTIMEDIA (contains 'youtube')") {
            AppCategorizer.categorize("com.google.android.youtube", -1) shouldBe AppCategory.MULTIMEDIA
        }
        test("com.netflix.mediaclient -> MULTIMEDIA") {
            AppCategorizer.categorize("com.netflix.mediaclient", -1) shouldBe AppCategory.MULTIMEDIA
        }
    }

    context("system apps") {
        test("com.android.settings -> SYSTEM") {
            AppCategorizer.categorize("com.android.settings", -1) shouldBe AppCategory.SYSTEM
        }
    }

    context("utility apps") {
        test("com.android.calculator2 -> UTILITIES") {
            AppCategorizer.categorize("com.android.calculator2", -1) shouldBe AppCategory.UTILITIES
        }
        test("com.google.android.apps.maps -> UTILITIES") {
            AppCategorizer.categorize("com.google.android.apps.maps", -1) shouldBe AppCategory.UTILITIES
        }
        test("com.android.contacts -> UTILITIES") {
            AppCategorizer.categorize("com.android.contacts", -1) shouldBe AppCategory.UTILITIES
        }
    }

    context("unknown apps default to ALL") {
        test("com.example.unknown -> ALL") {
            AppCategorizer.categorize("com.example.unknown", -1) shouldBe AppCategory.ALL
        }
        test("org.random.app -> ALL") {
            AppCategorizer.categorize("org.random.app", -1) shouldBe AppCategory.ALL
        }
    }

    context("regression: known conflict cases") {
        test("Google Play Store (com.android.vending) is resolved by exact allowlist") {
            AppCategorizer.categorize("com.android.vending", -1) shouldBe AppCategory.INTERNET
        }

        test("audioplayer prefers multimedia tokens over generic play match") {
            AppCategorizer.categorize("com.maxmpz.audioplayer", -1) shouldBe AppCategory.MULTIMEDIA
        }

        test("Facebook Pages Manager is resolved by exact allowlist") {
            AppCategorizer.categorize("com.facebook.pages.app", -1) shouldBe AppCategory.INTERNET
        }
    }

    context("scoring behavior") {
        test("longer multimedia token outranks generic game token inside same package") {
            AppCategorizer.categorize("org.example.player.gamehub", -1) shouldBe AppCategory.MULTIMEDIA
        }

        test("exact token match outranks weaker partial token") {
            AppCategorizer.categorize("com.example.music.app", -1) shouldBe AppCategory.MULTIMEDIA
        }
    }

    context("Android system category fallback") {
        test("CATEGORY_GAME falls back to GAMES") {
            AppCategorizer.categorize("com.unknown.app", android.content.pm.ApplicationInfo.CATEGORY_GAME) shouldBe AppCategory.GAMES
        }

        test("CATEGORY_AUDIO falls back to MULTIMEDIA") {
            AppCategorizer.categorize("com.unknown.app", android.content.pm.ApplicationInfo.CATEGORY_AUDIO) shouldBe AppCategory.MULTIMEDIA
        }

        test("CATEGORY_VIDEO falls back to MULTIMEDIA") {
            AppCategorizer.categorize("com.unknown.app", android.content.pm.ApplicationInfo.CATEGORY_VIDEO) shouldBe AppCategory.MULTIMEDIA
        }

        test("CATEGORY_IMAGE falls back to GRAPHICS") {
            AppCategorizer.categorize("com.unknown.app", android.content.pm.ApplicationInfo.CATEGORY_IMAGE) shouldBe AppCategory.GRAPHICS
        }

        test("CATEGORY_SOCIAL falls back to INTERNET") {
            AppCategorizer.categorize("com.unknown.app", android.content.pm.ApplicationInfo.CATEGORY_SOCIAL) shouldBe AppCategory.INTERNET
        }

        test("CATEGORY_MAPS falls back to UTILITIES") {
            AppCategorizer.categorize("com.unknown.app", android.content.pm.ApplicationInfo.CATEGORY_MAPS) shouldBe AppCategory.UTILITIES
        }

        test("CATEGORY_PRODUCTIVITY falls back to UTILITIES") {
            AppCategorizer.categorize("com.unknown.app", android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY) shouldBe AppCategory.UTILITIES
        }

        test("unknown category falls back to ALL") {
            AppCategorizer.categorize("com.unknown.app", 999) shouldBe AppCategory.ALL
        }
    }
})
