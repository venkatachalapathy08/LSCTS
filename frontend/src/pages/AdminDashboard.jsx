
import { useCallback, useEffect, useMemo, useState } from "react";
import "../styles/AdminDashboard.css";

const API_URL = "http://localhost:8080/api";

function getAuthHeaders() {
  const token = localStorage.getItem("token");

  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...getAuthHeaders(),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed: ${response.status}`);
  }

  if (response.status === 204) return null;

  return response.json();
}

function getStatusLabel(status) {
  switch (status) {
    case "PENDING":
      return "Pending";
    case "IN_PROGRESS":
      return "In Progress";
    case "COMPLETED":
      return "Completed";
    default:
      return status || "Unknown";
  }
}

function getStatusClass(status) {
  switch (status) {
    case "PENDING":
      return "status-pending";
    case "IN_PROGRESS":
      return "status-progress";
    case "COMPLETED":
      return "status-completed";
    default:
      return "status-default";
  }
}

function formatDate(dateValue) {
  if (!dateValue) return "—";

  const date = new Date(dateValue);

  if (Number.isNaN(date.getTime())) return "—";

  return date.toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function getResidentName(complaint) {
  return (
    complaint.username ??
    complaint.userName ??
    complaint.user?.name ??
    complaint.name ??
    "Unknown User"
  );
}

export default function AdminDashboard() {
  const [users, setUsers] = useState([]);
  const [complaints, setComplaints] = useState([]);

  const [usersLoading, setUsersLoading] = useState(true);
  const [complaintsLoading, setComplaintsLoading] = useState(true);

  const [usersError, setUsersError] = useState("");
  const [complaintsError, setComplaintsError] = useState("");
  const [pageError, setPageError] = useState("");

  const [userSearch, setUserSearch] = useState("");
  const [complaintSearch, setComplaintSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const [statusChanges, setStatusChanges] = useState({});
  const [updatingIds, setUpdatingIds] = useState([]);
  const [deletingIds, setDeletingIds] = useState([]);

  const loadUsers = useCallback(async () => {
    setUsersLoading(true);
    setUsersError("");

    try {
      const data = await apiRequest("/admin/users");
      setUsers(Array.isArray(data) ? data : []);
    } catch (error) {
      setUsersError(error.message || "Unable to load users.");
    } finally {
      setUsersLoading(false);
    }
  }, []);

  const loadComplaints = useCallback(async () => {
    setComplaintsLoading(true);
    setComplaintsError("");

    try {
      const data = await apiRequest("/complaints");
      setComplaints(Array.isArray(data) ? data : []);
    } catch (error) {
      setComplaintsError(error.message || "Unable to load complaints.");
    } finally {
      setComplaintsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUsers();
    loadComplaints();
  }, [loadUsers, loadComplaints]);

  const pendingCount = complaints.filter(
    (complaint) => complaint.status === "PENDING"
  ).length;

  const inProgressCount = complaints.filter(
    (complaint) => complaint.status === "IN_PROGRESS"
  ).length;

  const completedCount = complaints.filter(
    (complaint) => complaint.status === "COMPLETED"
  ).length;

  const filteredUsers = useMemo(() => {
    const search = userSearch.trim().toLowerCase();

    return users.filter((user) =>
      [user.name, user.email, user.phoneNumber, user.role]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(search))
    );
  }, [users, userSearch]);

  const filteredComplaints = useMemo(() => {
    const search = complaintSearch.trim().toLowerCase();

    return complaints.filter((complaint) => {
      const matchesStatus =
        statusFilter === "ALL" || complaint.status === statusFilter;

      const searchableText = [
        getResidentName(complaint),
        complaint.title,
        complaint.description,
        complaint.status,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      return matchesStatus && searchableText.includes(search);
    });
  }, [complaints, complaintSearch, statusFilter]);

  function handleStatusSelection(complaintId, status) {
    setStatusChanges((current) => ({
      ...current,
      [complaintId]: status,
    }));
  }

  async function handleStatusUpdate(complaint) {
    const complaintId = complaint.complaintId;
    const selectedStatus =
      statusChanges[complaintId] ?? complaint.status;

    if (!complaintId) {
      setPageError("Complaint ID is missing from the API response.");
      return;
    }

    if (selectedStatus === complaint.status) {
      setPageError("Please select a different status before updating.");
      return;
    }

    setPageError("");
    setUpdatingIds((current) => [...current, complaintId]);

    try {
      const updatedComplaint = await apiRequest(
        `/complaints/${complaintId}/status`,
        {
          method: "PATCH",
          body: JSON.stringify({ status: selectedStatus }),
        }
      );

      setComplaints((current) =>
        current.map((item) =>
          item.complaintId === complaintId
            ? { ...item, ...updatedComplaint }
            : item
        )
      );

      setStatusChanges((current) => {
        const next = { ...current };
        delete next[complaintId];
        return next;
      });
    } catch (error) {
      setPageError(
        `Status update failed: ${error.message || "Please try again."}`
      );
    } finally {
      setUpdatingIds((current) =>
        current.filter((id) => id !== complaintId)
      );
    }
  }

  async function handleDeleteUser(user) {
    if (!user.userId) {
      setPageError("User ID is missing from the API response.");
      return;
    }

    const confirmed = window.confirm(
      `Delete user "${user.name}"? This action may also delete their complaints.`
    );

    if (!confirmed) return;

    setPageError("");
    setDeletingIds((current) => [...current, user.userId]);

    try {
      await apiRequest(`/admin/users/${user.userId}`, {
        method: "DELETE",
      });

      setUsers((current) =>
        current.filter((item) => item.userId !== user.userId)
      );

      await loadComplaints();
    } catch (error) {
      setPageError(
        `User deletion failed: ${error.message || "Please try again."}`
      );
    } finally {
      setDeletingIds((current) =>
        current.filter((id) => id !== user.userId)
      );
    }
  }

  function handleLogout() {
    localStorage.removeItem("token");
    window.location.href = "/login";
  }

  return (
    <main className="admin-dashboard">
      <header className="admin-header">
        <div>
          <p className="admin-eyebrow">LSCTS ADMINISTRATION</p>
          <h1>Admin Dashboard</h1>
          <p className="admin-subtitle">
            Manage residents and track apartment complaints.
          </p>
        </div>

        <div className="header-actions">
          <button
            className="secondary-button"
            onClick={() => {
              loadUsers();
              loadComplaints();
            }}
          >
            Refresh Dashboard
          </button>

          <button className="logout-button" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      <div className="admin-content">
        {pageError && (
          <div className="error-message" role="alert">
            {pageError}
            <button
              className="inline-retry"
              onClick={() => setPageError("")}
            >
              Dismiss
            </button>
          </div>
        )}

        <section className="dashboard-summary" aria-label="Dashboard summary">
          <article className="summary-card">
            <span className="summary-label">Total Residents</span>
            <strong className="summary-value">
              {usersLoading ? "…" : users.length}
            </strong>
            <span className="summary-description">Registered accounts</span>
          </article>

          <article className="summary-card">
            <span className="summary-label">Total Complaints</span>
            <strong className="summary-value">
              {complaintsLoading ? "…" : complaints.length}
            </strong>
            <span className="summary-description">All reported issues</span>
          </article>

          <article className="summary-card">
            <span className="summary-label">Pending</span>
            <strong className="summary-value pending-number">
              {complaintsLoading ? "…" : pendingCount}
            </strong>
            <span className="summary-description">Awaiting action</span>
          </article>

          <article className="summary-card">
            <span className="summary-label">In Progress</span>
            <strong className="summary-value progress-number">
              {complaintsLoading ? "…" : inProgressCount}
            </strong>
            <span className="summary-description">Work underway</span>
          </article>

          <article className="summary-card">
            <span className="summary-label">Completed</span>
            <strong className="summary-value completed-number">
              {complaintsLoading ? "…" : completedCount}
            </strong>
            <span className="summary-description">Resolved complaints</span>
          </article>
        </section>

        {/* Resident Management */}
        <section className="dashboard-section">
          <div className="section-heading">
            <div>
              <h2>Resident Management</h2>
              <p>View and manage resident accounts.</p>
            </div>

            <div className="section-actions">
              <button className="secondary-button" onClick={loadUsers}>
                Refresh Users
              </button>

              <button
                className="primary-button"
                onClick={() =>
                  (window.location.href = "/admin/register-user")
                }
              >
                + Register Resident
              </button>
            </div>
          </div>

          <div className="table-toolbar">
            <input
              type="search"
              value={userSearch}
              onChange={(event) => setUserSearch(event.target.value)}
              placeholder="Search name, email, phone..."
              aria-label="Search residents"
            />

            <span className="result-count">
              {filteredUsers.length} residents
            </span>
          </div>

          {usersError && (
            <div className="error-message" role="alert">
              {usersError}
              <button className="inline-retry" onClick={loadUsers}>
                Retry
              </button>
            </div>
          )}

          <div className="table-wrapper">
            <table className="dashboard-table users-table">
              <thead>
                <tr>
                  <th>Resident</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Account</th>
                  <th>Action</th>
                </tr>
              </thead>

              <tbody>
                {usersLoading ? (
                  <tr>
                    <td colSpan="6" className="empty-cell">
                      Loading residents...
                    </td>
                  </tr>
                ) : filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="empty-cell">
                      No residents found.
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map((user) => (
                    <tr key={user.userId}>
                      <td className="user-name">{user.name || "—"}</td>
                      <td>{user.email || "—"}</td>
                      <td>{user.phoneNumber || "—"}</td>
                      <td>
                        <span
                          className={`role-badge role-${String(
                            user.role || "user"
                          ).toLowerCase()}`}
                        >
                          {user.role || "USER"}
                        </span>
                      </td>
                      <td>
                        {user.passwordSetupCompleted === false ? (
                          <span className="protected-label">
                            Setup pending
                          </span>
                        ) : (
                          <span className="account-active">Active</span>
                        )}
                      </td>
                      <td>
                        {user.role === "ADMIN" ? (
                          <span className="protected-label">
                            Protected
                          </span>
                        ) : (
                          <button
                            className="delete-button"
                            disabled={deletingIds.includes(user.userId)}
                            onClick={() => handleDeleteUser(user)}
                          >
                            {deletingIds.includes(user.userId)
                              ? "Deleting..."
                              : "Delete"}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>

        {/* Complaint Management */}
        <section className="dashboard-section">
          <div className="section-heading">
            <div>
              <h2>Complaint Management</h2>
              <p>Review complaints and update their status.</p>
            </div>

            <button
              className="secondary-button"
              onClick={loadComplaints}
            >
              Refresh Complaints
            </button>
          </div>

          <div className="table-toolbar complaint-toolbar">
            <input
              type="search"
              value={complaintSearch}
              onChange={(event) => setComplaintSearch(event.target.value)}
              placeholder="Search resident, title, description..."
              aria-label="Search complaints"
            />

            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              aria-label="Filter complaints by status"
            >
              <option value="ALL">All statuses</option>
              <option value="PENDING">Pending</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="COMPLETED">Completed</option>
            </select>

            <span className="result-count">
              {filteredComplaints.length} complaints
            </span>
          </div>

          {complaintsError && (
            <div className="error-message" role="alert">
              {complaintsError}
              <button className="inline-retry" onClick={loadComplaints}>
                Retry
              </button>
            </div>
          )}

          <div className="table-wrapper">
            <table className="dashboard-table complaints-table">
              <thead>
                <tr>
                  <th>S.No</th>
                  <th>Resident Name</th>
                  <th>Complaint</th>
                  <th>Description</th>
                  <th>Created At</th>
                  <th>Current Status</th>
                  <th>Update Status</th>
                </tr>
              </thead>

              <tbody>
                {complaintsLoading ? (
                  <tr>
                    <td colSpan="7" className="empty-cell">
                      Loading complaints...
                    </td>
                  </tr>
                ) : filteredComplaints.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-cell">
                      No complaints found.
                    </td>
                  </tr>
                ) : (
                  filteredComplaints.map((complaint, index) => {
                    const id = complaint.complaintId;
                    const selectedStatus =
                      statusChanges[id] ?? complaint.status;
                    const isUpdating = updatingIds.includes(id);

                    return (
                      <tr key={id}>
                        {/* Serial number, not the database complaint ID */}
                        <td>{index + 1}</td>

                        <td className="user-name">
                          {getResidentName(complaint)}
                        </td>

                        <td className="complaint-title">
                          {complaint.title || "—"}
                        </td>

                        <td className="description-cell">
                          {complaint.description || "—"}
                        </td>

                        <td>
                          {formatDate(complaint.complaintCreatedAt)}
                        </td>

                        <td>
                          <span
                            className={`status-badge ${getStatusClass(
                              complaint.status
                            )}`}
                          >
                            {getStatusLabel(complaint.status)}
                          </span>
                        </td>

                        <td>
                          <div className="status-update-controls">
                            <select
                              value={selectedStatus || "PENDING"}
                              onChange={(event) =>
                                handleStatusSelection(
                                  id,
                                  event.target.value
                                )
                              }
                              disabled={isUpdating}
                              aria-label={`Select status for ${
                                complaint.title || "complaint"
                              }`}
                            >
                              <option value="PENDING">Pending</option>
                              <option value="IN_PROGRESS">
                                In Progress
                              </option>
                              <option value="COMPLETED">Completed</option>
                            </select>

                            <button
                              className="primary-button update-status-button"
                              disabled={
                                isUpdating ||
                                selectedStatus === complaint.status
                              }
                              onClick={() =>
                                handleStatusUpdate(complaint)
                              }
                            >
                              {isUpdating ? "Saving..." : "Update"}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>

        <footer className="dashboard-footer">
          LSCTS · Local Service Complaint and Tracking System
        </footer>
      </div>
    </main>
  );
}
