import { useEffect, useMemo, useState } from "react";
import { extractRole, getSavedToken, request, saveToken } from "./api";

const emptyPatient = {
  id: "",
  name: "",
  email: "",
  address: "",
  dateOfBirth: "",
  registeredDate: ""
};

const emptyUser = {
  email: "",
  password: "",
  role: "USER"
};

const PATIENTS_ENDPOINT = "/patients";

function App() {
  const [token, setToken] = useState(getSavedToken());
  const [loginEmail, setLoginEmail] = useState("testuser@test.com");
  const [loginPassword, setLoginPassword] = useState("");
  const [patientForm, setPatientForm] = useState(emptyPatient);
  const [userForm, setUserForm] = useState(emptyUser);
  const [patients, setPatients] = useState([]);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [loadingPatients, setLoadingPatients] = useState(false);

  const role = useMemo(() => extractRole(token), [token]);
  const isLoggedIn = Boolean(token);
  const isAdmin = role === "ADMIN";
  const isEditingPatient = Boolean(patientForm.id);

  const updateStatus = (message) => {
    setError("");
    setStatus(message);
  };

  const updateError = (message) => {
    setStatus("");
    setError(message);
  };

  const clearMessages = () => {
    setStatus("");
    setError("");
  };

  const logout = (message = "Logged out") => {
    setToken("");
    setPatients([]);
    setPatientForm(emptyPatient);
    setUserForm(emptyUser);
    saveToken("");
    setLoginPassword("");
    updateStatus(message);
  };

  const loadPatients = async (tokenOverride = token) => {
    if (!tokenOverride) return;

    setLoadingPatients(true);
    try {
      const data = await request(PATIENTS_ENDPOINT, { method: "GET" }, tokenOverride);
      setPatients(Array.isArray(data) ? data : []);
      updateStatus("Patients loaded");
    } catch (e) {
      if (e.status === 401) {
        logout("Session expired. Please login again.");
        return;
      }
      updateError(`Get patients failed: ${e.message}`);
    } finally {
      setLoadingPatients(false);
    }
  };

  useEffect(() => {
    if (!token) return;
    loadPatients(token);
  }, [token]);

  const login = async (event) => {
    event.preventDefault();
    clearMessages();

    try {
      const result = await request("/auth/login", {
        method: "POST",
        body: JSON.stringify({
          email: loginEmail.trim(),
          password: loginPassword
        })
      });

      const nextToken = result?.token || "";
      if (!nextToken) throw new Error("No token returned from /auth/login");

      setToken(nextToken);
      saveToken(nextToken);
      setLoginPassword("");
      updateStatus("Login successful");
    } catch (e) {
      updateError(`Login failed: ${e.message}`);
    }
  };

  const handlePatientChange = (field, value) => {
    setPatientForm((prev) => ({ ...prev, [field]: value }));
  };

  const resetPatientForm = () => {
    setPatientForm(emptyPatient);
  };

  const startEditPatient = (patient) => {
    setPatientForm({
      id: patient.id || "",
      name: patient.name || "",
      email: patient.email || "",
      address: patient.address || "",
      dateOfBirth: String(patient.dateOfBirth || "").slice(0, 10),
      registeredDate: ""
    });
    updateStatus("Editing patient");
  };

  const savePatient = async (event) => {
    event.preventDefault();
    clearMessages();

    if (!isLoggedIn) {
      updateError("Login first");
      return;
    }

    const payload = {
      name: patientForm.name.trim(),
      email: patientForm.email.trim(),
      address: patientForm.address.trim(),
      dateOfBirth: patientForm.dateOfBirth
    };

    if (!isEditingPatient) {
      payload.registeredDate = patientForm.registeredDate;
    }

    try {
      if (isEditingPatient) {
        await request(
          `${PATIENTS_ENDPOINT}/${patientForm.id}`,
          {
            method: "PUT",
            body: JSON.stringify(payload)
          },
          token
        );
        updateStatus("Patient updated");
      } else {
        await request(
          PATIENTS_ENDPOINT,
          {
            method: "POST",
            body: JSON.stringify(payload)
          },
          token
        );
        updateStatus("Patient created");
      }

      resetPatientForm();
      await loadPatients();
    } catch (e) {
      updateError(`Save patient failed: ${e.message}`);
    }
  };

  const deletePatient = async (patientId) => {
    if (!patientId) return;
    const confirmed = window.confirm("Delete this patient?");
    if (!confirmed) return;

    clearMessages();
    try {
      await request(`${PATIENTS_ENDPOINT}/${patientId}`, { method: "DELETE" }, token);
      if (patientForm.id === patientId) {
        resetPatientForm();
      }
      updateStatus("Patient deleted");
      await loadPatients();
    } catch (e) {
      updateError(`Delete patient failed: ${e.message}`);
    }
  };

  const addUser = async (event) => {
    event.preventDefault();
    clearMessages();

    if (!isAdmin) {
      updateError("Only ADMIN can add users");
      return;
    }

    try {
      await request(
        "/auth/admin/users",
        {
          method: "POST",
          body: JSON.stringify({
            email: userForm.email.trim(),
            password: userForm.password,
            role: userForm.role
          })
        },
        token
      );

      setUserForm(emptyUser);
      updateStatus("User created");
    } catch (e) {
      updateError(`Add user failed: ${e.message}`);
    }
  };

  if (!isLoggedIn) {
    return (
      <main className="login-page">
        <section className="card login-card">
          <p className="eyebrow">Patient Management System</p>
          <h1>Login</h1>
          <p>Enter credentials to call <code>/auth/login</code> and open Home.</p>
          <form className="form" onSubmit={login}>
            <label>
              Email
              <input
                type="email"
                value={loginEmail}
                onChange={(e) => setLoginEmail(e.target.value)}
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                minLength={8}
                value={loginPassword}
                onChange={(e) => setLoginPassword(e.target.value)}
                required
              />
            </label>
            <button type="submit">Login</button>
          </form>
          {status && <p className="status ok">{status}</p>}
          {error && <p className="status error">{error}</p>}
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <div className="glow one" />
      <div className="glow two" />

      <section className="card hero">
        <p className="eyebrow">Home</p>
        <h1>Patient Dashboard</h1>
        <p>
          Authenticated session with patient CRUD. Endpoints used:
          <code> /patients</code> and <code>/auth/admin/users</code>.
        </p>
        <div className="stats">
          <div>
            <span>{patients.length}</span>
            <small>Patients</small>
          </div>
          <div>
            <span>{role || "USER"}</span>
            <small>Role</small>
          </div>
        </div>
        <div className="actions">
          <button type="button" className="ghost" onClick={() => loadPatients()} disabled={loadingPatients}>
            {loadingPatients ? "Refreshing..." : "Get All Patients"}
          </button>
          <button type="button" className="ghost" onClick={() => logout()}>
            Logout
          </button>
        </div>
      </section>

      <section className="card">
        <h2>{isEditingPatient ? "Update Patient" : "Add Patient"}</h2>
        <form className="form" onSubmit={savePatient}>
          <label>
            Full Name
            <input
              value={patientForm.name}
              onChange={(e) => handlePatientChange("name", e.target.value)}
              required
            />
          </label>
          <label>
            Email
            <input
              type="email"
              value={patientForm.email}
              onChange={(e) => handlePatientChange("email", e.target.value)}
              required
            />
          </label>
          <label>
            Address
            <input
              value={patientForm.address}
              onChange={(e) => handlePatientChange("address", e.target.value)}
              required
            />
          </label>
          <label>
            Date Of Birth
            <input
              type="date"
              value={patientForm.dateOfBirth}
              onChange={(e) => handlePatientChange("dateOfBirth", e.target.value)}
              required
            />
          </label>
          {!isEditingPatient && (
            <label>
              Registered Date
              <input
                type="date"
                value={patientForm.registeredDate}
                onChange={(e) => handlePatientChange("registeredDate", e.target.value)}
                required
              />
            </label>
          )}
          <div className="actions">
            <button type="submit">{isEditingPatient ? "Update Patient" : "Add Patient"}</button>
            {isEditingPatient && (
              <button type="button" className="ghost" onClick={resetPatientForm}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="card">
        <h2>Add User (Admin)</h2>
        {isAdmin ? (
          <form className="form" onSubmit={addUser}>
            <label>
              User Email
              <input
                type="email"
                value={userForm.email}
                onChange={(e) => setUserForm((prev) => ({ ...prev, email: e.target.value }))}
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                minLength={8}
                value={userForm.password}
                onChange={(e) => setUserForm((prev) => ({ ...prev, password: e.target.value }))}
                required
              />
            </label>
            <label>
              Role
              <select
                value={userForm.role}
                onChange={(e) => setUserForm((prev) => ({ ...prev, role: e.target.value }))}
              >
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </label>
            <button type="submit">Create User</button>
          </form>
        ) : (
          <p className="note">Current role is {role || "USER"}. Only ADMIN can add new users.</p>
        )}
      </section>

      <section className="card wide">
        <h2>Patients</h2>
        {status && <p className="status ok">{status}</p>}
        {error && <p className="status error">{error}</p>}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Address</th>
                <th>DOB</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {patients.length === 0 && (
                <tr>
                  <td colSpan="5" className="empty">
                    No patients found
                  </td>
                </tr>
              )}
              {patients.map((patient) => (
                <tr key={patient.id || patient.email}>
                  <td>{patient.name}</td>
                  <td>{patient.email}</td>
                  <td>{patient.address}</td>
                  <td>{String(patient.dateOfBirth || "").slice(0, 10)}</td>
                  <td>
                    <div className="actions">
                      <button type="button" className="ghost" onClick={() => startEditPatient(patient)}>
                        Update
                      </button>
                      <button type="button" className="danger" onClick={() => deletePatient(patient.id)}>
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  );
}

export default App;
