"use client"

import { useState, useMemo } from "react"
import {
  Search,
  Star,
  Grid3X3,
  Code2,
  Palette,
  Globe,
  Gamepad2,
  Music2,
  Wrench,
  FolderOpen,
  Settings,
  Calendar,
  Mail,
  Camera,
  Music,
  Map,
  FileText,
  Calculator,
  Clock,
  Cloud,
  Folder,
  Terminal,
  Code,
  Github,
  Database,
  Cpu,
  Layers,
  Podcast,
  BookOpen,
  Chrome,
  Paintbrush,
  Image,
  Video,
  Briefcase,
  User,
  ChevronRight,
  X,
} from "lucide-react"

type Profile = "personal" | "work"

interface App {
  icon: React.ComponentType<{ className?: string }>
  label: string
  category: string
  workProfile?: boolean
  color?: string
}

const categories = [
  { id: "favorites", label: "Favoritos", icon: Star },
  { id: "all", label: "Todas", icon: Grid3X3 },
  { id: "development", label: "Desarrollo", icon: Code2 },
  { id: "graphics", label: "Gráficos", icon: Palette },
  { id: "internet", label: "Internet", icon: Globe },
  { id: "games", label: "Juegos", icon: Gamepad2 },
  { id: "multimedia", label: "Multimedia", icon: Music2 },
  { id: "system", label: "Sistema", icon: Wrench },
  { id: "utilities", label: "Utilidades", icon: FolderOpen },
]

const apps: App[] = [
  // Favorites
  { icon: Terminal, label: "Terminal", category: "favorites" },
  { icon: Chrome, label: "Browser", category: "favorites" },
  { icon: Settings, label: "Ajustes", category: "favorites" },
  
  // Development
  { icon: Terminal, label: "Terminal", category: "development" },
  { icon: Code, label: "VS Code", category: "development" },
  { icon: Github, label: "GitHub", category: "development" },
  { icon: Database, label: "Database", category: "development" },
  { icon: Cpu, label: "Monitor", category: "development" },
  { icon: Layers, label: "Docker", category: "development" },
  
  // Graphics
  { icon: Paintbrush, label: "Draw", category: "graphics" },
  { icon: Image, label: "Photos", category: "graphics" },
  { icon: Camera, label: "Camera", category: "graphics" },
  
  // Internet
  { icon: Chrome, label: "Browser", category: "internet" },
  { icon: Mail, label: "Mail", category: "internet" },
  { icon: Cloud, label: "Cloud", category: "internet" },
  { icon: Github, label: "GitHub", category: "internet" },
  
  // Games
  { icon: Gamepad2, label: "Games", category: "games" },
  
  // Multimedia
  { icon: Music, label: "Music", category: "multimedia" },
  { icon: Video, label: "Video", category: "multimedia" },
  { icon: Podcast, label: "Podcast", category: "multimedia" },
  { icon: Camera, label: "Camera", category: "multimedia" },
  
  // System
  { icon: Settings, label: "Ajustes", category: "system" },
  { icon: Cpu, label: "Monitor", category: "system" },
  { icon: Folder, label: "Files", category: "system" },
  
  // Utilities
  { icon: Calculator, label: "Calc", category: "utilities" },
  { icon: Clock, label: "Clock", category: "utilities" },
  { icon: Calendar, label: "Calendar", category: "utilities" },
  { icon: FileText, label: "Notes", category: "utilities" },
  { icon: Map, label: "Maps", category: "utilities" },
  { icon: BookOpen, label: "Books", category: "utilities" },

  // Work profile apps
  { icon: Mail, label: "Work Mail", category: "internet", workProfile: true },
  { icon: Calendar, label: "Work Cal", category: "utilities", workProfile: true },
  { icon: Briefcase, label: "Slack", category: "internet", workProfile: true },
  { icon: FileText, label: "Docs", category: "utilities", workProfile: true },
]

