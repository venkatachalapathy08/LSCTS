
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { setupPassword } from "../api/authApi";

function SetupPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const token = searchParams.get("token");

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();

    setMessage("");
    setError("");

    if (!token) {
      setError("Setup token missing. Please open the email link again.");
      return;
    }

    if (password.length < 8 || password.length > 72) {
      setError("Password must contain 8 to 72 characters.");
      return;
    }

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    try {
      setLoading(true);

      const result = await setupPassword(token, password);

      // Backend must return LoginResponseDTO with a JWT.
      if (!result?.token || result.role !== "USER") {
        setError(
          "Password setup response is invalid. Please log in manually if your password was saved."
        );
        return;
      }

      // Use the SAME storage keys as your existing login page.
      localStorage.setItem("token", result.token);
      localStorage.setItem("role", result.role);

      if (result.id != null) {
        localStorage.setItem("userId", String(result.id));
      }

      if (result.name) {
        localStorage.setItem("name", result.name);
      }

      setPassword("");
      setConfirmPassword("");

      navigate("/user/dashboard", { replace: true });
    } catch (err) {
      setError(
        err.message || "Unable to set password. The link may be invalid or expired."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="setup-container">
      <form className="setup-card" onSubmit={handleSubmit}>
        <h1>Set Your Password</h1>
        <p>Create a password for your resident account.</p>

        <label htmlFor="password">New Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          minLength={8}
          maxLength={72}
          autoComplete="new-password"
          required
        />

        <label htmlFor="confirmPassword">Confirm Password</label>
        <input
          id="confirmPassword"
          type="password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          minLength={8}
          maxLength={72}
          autoComplete="new-password"
          required
        />

        {error && <p className="error-message">{error}</p>}
        {message && <p className="success-message">{message}</p>}

        <button type="submit" disabled={loading || !token}>
          {loading ? "Setting Password..." : "Set Password"}
        </button>
      </form>
    </div>
  );
}

export default SetupPassword;
