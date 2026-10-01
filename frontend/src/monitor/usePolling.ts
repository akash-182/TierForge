import { useEffect, useRef } from 'react'

/**
 * Calls `tick` immediately and then every `intervalMs`, skipping ticks while the browser tab is
 * hidden (and refreshes as soon as it is visible again). Failures are swallowed — the next tick simply retries.
 */
export function usePolling(tick: () => Promise<void> | void, intervalMs: number, enabled = true) {
  const latestTick = useRef(tick)
  useEffect(() => {
    latestTick.current = tick
  })

  useEffect(() => {
    if (!enabled) {
      return
    }
    const run = async () => {
      if (document.hidden) {
        return
      }
      try {
        await latestTick.current()
      } catch {
        // transient failure — retry on the next tick
      }
    }
    void run()
    const id = setInterval(run, intervalMs)
    // Refresh straight away when the tab becomes visible again instead of waiting for the next tick.
    const onVisible = () => void run()
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      clearInterval(id)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [intervalMs, enabled])
}