export function AppDrawer() {
  const [search, setSearch] = useState("")
  const [activeCategory, setActiveCategory] = useState("favorites")
  const [profile, setProfile] = useState<Profile>("personal")

  const filteredApps = useMemo(() => {
    let filtered = apps.filter((app) => {
      if (profile === "personal" && app.workProfile) return false
      if (profile === "work" && !app.workProfile) return false
      return true
    })

    if (search.trim()) {
      filtered = filtered.filter((app) =>
        app.label.toLowerCase().includes(search.toLowerCase())
      )
    } else if (activeCategory !== "all") {
      filtered = filtered.filter((app) => app.category === activeCategory)
    }

    const seen = new Set()
    return filtered.filter((app) => {
      if (seen.has(app.label)) return false
      seen.add(app.label)
      return true
    })
  }, [search, activeCategory, profile])

  const currentCategory = categories.find((c) => c.id === activeCategory)

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="px-4 pt-3 pb-4 space-y-4">
        {/* Profile Row */}
        <button
          onClick={() => setProfile(profile === "personal" ? "work" : "personal")}
          className="flex items-center gap-3 group w-full"
        >
          {/* Avatar */}
          <div className="relative">
            <div
              className={`w-11 h-11 rounded-2xl flex items-center justify-center transition-all duration-300 ${
                profile === "personal"
                  ? "bg-gradient-to-br from-accent/30 to-accent/10"
                  : "bg-gradient-to-br from-orange-500/30 to-orange-500/10"
              }`}
            >
              {profile === "personal" ? (
                <User className="w-5 h-5 text-accent" />
              ) : (
                <Briefcase className="w-5 h-5 text-orange-400" />
              )}
            </div>
            <div
              className={`absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-background transition-colors ${
                profile === "personal" ? "bg-emerald-500" : "bg-orange-500"
              }`}
            />
          </div>

          {/* Name & Status */}
          <div className="flex flex-col items-start flex-1">
            <span className="text-sm font-semibold tracking-tight">
              {profile === "personal" ? "Personal" : "Trabajo"}
            </span>
            <span className="text-[10px] text-muted-foreground flex items-center gap-0.5">
              Cambiar perfil
              <ChevronRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
            </span>
          </div>
        </button>

        {/* Search Bar */}
        <div className="relative flex items-center gap-3 px-4 py-3 bg-secondary/50 border border-border/40 rounded-2xl focus-within:border-accent/30 transition-colors">
          <Search className="h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Buscar..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1 bg-transparent text-sm text-foreground placeholder:text-muted-foreground/60 outline-none"
          />
          {search && (
            <button
              onClick={() => setSearch("")}
              className="p-1 rounded-lg hover:bg-secondary transition-colors"
            >
              <X className="h-3.5 w-3.5 text-muted-foreground" />
            </button>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex overflow-hidden">
        {/* Categories Sidebar */}
        <div 
          className="w-14 flex flex-col items-center py-1 overflow-y-auto"
          style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
        >
          {categories.map((cat) => {
            const Icon = cat.icon
            const isActive = activeCategory === cat.id
            return (
              <button
                key={cat.id}
                onClick={() => {
                  setActiveCategory(cat.id)
                  setSearch("")
                }}
                className="relative w-full flex flex-col items-center py-2.5 group"
              >
                {/* Active indicator */}
                <div
                  className={`absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 rounded-r-full transition-all duration-200 ${
                    isActive ? "bg-accent" : "bg-transparent"
                  }`}
                />
                
                <div
                  className={`p-2 rounded-xl transition-all duration-200 ${
                    isActive
                      ? "bg-accent/15 text-accent"
                      : "text-muted-foreground group-hover:text-foreground group-hover:bg-secondary/50"
                  }`}
                >
                  <Icon className="h-[18px] w-[18px]" />
                </div>
              </button>
            )
          })}
        </div>

        {/* Divider */}
        <div className="w-px bg-gradient-to-b from-transparent via-border/40 to-transparent my-3" />

        {/* Apps Grid */}
        <div 
          className="flex-1 overflow-y-auto px-2 pb-4"
          style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
        >
          {/* Category Header */}
          {!search && currentCategory && (
            <div className="sticky top-0 z-10 flex items-center gap-2 py-2.5 mb-1 bg-background/95 backdrop-blur-sm">
              <currentCategory.icon className="h-3.5 w-3.5 text-accent" />
              <span className="text-xs font-medium text-foreground/80">
                {currentCategory.label}
              </span>
              <div className="flex-1 h-px bg-border/30 ml-2" />
              <span className="text-[10px] text-muted-foreground/70 font-mono">
                {filteredApps.length}
              </span>
            </div>
          )}

          {search && (
            <div className="sticky top-0 z-10 flex items-center gap-2 py-2.5 mb-1 bg-background/95 backdrop-blur-sm">
              <Search className="h-3.5 w-3.5 text-muted-foreground" />
              <span className="text-xs text-muted-foreground truncate">
                &quot;{search}&quot;
              </span>
              <div className="flex-1 h-px bg-border/30 ml-2" />
              <span className="text-[10px] text-muted-foreground/70 font-mono">
                {filteredApps.length}
              </span>
            </div>
          )}

          {/* Apps */}
          <div className="grid grid-cols-3 gap-1">
            {filteredApps.map((app, idx) => {
              const Icon = app.icon
              return (
                <button
                  key={`${app.label}-${idx}`}
                  className="flex flex-col items-center gap-2 p-2.5 rounded-2xl hover:bg-secondary/40 active:scale-95 transition-all duration-150 group"
                >
                  <div
                    className={`relative w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-150 group-hover:scale-105 ${
                      app.workProfile 
                        ? "bg-orange-500/10 border border-orange-500/20" 
                        : "bg-secondary/80 border border-border/50"
                    }`}
                  >
                    <Icon
                      className={`h-5 w-5 ${
                        app.workProfile ? "text-orange-400" : "text-foreground/70"
                      }`}
                    />
                    
                    {app.workProfile && (
                      <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full bg-orange-500 border-2 border-background" />
                    )}
                  </div>
                  
                  <span className="text-[10px] text-center text-muted-foreground group-hover:text-foreground transition-colors line-clamp-1">
                    {app.label}
                  </span>
                </button>
              )
            })}
          </div>

          {/* Empty State */}
          {filteredApps.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
              <div className="w-12 h-12 rounded-xl bg-secondary/50 flex items-center justify-center mb-3">
                <Search className="h-6 w-6 opacity-30" />
              </div>
              <p className="text-xs font-medium">Sin resultados</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
