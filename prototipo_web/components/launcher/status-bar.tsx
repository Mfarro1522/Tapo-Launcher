"use client"

import { useEffect, useState } from "react"
import { Wifi, Signal, BatteryMedium } from "lucide-react"

export function StatusBar() {
  const [time, setTime] = useState<string | null>(null)

  useEffect(() => {
    const updateTime = () => {
      setTime(
        new Date().toLocaleTimeString("es-ES", {
          hour: "2-digit",
          minute: "2-digit",
          hour12: false,
        })
      )
    }
    updateTime()
    const interval = setInterval(updateTime, 1000)
    return () => clearInterval(interval)
  }, [])

  return (
    <div className="flex items-center justify-between px-5 py-2 text-foreground/80">
      <span className="text-xs font-mono tracking-tight">{time ?? "--:--"}</span>
      <div className="flex items-center gap-1.5">
        <Signal className="h-3.5 w-3.5" />
        <Wifi className="h-3.5 w-3.5" />
        <BatteryMedium className="h-4 w-4" />
      </div>
    </div>
  )
}
