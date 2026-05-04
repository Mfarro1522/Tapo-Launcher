"use client"

import { type LucideIcon } from "lucide-react"
import { cn } from "@/lib/utils"

interface AppIconProps {
  icon: LucideIcon
  label: string
  color?: string
  onClick?: () => void
  notification?: number
}

export function AppIcon({ icon: Icon, label, color = "bg-secondary", onClick, notification }: AppIconProps) {
  return (
    <button
      onClick={onClick}
      className="flex flex-col items-center gap-2 p-2 transition-all active:scale-90"
    >
      <div className="relative">
        <div
          className={cn(
            "w-14 h-14 rounded-2xl flex items-center justify-center transition-all",
            color
          )}
        >
          <Icon className="h-6 w-6 text-foreground" />
        </div>
        {notification && notification > 0 && (
          <span className="absolute -top-1 -right-1 min-w-5 h-5 px-1.5 bg-accent text-accent-foreground text-xs font-medium rounded-full flex items-center justify-center">
            {notification > 99 ? "99+" : notification}
          </span>
        )}
      </div>
      <span className="text-xs text-foreground/70 font-mono truncate max-w-16">{label}</span>
    </button>
  )
}
