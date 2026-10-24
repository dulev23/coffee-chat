import Header from './components/Header'
import { useHashRoute } from './hooks/useHashRoute'
import BookingPage from './pages/BookingPage'
import HostPage from './pages/HostPage'

export default function App() {
  const route = useHashRoute()

  return (
    <div className="shell">
      <Header route={route} />
      <main>{route === 'host' ? <HostPage /> : <BookingPage />}</main>
    </div>
  )
}
