"use client"

import { useEffect, useState } from "react"
import { Calendar, Cpu, HardDrive, Activity, Zap } from "lucide-react"

function ClockWidget() {
  const [mounted, setMounted] = useState(false)
  const [time, setTime] = useState(new Date())

  useEffect(() => {
    setMounted(true)
    const interval = setInterval(() => setTime(new Date()), 1000)
    return () => clearInterval(interval)
  }, [])

  const hours = mounted ? time.getHours().toString().padStart(2, "0") : "--"
  const minutes = mounted ? time.getMinutes().toString().padStart(2, "0") : "--"
  const seconds = mounted ? time.getSeconds().toString().padStart(2, "0") : "--"
  const date = mounted
    ? time.toLocaleDateString("es-ES", {
        weekday: "long",
        day: "numeric",
        month: "long",
      })
    : ""

  return (
    <div className="px-5">
      <div className="relative text-center py-10">
        {/* Glow effect */}
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <div className="w-48 h-48 bg-accent/5 rounded-full blur-3xl" />
        </div>
        
        {/* Time */}
        <div className="relative">
          <div className="text-8xl font-extralight tracking-[-0.05em] text-foreground font-mono tabular-nums">
            {hours}
            <span className="text-accent animate-pulse">:</span>
            {minutes}
          </div>
          <div className="text-lg font-mono text-muted-foreground/50 mt-1 tabular-nums">
            :{seconds}
          </div>
        </div>
        
        {/* Date */}
        <div className="relative mt-4 text-sm text-muted-foreground capitalize tracking-wide">
          {date}
        </div>
      </div>
    </div>
  )
}

function SystemWidget() {
  const stats = [
    { icon: Cpu, label: "CPU", value: "23", unit: "%", color: "from-emerald-500/20 to-emerald-500/5" },
    { icon: HardDrive, label: "RAM", value: "4.2", unit: "GB", color: "from-blue-500/20 to-blue-500/5" },
    { icon: Activity, label: "BAT", value: "78", unit: "%", color: "from-amber-500/20 to-amber-500/5" },
  ]

  return (
    <div className="px-5">
      <div className="grid grid-cols-3 gap-2">
        {stats.map((stat) => (
          <div
            key={stat.label}
            className={`relative flex flex-col items-center gap-2 p-4 rounded-2xl bg-gradient-to-b ${stat.color} border border-white/5 overflow-hidden`}
          >
            {/* Icon */}
            <stat.icon className="h-5 w-5 text-foreground/70" />
            
            {/* Value */}
            <div className="flex items-baseline gap-0.5">
              <span className="text-xl font-semibold font-mono tabular-nums text-foreground">
                {stat.value}
              </span>
              <span className="text-xs text-muted-foreground font-mono">
                {stat.unit}
              </span>
            </div>
            
            {/* Label */}
            <span className="text-[10px] font-mono text-muted-foreground uppercase tracking-wider">
              {stat.label}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

function UpcomingWidget() {
  const events = [
    { time: "10:00", title: "Team standup", accent: true },
    { time: "14:30", title: "Code review", accent: false },
    { time: "16:00", title: "Deploy v2.1", accent: false },
  ]

  return (
    <div className="px-5">
      <div className="relative p-4 rounded-2xl bg-gradient-to-b from-secondary/50 to-secondary/20 border border-white/5 overflow-hidden">
        {/* Header */}
        <div className="flex items-center gap-2 mb-4">
          <div className="p-1.5 rounded-lg bg-accent/10">
            <Calendar className="h-4 w-4 text-accent" />
          </div>
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Hoy
          </span>
          <div className="flex-1 h-px bg-gradient-to-r from-border/50 to-transparent" />
          <Zap className="h-3 w-3 text-accent" />
        </div>
        
        {/* Events */}
        <div className="space-y-3">
          {events.map((event) => (
            <div key={event.title} className="flex items-center gap-3 group">
              {/* Indicator */}
              <div
                className={`w-1 h-10 rounded-full transition-all ${
                  event.accent
                    ? "bg-gradient-to-b from-accent to-accent/30"
                    : "bg-gradient-to-b from-muted-foreground/30 to-transparent"
                }`}
              />
              
              {/* Content */}
              <div className="flex-1 min-w-0">
                <span className="text-[10px] font-mono text-muted-foreground/70 uppercase tracking-wider">
                  {event.time}
                </span>
                <p className={`text-sm truncate ${event.accent ? "text-foreground font-medium" : "text-muted-foreground"}`}>
                  {event.title}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export function HomeWidgets() {
  return (
    <div className="space-y-5">
      <ClockWidget />
      <SystemWidget />
      <UpcomingWidget />
    </div>
  )
}
