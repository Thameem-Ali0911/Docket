import { useEffect, useState } from 'react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function App() {
    const [health, setHealth] = useState('checking...')
    const [error, setError] = useState(null)

    useEffect(() => {
        fetch(`${API_BASE_URL}/api/health`)
            .then((res) => {
                if (!res.ok) throw new Error(`Request failed: ${res.status}`)
                return res.text()
            })
            .then((text) => setHealth(text))
            .catch((err) => setError(err.message))
    }, [])

    return (
        <section id="center">
            <div>
                <h1>Docket</h1>
                <p>
                    Backend health check:{' '}
                    <strong>{error ? `error — ${error}` : health}</strong>
                </p>
            </div>
        </section>
    )
}

export default App
