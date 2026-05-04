"use client"

import { Phone, MessageSquare, Chrome, Camera } from "lucide-react"

export function Dock() {
  const dockApps = [
    { icon: Phone, label: "Phone", color: "from-green-500/20 to-green-500/5" },
    { icon: MessageSquare, label: "Messages", color: "from-blue-500/20 to-blue-500/5" },
    { icon: Chrome, label: "Browser", color: "from-amber-500/20 to-amber-500/5" },
    { icon: Camera, label: "Camera", color: "from-rose-500/20 to-rose-500/5" },
  ]

  return (
    <div className="px-5 pb-2">
      <div className="relative">
        {/* Glow */}
        <div className="absolute inset-0 bg-gradient-to-t from-accent/5 to-transparent rounded-3xl blur-xl" />
        
        {/* Dock */}
        <div className="relative flex items-center justify-around py-3 px-3 bg-secondary/40 backdrop-blur-xl border border-white/5 rounded-3xl">
          {dockApps.map((app) => (
            <button
              key={app.label}
              className="group relative p-3 rounded-2xl transition-all duration-200 active:scale-90"
            >
              {/* Icon bg */}
              <div className={`absolute inset-0 bg-gradient-to-b ${app.color} rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity`} />
              
              {/* Icon */}
              <app.icon className="relative h-6 w-6 text-foreground/80 group-hover:text-foreground transition-colors" />
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
