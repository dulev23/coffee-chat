import { useEffect, useState } from 'react'

export type Route = 'booking' | 'host'

const currentRoute = (): Route => (window.location.hash === '#host' ? 'host' : 'booking')

export function useHashRoute() {
  const [route, setRoute] = useState(currentRoute)

  useEffect(() => {
    const onHashChange = () => setRoute(currentRoute())
    window.addEventListener('hashchange', onHashChange)
    return () => window.removeEventListener('hashchange', onHashChange)
  }, [])

  return route
}
