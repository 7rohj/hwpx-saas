import { useEffect, useState } from "react"
import Dashboard from "./pages/Dashboard"
import Login from "./Login"
import Signup from "./Signup"

type AuthView = "login" | "signup"

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [authView, setAuthView] = useState<AuthView>("login")

  useEffect(() => {
    const email = localStorage.getItem("userEmail")
    const apiKey = localStorage.getItem("apiKey")
    setIsLoggedIn(Boolean(email && apiKey))
  }, [])

  function handleLoginSuccess() {
    setIsLoggedIn(true)
  }

  function handleSignupSuccess() {
    setIsLoggedIn(true)
  }

  function handleLogout() {
    localStorage.removeItem("userEmail")
    localStorage.removeItem("apiKey")
    setIsLoggedIn(false)
    setAuthView("login")
  }

  if (!isLoggedIn) {
    if (authView === "signup") {
      return (
        <Signup
          onSignupSuccess={handleSignupSuccess}
          onGoLogin={() => setAuthView("login")}
        />
      )
    }

    return (
      <Login
        onLoginSuccess={handleLoginSuccess}
        onGoSignup={() => setAuthView("signup")}
      />
    )
  }

  return (
    <div>
      <div className="fixed right-4 top-4 z-50">
        <button
          onClick={handleLogout}
          className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm shadow-sm hover:bg-slate-50"
        >
          로그아웃
        </button>
      </div>
      <Dashboard />
    </div>
  )
}