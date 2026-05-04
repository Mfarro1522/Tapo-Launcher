"use client"

import { Search, Sparkles } from "lucide-react"

interface SearchBarProps {
  onFocus?: () => void
}

export function SearchBar({ onFocus }: SearchBarProps) {
  return (
    <div className="px-5">
      <button
        onClick={onFocus}
        className="group w-full relative"
      >
        {/* Glow effect on hover */}
        <div className="absolute inset-0 bg-gradient-to-r from-accent/10 via-transparent to-accent/5 rounded-2xl blur-xl opacity-0 group-hover:opacity-100 transition-opacity" />
        
        {/* Search bar */}
        <div className="relative flex items-center gap-3 px-4 py-3.5 bg-secondary/40 border border-white/5 rounded-2xl text-muted-foreground transition-all group-hover:border-accent/20 group-hover:bg-secondary/60 active:scale-[0.98]">
          <Search className="h-5 w-5 text-muted-foreground/70 group-hover:text-accent transition-colors" />
          <span className="text-sm flex-1 text-left text-muted-foreground/70 group-hover:text-muted-foreground transition-colors">
            Buscar aplicaciones...
          </span>
          <div className="flex items-center gap-1.5 px-2 py-1 rounded-lg bg-secondary/50 text-[10px] font-mono text-muted-foreground/50">
            <Sparkles className="h-3 w-3" />
            <span>AI</span>
          </div>
        </div>
      </button>
    </div>
  )
}
