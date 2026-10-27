
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { resetPassword } from "../api/authApi";

function ResetPassword() {
  const location = useLocation();
  const navigate = useNavigate();

  const resetToken = location.state?.resetToken;

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!resetToken) {
      setError("Reset token missing. Please start again.");
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

      const result = await resetPassword(resetToken, password);

      setMessage(
        typeof result === "string"
          ? result
          : result?.message || "Password reset successfully."
      );

      setPassword("");
      setConfirmPassword("");

      // Redirect after successful password reset.
      navigate("/login", { replace: true });
    } catch (err) {
      setError(err.message || "Password reset failed.");
    } finally {
      setLoading(false);
    }
  }

  if (!resetToken) {
    return (
      <main className="setup-container">
        <section className="setup-card">
          <h1>Reset Password</h1>
          <p>
            Reset token missing. Please request a new OTP.
          </p>
          <Link to="/forgot-password">Forgot Password</Link>
        </section>
      </main>
    );
  }

  return (
    <main className="setup-container">
      <form className="setup-card" onSubmit={handleSubmit}>
        <h1>Reset Password</h1>
        <p>Enter your new password.</p>

        <label htmlFor="password">New password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="new-password"
          minLength={8}
          maxLength={72}
          required
        />

        <label htmlFor="confirmPassword">Confirm new password</label>
        <input
          id="confirmPassword"
          type="password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          autoComplete="new-password"
          minLength={8}
          maxLength={72}
          required
        />

        {message && <p>{message}</p>}
        {error && <p className="error-message">{error}</p>}

        <button type="submit" disabled={loading}>
          {loading ? "Resetting..." : "Reset Password"}
        </button>

        <p>
          <Link to="/login">Back to Login</Link>
        </p>
      </form>
    </main>
  );
}

export default ResetPassword;
