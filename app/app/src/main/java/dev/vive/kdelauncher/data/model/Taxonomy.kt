package dev.vive.kdelauncher.data.model

/**
 * Full taxonomy available to the AI. The launcher only shows 5 fixed tabs by default,
 * but the LLM can choose from this extended pool and create dynamic categories.
 *
 * Grouping guidance:
 * - If a category would end up with only 1-2 apps, merge it into the closest
 *   semantic super-group (e.g. Yape + Rappi → "shopping" instead of separate
 *   "finance" + "shopping").
 * - Aim for 5-7 categories total, never more than 7.
 */
val defaultCategories = listOf(
    AiCategory("social",       "Social",       "Messaging, chat, social networks, communities, dating",
               listOf("message-circle","users","heart","at-sign","share-2")),
    AiCategory("media",        "Media",        "Video streaming, music players, podcasts, radio",
               listOf("play-circle","music","video","headphones","radio")),
    AiCategory("creativity",   "Creatividad",  "Photo editing, design, drawing, gallery apps",
               listOf("image","palette","pen-tool","camera","aperture")),
    AiCategory("games",        "Juegos",       "Mobile games, game launchers, emulators",
               listOf("gamepad-2","swords","trophy","zap","dice-5")),
    AiCategory("finance",      "Finanzas",     "Banking, wallets, payments, crypto, investments",
               listOf("wallet","banknote","shield-dollar","coins","trending-up")),
    AiCategory("shopping",     "Compras",      "E-commerce, delivery, marketplaces, food ordering",
               listOf("shopping-bag","package","tag","store","truck")),
    AiCategory("travel",       "Viajes",       "Maps, navigation, ride hailing, hotels, booking",
               listOf("map-pin","plane","car","compass","navigation")),
    AiCategory("productivity", "Productividad","Notes, tasks, calendar, documents, email clients",
               listOf("briefcase","file-text","calendar","check-square","mail")),
    AiCategory("browsers",     "Navegadores",  "Web browsers, privacy browsers",
               listOf("globe","shield","search","wifi","lock")),
    AiCategory("development",  "Desarrollo",   "Terminals, SSH, developer tools, FOSS stores, app patchers",
               listOf("terminal","code-2","cpu","git-branch","package-2")),
    AiCategory("utilities",    "Utilidades",   "System utilities, file managers, VPN, backup, device sync",
               listOf("wrench","settings","hard-drive","layers","shield-check"))
)
