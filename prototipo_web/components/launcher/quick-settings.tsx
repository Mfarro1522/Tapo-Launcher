"use client"

import { useState } from "react"
import { Wifi, Bluetooth, Moon, FlashlightOff, Flashlight, Volume2, VolumeX, RotateCcw } from "lucide-react"
import { cn } from "@/lib/utils"

interface QuickTileProps {
  icon: React.ReactNode
  activeIcon?: React.ReactNode
  label: string
  active?: boolean
  onToggle?: () => void
  color?: string
}

function QuickTile({ icon, activeIcon, label, active, onToggle, color = "accent" }: QuickTileProps) {
  const colorClasses: Record<string, string> = {
    accent: "bg-gradient-to-b from-accent/30 to-accent/10 text-accent border-accent/20",
    blue: "bg-gradient-to-b from-blue-500/30 to-blue-500/10 text-blue-400 border-blue-500/20",
    purple: "bg-gradient-to-b from-violet-500/30 to-violet-500/10 text-violet-400 border-violet-500/20",
    amber: "bg-gradient-to-b from-amber-500/30 to-amber-500/10 text-amber-400 border-amber-500/20",
    rose: "bg-gradient-to-b from-rose-500/30 to-rose-500/10 text-rose-400 border-rose-500/20",
    emerald: "bg-gradient-to-b from-emerald-500/30 to-emerald-500/10 text-emerald-400 border-emerald-500/20",
  }

  return (
    <button
      onClick={onToggle}
      className={cn(
        "relative flex flex-col items-center gap-2 p-3.5 rounded-2xl transition-all duration-200 active:scale-95 border",
        active
          ? colorClasses[color]
          : "bg-secondary/40 text-muted-foreground border-white/5 hover:bg-secondary/60"
      )}
    >
      {/* Glow effect when active */}
      {active && (
        <div className="absolute inset-0 bg-current opacity-5 rounded-2xl blur-xl" />
      )}
      
      <div className="relative">
        {active && activeIcon ? activeIcon : icon}
      </div>
      <span className="text-[10px] font-medium tracking-wide uppercase">{label}</span>
    </button>
  )
}

export function QuickSettings() {
  const [settings, setSettings] = useState({
    wifi: true,
    bluetooth: false,
    dnd: false,
    flashlight: false,
    sound: true,
    rotation: true,
  })

  const toggle = (key: keyof typeof settings) => {
    setSettings((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  return (
    <div className="px-5">
      <div className="grid grid-cols-4 gap-2">
        <QuickTile
          icon={<Wifi className="h-5 w-5" />}
          label="WiFi"
          active={settings.wifi}
          onToggle={() => toggle("wifi")}
          color="blue"
        />
        <QuickTile
          icon={<Bluetooth className="h-5 w-5" />}
          label="BT"
          active={settings.bluetooth}
          onToggle={() => toggle("bluetooth")}
          color="blue"
        />
        <QuickTile
          icon={<Moon className="h-5 w-5" />}
          label="DND"
          active={settings.dnd}
          onToggle={() => toggle("dnd")}
          color="purple"
        />
        <QuickTile
          icon={<FlashlightOff className="h-5 w-5" />}
          activeIcon={<Flashlight className="h-5 w-5" />}
          label="Light"
          active={settings.flashlight}
          onToggle={() => toggle("flashlight")}
          color="amber"
        />
        <QuickTile
          icon={<VolumeX className="h-5 w-5" />}
          activeIcon={<Volume2 className="h-5 w-5" />}
          label="Sound"
          active={settings.sound}
          onToggle={() => toggle("sound")}
          color="rose"
        />
        <QuickTile
          icon={<RotateCcw className="h-5 w-5" />}
          label="Rotate"
          active={settings.rotation}
          onToggle={() => toggle("rotation")}
          color="emerald"
        />
      </div>
    </div>
  )
}
