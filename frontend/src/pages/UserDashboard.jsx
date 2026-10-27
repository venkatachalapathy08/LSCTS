
import { useCallback, useEffect, useMemo, useState } from "react";
import "../styles/UserDashboard.css";

const API_BASE_URL = "http://localhost:8080/api";

function getToken() {
  return localStorage.getItem("token");
}

async function apiRequest(path, options = {}) {
  const token = localStorage.getItem("token");

  const headers = {
    ...(options.headers || {}),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  if (options.body) {
    headers["Content-Type"] = "application/json";
  }

  let response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
    });
  } catch (error) {
    // Network error: backend server may be stopped
    throw new Error(
      "Unable to connect to the server. Please check whether the backend is running."
    );
  }

  const responseText = await response.text();

  let responseData = null;

  if (responseText) {
    try {
      responseData = JSON.parse(responseText);
    } catch {
      responseData = responseText;
    }
  }

  if (!response.ok) {
    const error = new Error(
      typeof responseData === "string"
        ? responseData
        : responseData?.message ||
          responseData?.error ||
          `Request failed with status ${response.status}`
    );

    // Preserve the actual HTTP status code
    error.status = response.status;

    throw error;
  }

  return responseData;
}

function formatDate(value) {
  if (!value) return "—";

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString();
}

function normaliseStatus(status) {
  const value = String(status || "PENDING").toUpperCase();

  if (value === "IN_PROGRESS" || value === "IN PROGRESS") {
    return "IN_PROGRESS";
  }

  if (value === "COMPLETED") {
    return "COMPLETED";
  }

  return "PENDING";
}

function statusLabel(status) {
  const labels = {
    PENDING: "Pending",
    IN_PROGRESS: "In progress",
    COMPLETED: "Completed",
  };

  return labels[normaliseStatus(status)];
}

function getErrorMessage(error) {
  if (error.status === 401) {
    return "Your session has expired. Please log in again.";
  }

  if (error.status === 403) {
    return "You don't have permission to perform this action.";
  }

  if (error.status === 404) {
    return "The requested resource was not found.";
  }

  return error.message || "Something went wrong. Please try again.";
}

