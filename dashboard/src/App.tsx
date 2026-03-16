import { useEffect, useState } from "react"
import Dashboard from "./pages/Dashboard"
import LandingPage from "./pages/LandingPage"
import ApiKeysPage from "./pages/ApiKeysPage"
import UsageLogsPage from "./pages/UsageLogsPage"
import PricingPage from "./pages/PricingPage"
import Login from "./Login"
import Signup from "./Signup"

type AuthView = "login" | "signup"
export type PageType = "landing" | "dashboard" | "apikeys" | "usage" | "pricing"

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [authView, setAuthView] = useState<AuthView>("login")
  const [currentPage, setCurrentPage] = useState<PageType>("landing")

  useEffect(() => {
    const email = localStorage.getItem("userEmail")
    const apiKey = localStorage.getItem("apiKey")
    setIsLoggedIn(Boolean(email && apiKey))
  }, [])

  function handleLoginSuccess() {
    setIsLoggedIn(true)
    setCurrentPage("dashboard")
  }

  function handleSignupSuccess() {
    setIsLoggedIn(true)
    setCurrentPage("dashboard")
  }

  function handleLogout() {
    localStorage.removeItem("userEmail")
    localStorage.removeItem("apiKey")
    setIsLoggedIn(false)
    setAuthView("login")
    setCurrentPage("landing")
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

  if (currentPage === "landing") {
    return <LandingPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "dashboard") {
    return <Dashboard onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "apikeys") {
    return <ApiKeysPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "usage") {
    return <UsageLogsPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  if (currentPage === "pricing") {
    return <PricingPage onNavigate={setCurrentPage} onLogout={handleLogout} />
  }

  return <Dashboard onNavigate={setCurrentPage} onLogout={handleLogout} />
}