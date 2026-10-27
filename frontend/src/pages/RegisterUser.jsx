import { useState } from "react";
import { useNavigate } from "react-router-dom";

function RegisterUser() {
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    const token = localStorage.getItem("token");

    if (!token || localStorage.getItem("role") !== "ADMIN") {
      navigate("/login", { replace: true });
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(
        "http://localhost:8080/api/admin/users",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            name,
            email,
            phoneNumber,
          }),
        }
      );

      const responseText = await response.text();

      if (!response.ok) {
        throw new Error(
          responseText || `Registration failed (${response.status})`
        );
      }

      setSuccess(
        responseText || "Resident registration request completed."
      );

      setName("");
      setEmail("");
      setPhoneNumber("");
    } catch (err) {
      setError(err.message || "Unable to register resident.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="setup-container">
      <form className="setup-card" onSubmit={handleSubmit}>
        <h1>Register Resident</h1>
        <p>Create a resident account for your apartment.</p>

        <label htmlFor="name">Resident Name</label>
        <input
          id="name"
          type="text"
          value={name}
          onChange={(event) => setName(event.target.value)}
          required
        />

        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />

        <label htmlFor="phoneNumber">Phone Number</label>
        <input
          id="phoneNumber"
          type="tel"
          value={phoneNumber}
          onChange={(event) => setPhoneNumber(event.target.value)}
          pattern="[0-9]{10}"
          title="Enter a 10-digit phone number"
          required
        />

        {error && (
          <p className="error-message" role="alert">
            {error}
          </p>
        )}

        {success && (
          <p role="status">
            {success}
          </p>
        )}

        <button type="submit" disabled={loading}>
          {loading ? "Registering..." : "Register Resident"}
        </button>

        <button
          type="button"
          onClick={() => navigate("/admin")}
          disabled={loading}
        >
          Back to Dashboard
        </button>
      </form>
    </main>
  );
}

export default RegisterUser;