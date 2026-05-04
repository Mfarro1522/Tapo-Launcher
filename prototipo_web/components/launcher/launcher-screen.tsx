"use client"

import { useState } from "react"
import { StatusBar } from "./status-bar"
import { AppDrawer } from "./app-drawer"
import { QuickSettings } from "./quick-settings"
import { ChevronDown } from "lucide-react"

export function LauncherScreen() {
  const [showQuickSettings, setShowQuickSettings] = useState(false)

  return (
    <div className="relative h-full flex flex-col bg-background overflow-hidden">
      {/* Status Bar */}
      <StatusBar />

      {/* Quick Settings Toggle */}
      <div className="px-5">
        <button
          onClick={() => setShowQuickSettings(!showQuickSettings)}
          className="w-full flex items-center justify-center py-1 text-muted-foreground/40 hover:text-muted-foreground/60 transition-colors"
        >
          <ChevronDown
            className={`h-4 w-4 transition-transform duration-300 ${showQuickSettings ? "rotate-180" : ""}`}
          />
        </button>
      </div>

      {/* Quick Settings Panel */}
      <div
        className={`overflow-hidden transition-all duration-300 ease-out ${
          showQuickSettings ? "max-h-44 opacity-100" : "max-h-0 opacity-0"
        }`}
      >
        <QuickSettings />
      </div>

      {/* App Drawer - Main Content */}
      <div className="flex-1 overflow-hidden">
        <AppDrawer />
      </div>

      {/* Home Indicator */}
      <div className="flex justify-center py-2">
        <div className="w-28 h-1 bg-foreground/15 rounded-full" />
      </div>
    </div>
  )
}
