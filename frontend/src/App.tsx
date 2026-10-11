import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'

type Movie = { id: number; title: string; genre: string; runtimeMinutes: number; rating: string; language: string; synopsis: string; posterTheme: string }
type Showtime = { id: number; movieId: number; movieTitle: string; startsAt: string; venue: string; screenName: string }
type Seat = { id: number; label: string; row: string; number: number; status: 'AVAILABLE' | 'HELD' | 'BOOKED'; price: number }
type User = { id: number; name: string; email: string }
type AuthResponse = { token: string; tokenType: string; expiresInSeconds: number; user: User }
type Hold = { holdToken: string; seats: string[]; expiresAt: string; holdDurationSeconds: number }
type Booking = { id: number; bookingCode: string; movieTitle: string; startsAt: string; venue: string; screenName: string; seats: string[]; totalAmount: number; bookedAt: string }
type ApiError = { message?: string }

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'
const money = (value: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value)
const dateLabel = (value: string, options?: Intl.DateTimeFormatOptions) => new Intl.DateTimeFormat('en-IN', options ?? { weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }).format(new Date(value))

async function api<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const storedToken = token ?? sessionStorage.getItem('luma-token') ?? undefined
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (storedToken) headers.set('Authorization', `Bearer ${storedToken}`)
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  const payload = await response.json().catch(() => null) as T | ApiError | null
  if (!response.ok) throw new Error((payload as ApiError | null)?.message ?? `Request failed (${response.status})`)
  return payload as T
}

const rowLetters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']

