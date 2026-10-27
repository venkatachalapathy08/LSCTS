
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { forgotPassword, verifyOtp } from "../api/authApi";

function ForgotPassword() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [step, setStep] = useState(1);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleRequestOtp(event) {
    event.preventDefault();
    setError("");
    setMessage("");

    try {
      setLoading(true);

      const result = await forgotPassword(email.trim());

      setStep(2);
      setMessage(
        typeof result === "string"
          ? result
          : result?.message ||
              "If an account exists for this email, a password reset OTP will be sent."
      );
    } catch (err) {
      setError(err.message || "Unable to request OTP.");
    } finally {
      setLoading(false);
    }
  }

  async function handleVerifyOtp(event) {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!/^[0-9]{6}$/.test(otp)) {
      setError("OTP must contain exactly 6 digits.");
      return;
    }

    try {
      setLoading(true);

      const result = await verifyOtp(email.trim(), otp);

      // AuthService.verifyOtp() returns the raw reset token as a String.
      const resetToken =
        typeof result === "string"
          ? result
          : result?.resetToken || result?.token;

      if (!resetToken) {
        throw new Error(
          "Reset token missing in the OTP verification response."
        );
      }

      navigate("/reset-password", {
        state: { resetToken },
      });
    } catch (err) {
      setError(err.message || "OTP verification failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="setup-container">
      <section className="setup-card">
        <h1>Forgot Password</h1>
        <p>Reset your LSCTS account password.</p>

        {step === 1 ? (
          <form onSubmit={handleRequestOtp}>
            <label htmlFor="email">Registered email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              maxLength={150}
              required
            />

            <button type="submit" disabled={loading}>
              {loading ? "Sending OTP..." : "Send OTP"}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp}>
            <p>Enter the 6-digit OTP sent to your email.</p>

            <label htmlFor="otp">OTP</label>
            <input
              id="otp"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              value={otp}
              onChange={(event) =>
                setOtp(
                  event.target.value.replace(/\D/g, "").slice(0, 6)
                )
              }
              placeholder="Enter 6-digit OTP"
              maxLength={6}
              required
            />

            <button type="submit" disabled={loading}>
              {loading ? "Verifying..." : "Verify OTP"}
            </button>

            <button
              type="button"
              disabled={loading}
              onClick={() => {
                setStep(1);
                setOtp("");
                setMessage("");
                setError("");
              }}
            >
              Change Email
            </button>
          </form>
        )}

        {message && <p>{message}</p>}
        {error && <p className="error-message">{error}</p>}

        <p>
          Remember your password? <Link to="/login">Back to Login</Link>
        </p>
      </section>
    </main>
  );
}

export default ForgotPassword;