export default function UserDashboard() {
  const [activeTab, setActiveTab] = useState("overview");
  const [profile, setProfile] = useState(null);
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState("");
  const [notice, setNotice] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState({
    title: "",
    description: "",
  });

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setPageError("");

    try {
      const [profileData, complaintData] = await Promise.all([
        apiRequest("/users/me"),
        apiRequest("/complaints/my"),
      ]);

      setProfile(profileData);

      const list = Array.isArray(complaintData)
        ? complaintData
        : complaintData?.content ||
          complaintData?.complaints ||
          [];

      setComplaints(list);
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const counts = useMemo(() => {
    const result = {
      total: complaints.length,
      pending: 0,
      inProgress: 0,
      completed: 0,
    };

    complaints.forEach((item) => {
      const status = normaliseStatus(item.status);

      if (status === "PENDING") {
        result.pending++;
      } else if (status === "IN_PROGRESS") {
        result.inProgress++;
      } else {
        result.completed++;
      }
    });

    return result;
  }, [complaints]);

  const filteredComplaints = useMemo(() => {
    return complaints.filter((item) => {
      const status = normaliseStatus(item.status);

      const text = [
        item.title || "",
        item.description || "",
        item.complaintId ?? item.complaint_id ?? "",
      ]
        .join(" ")
        .toLowerCase();

      return (
        (statusFilter === "ALL" || status === statusFilter) &&
        text.includes(search.toLowerCase().trim())
      );
    });
  }, [complaints, search, statusFilter]);

  function handleInputChange(event) {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleSubmitComplaint(event) {
    event.preventDefault();

    setNotice("");
    setPageError("");

    const title = form.title.trim();
    const description = form.description.trim();

    if (!title || !description) {
      setPageError("Please enter both a title and description.");
      return;
    }

    setSubmitting(true);

    try {
      await apiRequest("/complaints", {
        method: "POST",
        body: JSON.stringify({
          title,
          description,
        }),
      });

      setForm({
        title: "",
        description: "",
      });

      setNotice("Your complaint has been submitted successfully.");

      await loadDashboard();
      setActiveTab("complaints");
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  function handleLogout() {
    ["token", "role", "userId", "name"].forEach((key) => {
      localStorage.removeItem(key);
    });

    window.location.href = "/login";
  }

  const navigation = [
    { id: "overview", label: "Overview", icon: "⌂" },
    { id: "create", label: "Raise a complaint", icon: "+" },
    { id: "complaints", label: "My complaints", icon: "▤" },
    { id: "profile", label: "My profile", icon: "♙" },
  ];

  return (
    <div className="user-layout">
      <aside className="user-sidebar">
        <div className="user-brand">
          <div className="user-brand-icon">L</div>

          <div>
            <h2>LSTS</h2>
            <p>Apartment Services</p>
          </div>
        </div>

        <div className="user-sidebar-label">RESIDENT MENU</div>

        <nav className="user-nav">
          {navigation.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`user-nav-item ${
                activeTab === item.id ? "active" : ""
              }`}
              onClick={() => {
                setActiveTab(item.id);
                setNotice("");
                setPageError("");
              }}
            >
              <span className="user-nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="user-sidebar-bottom">
          <div className="user-profile-mini">
            <div className="user-avatar">
              {(profile?.name ||
                localStorage.getItem("name") ||
                "R")
                .charAt(0)
                .toUpperCase()}
            </div>

            <div>
              <strong>
                {profile?.name ||
                  localStorage.getItem("name") ||
                  "Resident"}
              </strong>
              <small>Resident account</small>
            </div>
          </div>

          <button
            className="user-logout-button"
            type="button"
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>
      </aside>

      <main className="user-main">
        <header className="user-topbar">
          <div>
            <p className="user-eyebrow">
              LOCAL SERVICE COMPLAINT TRACKING
            </p>

            <h1>
              {navigation.find((item) => item.id === activeTab)?.label ||
                "Dashboard"}
            </h1>
          </div>

          <button
            className="user-refresh-button"
            type="button"
            onClick={loadDashboard}
            disabled={loading}
          >
            ↻ Refresh
          </button>
        </header>

        {notice && (
          <div className="user-alert user-alert-success" role="status">
            {notice}
          </div>
        )}

        {pageError && (
          <div className="user-alert user-alert-error" role="alert">
            {pageError}
          </div>
        )}

        {activeTab === "overview" && (
          <section>
            <div className="user-welcome">
              <div>
                <p className="user-eyebrow">YOUR RESIDENT PORTAL</p>

                <h2>
                  Hello, {profile?.name?.split(" ")[0] || "Resident"}!
                </h2>

                <p>
                  Raise a service complaint and track its progress here.
                </p>

                <button
                  type="button"
                  className="user-primary-button"
                  onClick={() => {
                    setActiveTab("create");
                    setPageError("");
                  }}
                >
                  ＋ Raise a complaint
                </button>
              </div>

              <div className="user-welcome-symbol">LSTS</div>
            </div>

            <div className="user-stats-grid">
              <article className="user-stat-card">
                <span className="user-stat-icon">▤</span>
                <p>Total complaints</p>
                <h2>{loading ? "…" : counts.total}</h2>
                <small>All complaints you submitted</small>
              </article>

              <article className="user-stat-card">
                <span className="user-stat-icon">◷</span>
                <p>Pending</p>
                <h2>{loading ? "…" : counts.pending}</h2>
                <small>Waiting for review</small>
              </article>

              <article className="user-stat-card">
                <span className="user-stat-icon">↻</span>
                <p>In progress</p>
                <h2>{loading ? "…" : counts.inProgress}</h2>
                <small>Being worked on</small>
              </article>

              <article className="user-stat-card">
                <span className="user-stat-icon">✓</span>
                <p>Completed</p>
                <h2>{loading ? "…" : counts.completed}</h2>
                <small>Work marked as completed</small>
              </article>
            </div>

            <section className="user-panel">
              <div className="user-panel-heading">
                <div>
                  <h2>Recent complaints</h2>
                  <p>Latest updates on your service requests.</p>
                </div>

                <button
                  type="button"
                  className="user-text-button"
                  onClick={() => setActiveTab("complaints")}
                >
                  View all →
                </button>
              </div>

              {loading ? (
                <p className="user-loading">Loading your complaints…</p>
              ) : complaints.length === 0 ? (
                <div className="user-empty">
                  <span>▤</span>
                  <h3>No complaints yet</h3>
                  <p>
                    If you need apartment maintenance, submit your first
                    complaint.
                  </p>

                  <button
                    type="button"
                    className="user-primary-button"
                    onClick={() => setActiveTab("create")}
                  >
                    Raise a complaint
                  </button>
                </div>
              ) : (
                <div className="user-table-wrapper">
                  <table className="user-table">
                    <thead>
                      <tr>
                        <th>Complaint</th>
                        <th>Submitted</th>
                        <th>Status</th>
                      </tr>
                    </thead>

                    <tbody>
                      {[...complaints]
                        .sort(
                          (a, b) =>
                            new Date(
                              b.complaintCreatedAt || b.createdAt
                            ) -
                            new Date(
                              a.complaintCreatedAt || a.createdAt
                            )
                        )
                        .slice(0, 5)
                        .map((item) => {
                          const id =
                            item.complaintId ??
                            item.complaint_id ??
                            item.id;

                          return (
                            <tr key={id}>
                              <td>
                                <strong>#{id}</strong>
                                <div className="user-complaint-title">
                                  {item.title || "Untitled complaint"}
                                </div>
                              </td>

                              <td>
                                {formatDate(
                                  item.complaintCreatedAt ??
                                    item.createdAt
                                )}
                              </td>

                              <td>
                                <span
                                  className={`user-status ${normaliseStatus(
                                    item.status
                                  ).toLowerCase()}`}
                                >
                                  {statusLabel(item.status)}
                                </span>
                              </td>
                            </tr>
                          );
                        })}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </section>
        )}

        {activeTab === "create" && (
          <section className="user-panel user-form-panel">
            <div className="user-panel-heading">
              <div>
                <h2>Raise a service complaint</h2>
                <p>
                  Describe the issue clearly so the apartment admin can
                  follow up.
                </p>
              </div>
            </div>

            <form
              className="user-complaint-form"
              onSubmit={handleSubmitComplaint}
            >
              <label>
                Complaint title

                <input
                  name="title"
                  value={form.title}
                  onChange={handleInputChange}
                  maxLength={100}
                  placeholder="e.g. Water leakage in bathroom"
                  required
                />
              </label>

              <label>
                Description

                <textarea
                  name="description"
                  value={form.description}
                  onChange={handleInputChange}
                  maxLength={5000}
                  rows={7}
                  placeholder="Explain where the issue is and what happened…"
                  required
                />

                <small>{form.description.length}/5000 characters</small>
              </label>

              <div className="user-form-note">
                <strong>What happens next?</strong>
                <p>
                  Your complaint will be recorded as Pending. The admin
                  will review it and update the status as work progresses.
                </p>
              </div>

              <div className="user-form-actions">
                <button
                  type="button"
                  className="user-secondary-button"
                  onClick={() => setActiveTab("overview")}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="user-primary-button"
                  disabled={submitting}
                >
                  {submitting ? "Submitting…" : "Submit complaint"}
                </button>
              </div>
            </form>
          </section>
        )}

        {activeTab === "complaints" && (
          <section className="user-panel">
            <div className="user-panel-heading">
              <div>
                <h2>My complaints</h2>
                <p>
                  Only complaints submitted from your account are shown
                  here.
                </p>
              </div>

              <button
                type="button"
                className="user-primary-button"
                onClick={() => setActiveTab("create")}
              >
                ＋ New complaint
              </button>
            </div>

            <div className="user-filters">
              <input
                aria-label="Search complaints"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Search by title or description…"
              />

              <select
                aria-label="Filter by status"
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value)}
              >
                <option value="ALL">All statuses</option>
                <option value="PENDING">Pending</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>

            {loading ? (
              <p className="user-loading">Loading your complaints…</p>
            ) : filteredComplaints.length === 0 ? (
              <p className="user-empty-simple">
                No complaints match your search.
              </p>
            ) : (
              <div className="user-table-wrapper">
                <table className="user-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Complaint</th>
                      <th>Description</th>
                      <th>Submitted</th>
                      <th>Status</th>
                      <th>Last updated</th>
                    </tr>
                  </thead>

                  <tbody>
                    {filteredComplaints.map((item) => {
                      const id =
                        item.complaintId ??
                        item.complaint_id ??
                        item.id;

                      return (
                        <tr key={id}>
                          <td>#{id}</td>

                          <td>
                            <strong>{item.title || "Untitled"}</strong>
                          </td>

                          <td className="user-description-cell">
                            {item.description || "—"}
                          </td>

                          <td>
                            {formatDate(
                              item.complaintCreatedAt ?? item.createdAt
                            )}
                          </td>

                          <td>
                            <span
                              className={`user-status ${normaliseStatus(
                                item.status
                              ).toLowerCase()}`}
                            >
                              {statusLabel(item.status)}
                            </span>
                          </td>

                          <td>
                            {formatDate(
                              item.updatedAt ?? item.updated_at
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}

        {activeTab === "profile" && (
          <section className="user-panel user-profile-panel">
            <div className="user-panel-heading">
              <div>
                <h2>My profile</h2>
                <p>Your account details registered with the apartment.</p>
              </div>
            </div>

            {loading ? (
              <p className="user-loading">Loading profile…</p>
            ) : profile ? (
              <div className="user-profile-grid">
                <div className="user-profile-field">
                  <span>Full name</span>
                  <strong>{profile.name || "—"}</strong>
                </div>

                <div className="user-profile-field">
                  <span>Email address</span>
                  <strong>{profile.email || "—"}</strong>
                </div>

                <div className="user-profile-field">
                  <span>Phone number</span>
                  <strong>
                    {profile.phoneNumber ??
                      profile.phone_number ??
                      "—"}
                  </strong>
                </div>

                <div className="user-profile-field">
                  <span>Account role</span>
                  <strong>{profile.role || "USER"}</strong>
                </div>

                <div className="user-profile-field">
                  <span>Account status</span>
                  <strong>
                    <span
                      className={`user-status ${
                        profile.active === false ? "pending" : "completed"
                      }`}
                    >
                      {profile.active === false ? "Inactive" : "Active"}
                    </span>
                  </strong>
                </div>
              </div>
            ) : (
              <p>Profile details are unavailable.</p>
            )}
          </section>
        )}
      </main>
    </div>
  );
}