export default function App() {
  const [movies, setMovies] = useState<Movie[]>([])
  const [shows, setShows] = useState<Showtime[]>([])
  const [selectedMovie, setSelectedMovie] = useState<Movie | null>(null)
  const [activeShow, setActiveShow] = useState<Showtime | null>(null)
  const [seats, setSeats] = useState<Seat[]>([])
  const [picked, setPicked] = useState<string[]>([])
  const [hold, setHold] = useState<Hold | null>(null)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const [bookings, setBookings] = useState<Booking[]>([])
  const [token, setToken] = useState<string | null>(() => sessionStorage.getItem('luma-token'))
  const [user, setUser] = useState<User | null>(() => {
    const raw = sessionStorage.getItem('luma-user')
    try { return raw ? JSON.parse(raw) as User : null } catch { return null }
  })
  const [activePage, setActivePage] = useState<'discover' | 'tickets'>('discover')
  const [authOpen, setAuthOpen] = useState(false)
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [pendingHold, setPendingHold] = useState(false)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')

  const refreshCatalog = useCallback(async () => {
    setLoading(true)
    try {
      const [movieData, showData] = await Promise.all([api<Movie[]>('/movies'), api<Showtime[]>('/shows')])
      setMovies(movieData); setShows(showData); setError('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not connect to the cinema service.')
    } finally { setLoading(false) }
  }, [])

  const refreshSeats = useCallback(async (showtimeId: number) => {
    try { setSeats(await api<Seat[]>(`/shows/${showtimeId}/seats`)); setError('') }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not load the seat map.') }
  }, [])

  const refreshBookings = useCallback(async (authToken?: string) => {
    try { setBookings(await api<Booking[]>('/bookings/me', {}, authToken)) }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not load your tickets.') }
  }, [])

  useEffect(() => { void refreshCatalog() }, [refreshCatalog])
  useEffect(() => {
    if (selectedMovie) document.getElementById('show-picker')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [selectedMovie])
  useEffect(() => {
    if (!hold) { setSecondsLeft(0); return }
    const update = () => setSecondsLeft(Math.max(0, Math.ceil((new Date(hold.expiresAt).getTime() - Date.now()) / 1000)))
    update()
    const timer = window.setInterval(update, 1000)
    return () => window.clearInterval(timer)
  }, [hold])
  useEffect(() => {
    if (secondsLeft === 0 && hold && hold.expiresAt) {
      setHold(null); setPicked([])
      if (activeShow) void refreshSeats(activeShow.id)
    }
    // The timer reaching zero expires the local UI hold; the database enforces the actual expiry.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [secondsLeft])
  useEffect(() => {
    if (!toast) return
    const id = window.setTimeout(() => setToast(''), 3600)
    return () => window.clearTimeout(id)
  }, [toast])

  const currentShows = useMemo(() => selectedMovie ? shows.filter(show => show.movieId === selectedMovie.id) : [], [selectedMovie, shows])
  const total = useMemo(() => seats.filter(seat => picked.includes(seat.label)).reduce((sum, seat) => sum + Number(seat.price), 0), [seats, picked])
  const seatsByRow = useMemo(() => rowLetters.map(row => ({ row, seats: seats.filter(seat => seat.row === row).sort((a, b) => a.number - b.number) })).filter(group => group.seats.length), [seats])

  function chooseMovie(movie: Movie) {
    setSelectedMovie(movie); setActiveShow(null); setSeats([]); setPicked([]); setHold(null); setError('')
  }

  async function chooseShow(show: Showtime) {
    setActiveShow(show); setPicked([]); setHold(null); setError('')
    await refreshSeats(show.id)
    document.getElementById('seat-picker')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  function toggleSeat(seat: Seat) {
    if (hold || seat.status !== 'AVAILABLE') return
    setPicked(current => current.includes(seat.label) ? current.filter(label => label !== seat.label) : current.length >= 8 ? current : [...current, seat.label])
  }

  async function createHold(authToken = token) {
    if (!activeShow || picked.length === 0) return
    if (!authToken) { setPendingHold(true); setAuthMode('login'); setAuthOpen(true); return }
    setBusy(true); setError('')
    try {
      const newHold = await api<Hold>(`/shows/${activeShow.id}/holds`, { method: 'POST', body: JSON.stringify({ seats: picked }) }, authToken)
      setHold(newHold); setToast(`Seats ${newHold.seats.join(', ')} are held for five minutes.`)
      await refreshSeats(activeShow.id)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not hold these seats.')
      await refreshSeats(activeShow.id)
    } finally { setBusy(false) }
  }

  async function submitAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const path = authMode === 'register' ? '/auth/register' : '/auth/login'
      const body = authMode === 'register' ? { name, email, password } : { email, password }
      const auth = await api<AuthResponse>(path, { method: 'POST', body: JSON.stringify(body) }, '')
      sessionStorage.setItem('luma-token', auth.token); sessionStorage.setItem('luma-user', JSON.stringify(auth.user))
      setToken(auth.token); setUser(auth.user); setAuthOpen(false); setPassword(''); setToast(`Welcome${auth.user.name ? `, ${auth.user.name.split(' ')[0]}` : ''}. You're signed in.`)
      if (pendingHold) { setPendingHold(false); await createHold(auth.token) }
      await refreshBookings(auth.token)
    } catch (e) { setError(e instanceof Error ? e.message : 'Authentication failed.') }
    finally { setBusy(false) }
  }

  async function releaseHold() {
    if (!hold || !token) return
    setBusy(true); setError('')
    try {
      await api<void>(`/holds/${hold.holdToken}`, { method: 'DELETE' }, token)
      setHold(null); setPicked([]); setToast('Your seats have been released.')
      if (activeShow) await refreshSeats(activeShow.id)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not release these seats.')
    } finally { setBusy(false) }
  }

  async function confirmBooking() {
    if (!hold || !token) return
    setBusy(true); setError('')
    try {
      const booking = await api<Booking>('/holds/confirm', { method: 'POST', body: JSON.stringify({ holdToken: hold.holdToken }) }, token)
      setHold(null); setPicked([]); setToast(`Booking confirmed · ${booking.bookingCode}`)
      if (activeShow) await refreshSeats(activeShow.id)
      await refreshBookings(token)
      setActivePage('tickets')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not confirm your booking.')
      if (activeShow) await refreshSeats(activeShow.id)
    } finally { setBusy(false) }
  }

  function signOut() {
    sessionStorage.removeItem('luma-token'); sessionStorage.removeItem('luma-user')
    setToken(null); setUser(null); setBookings([]); setHold(null); setPendingHold(false); setToast('You have signed out.')
  }

  async function showTickets() {
    if (!token) { setAuthMode('login'); setAuthOpen(true); return }
    setActivePage('tickets'); await refreshBookings(token)
    document.getElementById('main-content')?.scrollIntoView({ behavior: 'smooth' })
  }

  return <div className="app-shell">
    <header className="site-header">
      <a className="brand" href="#top" onClick={() => setActivePage('discover')} aria-label="Luma home"><span className="brand-mark">L</span><span>LUMA<span className="brand-period">.</span></span></a>
      <nav className="main-nav" aria-label="Main navigation">
        <button className={activePage === 'discover' ? 'nav-link active' : 'nav-link'} onClick={() => setActivePage('discover')}>Discover</button>
        <button className={activePage === 'tickets' ? 'nav-link active' : 'nav-link'} onClick={() => void showTickets()}>My tickets</button>
      </nav>
      <div className="header-account">
        {user ? <><span className="user-greeting">Hey, {user.name.split(' ')[0]}</span><button className="avatar-button" onClick={signOut} title="Sign out">{user.name.charAt(0).toUpperCase()}</button></> : <button className="button button-outline header-signin" onClick={() => { setAuthMode('login'); setAuthOpen(true) }}>Sign in <span aria-hidden>↗</span></button>}
      </div>
    </header>

    <main id="main-content">
      {activePage === 'discover' ? <>
        <section className="hero" id="top">
          <div className="hero-glow glow-one"/><div className="hero-glow glow-two"/>
          <div className="hero-copy">
            <div className="eyebrow"><span className="eyebrow-dot"/> YOUR NEXT GREAT NIGHT OUT</div>
            <h1>Stories hit<br/><span>different</span> on the<br/>big screen.</h1>
            <p>Find the film. Pick your place. Make it a night.</p>
            <button className="button button-primary hero-cta" onClick={() => document.getElementById('now-showing')?.scrollIntoView({ behavior: 'smooth' })}>Explore movies <span>↓</span></button>
            <div className="hero-footnote"><span className="tiny-stars">✦ ✦ ✦</span> The little details make the night.</div>
          </div>
          <div className="hero-art" aria-hidden="true">
            <div className="ticket ticket-back"><span>ADMIT ONE</span><b>✦</b><small>ROW C · SEAT 08</small></div>
            <div className="hero-poster"><div className="poster-orbit orbit-a"/><div className="poster-orbit orbit-b"/><div className="poster-sun"/><div className="poster-mountain mountain-a"/><div className="poster-mountain mountain-b"/><div className="poster-caption"><span>TONIGHT, WE ESCAPE</span><strong>THE<br/>UNKNOWN</strong><small>A LUMA ORIGINAL EXPERIENCE</small></div></div>
            <div className="hero-art-label"><span className="label-line"/> SCREEN MAGIC, MADE SIMPLE</div>
          </div>
          <div className="hero-bottom"><span>01 / DISCOVER</span><span>MADE FOR THE MOMENT <b>✳</b></span><span>SCROLL TO EXPLORE ↓</span></div>
        </section>

        <section className="content-section movies-section" id="now-showing">
          <div className="section-heading"><div><span className="section-kicker">ON THE MARQUEE</span><h2>Now showing<span className="heading-dot">.</span></h2></div><span className="section-aside">A good story is waiting.</span></div>
          {error && <div className="alert alert-error" role="alert"><span>!</span><div><strong>Something needs a second take.</strong><p>{error}</p></div><button onClick={() => void refreshCatalog()}>Retry</button></div>}
          {loading ? <div className="loading-grid">{[1, 2, 3].map(i => <div className="skeleton-card" key={i}/>)}</div> : movies.length === 0 ? <div className="empty-state"><span>✳</span><h3>No showtimes on the board yet.</h3><p>Check that the API and MySQL container are running, then retry.</p><button className="button button-outline" onClick={() => void refreshCatalog()}>Reload catalogue</button></div> : <div className="movie-grid">
            {movies.map((movie, index) => <article className={`movie-card theme-${movie.posterTheme}`} key={movie.id}>
              <div className={`movie-poster poster-theme-${movie.posterTheme}`}>
                <div className="poster-grain"/><div className="poster-kicker">LUMA PICTURES <span>№ 0{index + 1}</span></div>
                <div className={`movie-art art-${movie.posterTheme}`}><span className="art-orbit"/><span className="art-disc"/><span className="art-ridge ridge-one"/><span className="art-ridge ridge-two"/><span className="art-star">✦</span></div>
                <div className="poster-bottom"><span>{movie.genre.split('/')[0].trim().toUpperCase()}</span><span>{movie.runtimeMinutes} MIN</span></div>
                <div className="rating-stamp">{movie.rating}</div>
              </div>
              <div className="movie-details"><div className="movie-title-line"><h3>{movie.title}</h3><span className="movie-language">{movie.language}</span></div><p>{movie.genre} <span>·</span> {movie.runtimeMinutes} min</p><p className="movie-synopsis">{movie.synopsis}</p><button className="text-action" onClick={() => chooseMovie(movie)}>Find a showtime <span>↗</span></button></div>
            </article>)}
          </div>}
        </section>

        {selectedMovie && <section className="content-section booking-section" id="show-picker">
          <div className="section-heading"><div><span className="section-kicker">YOUR NIGHT, YOUR CALL</span><h2>Pick a show<span className="heading-dot">.</span></h2></div><button className="subtle-action" onClick={() => { setSelectedMovie(null); setActiveShow(null); setHold(null) }}>Close ×</button></div>
          <div className="selected-film-banner"><div className={`mini-poster poster-theme-${selectedMovie.posterTheme}`}><span>{selectedMovie.title.split(' ').map(w => w[0]).slice(0, 3).join('')}</span></div><div><span className="section-kicker">YOU'RE WATCHING</span><h3>{selectedMovie.title}</h3><p>{selectedMovie.genre} · {selectedMovie.runtimeMinutes} min · {selectedMovie.rating}</p></div></div>
          {currentShows.length === 0 ? <div className="empty-state compact"><p>No upcoming showtimes are available for this movie.</p></div> : <div className="showtime-grid">{currentShows.map(show => <button key={show.id} className={`showtime-card ${activeShow?.id === show.id ? 'chosen' : ''}`} onClick={() => void chooseShow(show)}><span className="show-date">{dateLabel(show.startsAt, { weekday: 'short', day: 'numeric', month: 'short' })}</span><strong>{dateLabel(show.startsAt, { hour: 'numeric', minute: '2-digit' })}</strong><span>{show.screenName}</span><small>{show.venue}</small><span className="show-arrow">↗</span></button>)}</div>}
        </section>}

        {activeShow && <section className="content-section seat-section" id="seat-picker">
          <div className="section-heading"><div><span className="section-kicker">THE BEST SEAT IS YOURS</span><h2>Choose your seats<span className="heading-dot">.</span></h2><p className="section-subtitle">{activeShow.movieTitle} · {dateLabel(activeShow.startsAt)} · {activeShow.screenName}</p></div><span className="seat-limit">UP TO 8 SEATS</span></div>
          <div className="seat-layout">
            <div className="seat-main"><div className="screen-wrap"><div className="screen-glow"/><div className="screen-shape"/><span>SCREEN THIS WAY</span></div>
              <div className="seat-map">{seatsByRow.map(group => <div className="seat-row" key={group.row}><span className="row-label">{group.row}</span><div className="seat-row-items">{group.seats.map(seat => <button key={seat.label} aria-label={`Seat ${seat.label}, ${seat.status.toLowerCase()}`} title={`${seat.label} · ${money(Number(seat.price))}`} disabled={Boolean(hold) || seat.status !== 'AVAILABLE'} className={`seat ${seat.status === 'BOOKED' ? 'booked' : seat.status === 'HELD' ? 'held' : picked.includes(seat.label) ? 'selected' : ''}`} onClick={() => toggleSeat(seat)}>{seat.number}</button>)}</div><span className="row-label">{group.row}</span></div>)}</div>
              <div className="seat-legend"><span><i className="legend-seat available"/> Available</span><span><i className="legend-seat selected"/> Selected</span><span><i className="legend-seat held"/> On hold</span><span><i className="legend-seat booked"/> Booked</span></div>
              <p className="seat-help">Front rows A–B · Standard C–F · Value G–H</p>
            </div>
            <aside className="order-summary"><span className="section-kicker">YOUR ORDER</span><h3>{hold ? 'Seats are held!' : picked.length ? `${picked.length} seat${picked.length > 1 ? 's' : ''} selected` : 'Make yourself at home.'}</h3><p className="summary-show-name">{activeShow.movieTitle}</p><p className="summary-show-time">{dateLabel(activeShow.startsAt)}<br/>{activeShow.venue}</p>
              <div className="summary-seats"><span>Selected seats</span><strong>{hold ? hold.seats.join(', ') : picked.length ? [...picked].sort().join(', ') : '—'}</strong></div>
              <div className="summary-total"><span>Total</span><strong>{money(hold ? seats.filter(seat => hold.seats.includes(seat.label)).reduce((sum, seat) => sum + Number(seat.price), 0) : total)}</strong></div>
              {hold ? <><div className={`hold-clock ${secondsLeft < 60 ? 'urgent' : ''}`}><span>◷</span><div><strong>{Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, '0')}</strong><small>Time left to confirm</small></div></div><button className="button button-primary full-width" disabled={busy || secondsLeft === 0} onClick={() => void confirmBooking()}>{busy ? 'Confirming…' : 'Confirm booking'} <span>↗</span></button><button className="text-action cancel-hold" onClick={() => void releaseHold()} disabled={busy}>Release seats</button></> : <button className="button button-primary full-width" disabled={busy || picked.length === 0} onClick={() => void createHold()}>{busy ? 'Holding seats…' : `Hold ${picked.length || ''} seat${picked.length === 1 ? '' : 's'}`} <span>↗</span></button>}
              <p className="secure-note"><span>✳</span> Seats are temporarily reserved for 5 minutes. No charge is taken in this demo.</p>
            </aside>
          </div>
        </section>}
        <section className="closing-banner"><span className="closing-star">✳</span><div><span className="section-kicker">A LITTLE ESCAPE GOES A LONG WAY</span><h2>Make room for a story.</h2><p>Your next favourite memory might be two hours away.</p></div><button className="button button-light" onClick={() => { setSelectedMovie(null); setActiveShow(null); document.getElementById('now-showing')?.scrollIntoView({ behavior: 'smooth' }) }}>Browse all films <span>↑</span></button></section>
      </> : <section className="content-section tickets-page"><div className="section-heading"><div><span className="section-kicker">YOUR PERSONAL MARQUEE</span><h2>My tickets<span className="heading-dot">.</span></h2></div><button className="subtle-action" onClick={() => setActivePage('discover')}>← Discover</button></div>
        {!user ? <div className="empty-state"><span>✳</span><h3>Sign in to see your tickets.</h3><button className="button button-primary" onClick={() => setAuthOpen(true)}>Sign in</button></div> : bookings.length === 0 ? <div className="empty-state"><span>✳</span><h3>Your next cinema night starts here.</h3><p>No bookings yet. Find a film and reserve your seats.</p><button className="button button-primary" onClick={() => setActivePage('discover')}>Explore movies</button></div> : <div className="tickets-list">{bookings.map(booking => <article className="ticket-card" key={booking.id}><div className="ticket-card-main"><span className="section-kicker">BOOKING CONFIRMED</span><h3>{booking.movieTitle}</h3><p>{dateLabel(booking.startsAt)} · {booking.screenName}</p><p>{booking.venue}</p><div className="ticket-seat-tags">{booking.seats.map(seat => <span key={seat}>{seat}</span>)}</div></div><div className="ticket-card-side"><span>BOOKING CODE</span><strong>{booking.bookingCode}</strong><span>TOTAL PAID</span><strong>{money(Number(booking.totalAmount))}</strong></div></article>)}</div>}
      </section>}
    </main>

    <footer className="site-footer"><a className="brand footer-brand" href="#top"><span className="brand-mark">L</span><span>LUMA<span className="brand-period">.</span></span></a><span>EVERY STORY DESERVES A BIG SCREEN.</span><span>© 2026 LUMA CINEMA</span></footer>

    {authOpen && <div className="modal-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setAuthOpen(false) }}><section className="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-title"><button className="modal-close" aria-label="Close" onClick={() => setAuthOpen(false)}>×</button><span className="section-kicker">YOUR SEAT IS WAITING</span><h2 id="auth-title">{authMode === 'login' ? 'Welcome back.' : 'Join the story.'}</h2><p>{authMode === 'login' ? 'Sign in to hold seats and keep your tickets together.' : 'Make an account to save your next movie night.'}</p><form onSubmit={submitAuth} className="auth-form">{authMode === 'register' && <label>Your name<input value={name} onChange={e => setName(e.target.value)} required maxLength={100} autoComplete="name" placeholder="Alex Morgan"/></label>}<label>Email address<input type="email" value={email} onChange={e => setEmail(e.target.value)} required maxLength={190} autoComplete="email" placeholder="you@example.com"/></label><label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={authMode === 'register' ? 8 : 1} maxLength={72} autoComplete={authMode === 'register' ? 'new-password' : 'current-password'} placeholder={authMode === 'register' ? 'At least 8 characters' : 'Your password'}/></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="button button-primary full-width" type="submit" disabled={busy}>{busy ? 'One moment…' : authMode === 'login' ? 'Sign in' : 'Create account'} <span>↗</span></button></form><p className="auth-switch">{authMode === 'login' ? 'New to Luma?' : 'Already have an account?'} <button onClick={() => { setAuthMode(authMode === 'login' ? 'register' : 'login'); setError('') }}>{authMode === 'login' ? 'Create an account' : 'Sign in'}</button></p><p className="auth-smallprint">Demo project · Passwords are hashed server-side · JWT authentication</p></section></div>}
    {toast && <div className="toast" role="status"><span>✓</span>{toast}<button aria-label="Dismiss notification" onClick={() => setToast('')}>×</button></div>}
  </div>
}
