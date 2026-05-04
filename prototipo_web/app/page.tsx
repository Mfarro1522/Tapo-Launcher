"use client"

import { useState, useEffect } from "react"
import { LauncherScreen } from "@/components/launcher/launcher-screen"
import { Sun, Moon } from "lucide-react"

export default function Home() {
  const [isDark, setIsDark] = useState(true)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
    document.documentElement.classList.add("dark")
  }, [])

  const toggleTheme = () => {
    const newIsDark = !isDark
    setIsDark(newIsDark)
    if (newIsDark) {
      document.documentElement.classList.add("dark")
    } else {
      document.documentElement.classList.remove("dark")
    }
  }

  return (
    <main className="min-h-screen flex items-center justify-center p-4 relative">
      {/* Theme Toggle Button */}
      {mounted && (
        <button
          onClick={toggleTheme}
          className="absolute top-4 right-4 p-3 rounded-full bg-card border border-border hover:bg-secondary transition-colors z-50"
          aria-label="Cambiar tema"
        >
          {isDark ? (
            <Sun className="w-5 h-5 text-foreground" />
          ) : (
            <Moon className="w-5 h-5 text-foreground" />
          )}
        </button>
      )}

      {/* Phone Frame - Mobile First */}
      <div className="w-full max-w-[390px] h-[844px] bg-background border border-border rounded-[3rem] overflow-hidden shadow-2xl shadow-black/50 relative">
        {/* Notch */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-32 h-7 bg-background rounded-b-3xl z-10 flex items-center justify-center">
          <div className="w-16 h-4 bg-card rounded-full" />
        </div>
        
        {/* Screen Content */}
        <div className="h-full pt-7">
          <LauncherScreen />
        </div>
      </div>
    </main>
  )
}
