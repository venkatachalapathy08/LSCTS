
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../api/authApi";

function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    try {
      setLoading(true);

      const data = await login(email.trim(), password);

      if (data.role !== "ADMIN" && data.role !== "USER") {
        throw new Error(
          "Unknown user role. Please contact the administrator."
        );
      }

      localStorage.setItem("token", data.token);
      localStorage.setItem("role", data.role);
      localStorage.setItem("userId", String(data.id));
      localStorage.setItem("name", data.name);

      if (data.role === "ADMIN") {
        navigate("/admin", { replace: true });
      } else {
        navigate("/user", { replace: true });
      }
    } catch (err) {
      setError(err.message || "Unable to log in.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="setup-container">
      <form className="setup-card" onSubmit={handleSubmit}>
        <h1>LSCTS Login</h1>
        <p>Local Service Complaint and Tracking System</p>

        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          autoComplete="username"
          required
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
          required
        />

        <div className="forgot-password-link">
          <Link to="/forgot-password">
            Forgot Password?
          </Link>
        </div>

        {error && <p className="error-message">{error}</p>}

        <button type="submit" disabled={loading}>
          {loading ? "Logging in..." : "Login"}
        </button>
      </form>
    </main>
  );
}

export default Login;
