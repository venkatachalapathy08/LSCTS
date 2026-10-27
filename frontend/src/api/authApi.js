
const API_BASE_URL = "http://localhost:8080";

async function readResponse(response) {
  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function getErrorMessage(data, fallback) {
  if (typeof data === "string" && data.trim()) {
    return data;
  }

  return (
    data?.message ||
    data?.error ||
    data?.detail ||
    fallback
  );
}

async function postRequest(endpoint, body, fallbackMessage) {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  const data = await readResponse(response);

  if (!response.ok) {
    throw new Error(getErrorMessage(data, fallbackMessage));
  }

  return data;
}

export async function login(email, password) {
  const data = await postRequest(
    "/api/auth/login",
    { email, password },
    "Login failed. Check your credentials."
  );

  if (!data?.token) {
    throw new Error(
      "Login response does not contain a JWT token."
    );
  }

  return data;
}

export async function setupPassword(setupToken, newPassword) {
  const data = await postRequest(
    "/api/auth/setup-password",
    { setupToken, newPassword },
    "Password setup failed. The link may be invalid or expired."
  );

  if (!data?.token) {
    throw new Error(
      "Password was processed, but the response does not contain a JWT token."
    );
  }

  return data;
}

export async function forgotPassword(email) {
  return postRequest(
    "/api/auth/forgot-password",
    { email },
    "Unable to request password reset."
  );
}

export async function verifyOtp(email, otp) {
  return postRequest(
    "/api/auth/verify-otp",
    { email, otp },
    "OTP verification failed."
  );
}

export async function resetPassword(resetToken, newPassword) {
  return postRequest(
    "/api/auth/reset-password",
    { resetToken, newPassword },
    "Password reset failed."
  );
}
