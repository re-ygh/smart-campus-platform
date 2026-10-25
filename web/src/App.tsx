import { useEffect, useState, useCallback } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import './style.css';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:9080';

type Role = 'FACULTY' | 'TEACHER' | 'STUDENT';

type AuthState = {
  token: string;
  role: Role;
  tenantId: string;
  username: string;
};

type Toast = {
  id: string;
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
};

export default function App() {
  const [auth, setAuth] = useState<AuthState | null>(() => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role') as Role | null;
    const tenantId = localStorage.getItem('tenantId');
    const username = localStorage.getItem('username');
    if (token && role && tenantId && username) {
      return { token, role, tenantId, username };
    }
    return null;
  });

  const handleLoginSuccess = (token: string, role: Role, tenantId: string, username: string) => {
    localStorage.setItem('token', token);
    localStorage.setItem('role', role);
    localStorage.setItem('tenantId', tenantId);
    localStorage.setItem('username', username);
    setAuth({ token, role, tenantId, username });
  };

  const handleLogout = () => {
    localStorage.clear();
    setAuth(null);
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={!auth ? <LoginPage onLoginSuccess={handleLoginSuccess} /> : <Navigate to="/dashboard" replace />}
        />
        <Route
          path="/dashboard"
          element={auth ? <DashboardPage auth={auth} onLogout={handleLogout} /> : <Navigate to="/login" replace />}
        />
        <Route path="*" element={<Navigate to={auth ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </BrowserRouter>
  );
}

function LoginPage({ onLoginSuccess }: { onLoginSuccess: (t: string, r: Role, tenantId: string, username: string) => void }) {
  const [username, setUsername] = useState('amin');
  const [password, setPassword] = useState('13831383');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const login = async () => {
    if (!username || !password) {
      setError('Please enter username and password');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      if (!res.ok) {
        const errData = await res.json().catch(() => ({ message: 'Login failed' }));
        throw new Error(errData.message || 'Invalid credentials');
      }
      const data = await res.json();
      onLoginSuccess(data.token, data.role as Role, data.tenantId, username);
      navigate('/dashboard', { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !loading) {
      login();
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <div className="login-icon">🎓</div>
          <h2>Smart Campus</h2>
          <p className="muted">Sign in to access your dashboard</p>
        </div>

        {error && (
          <div className="alert error">
            <span>⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <div className="form-group">
          <label className="form-label">Username</label>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Enter your username"
            disabled={loading}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Enter your password"
            disabled={loading}
          />
        </div>

        <button onClick={login} disabled={loading} style={{ width: '100%', marginTop: 8 }}>
          {loading ? <><span className="loading" /> Signing in...</> : 'Sign In'}
        </button>

        <div style={{ marginTop: 24, padding: 16, background: 'var(--bg-secondary)', borderRadius: 'var(--radius)', fontSize: 12 }}>
          <p style={{ fontWeight: 600, marginBottom: 8 }}>Demo Credentials:</p>
          <p className="muted">Faculty: amin / 13831383</p>
          <p className="muted">Teacher: teacher1 / password</p>
          <p className="muted">Student: student1 / password</p>
        </div>
      </div>
    </div>
  );
}

function DashboardPage({ auth, onLogout }: { auth: AuthState; onLogout: () => void }) {
  const [activeTab, setActiveTab] = useState('overview');
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((message: string, type: Toast['type'] = 'info') => {
    const id = Date.now().toString();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  }, []);

  const sidebarLinks = [
    { id: 'overview', label: 'Overview', icon: '🏠', roles: ['FACULTY', 'TEACHER', 'STUDENT'] },
    { id: 'users', label: 'Manage Users', icon: '👥', roles: ['FACULTY'] },
    { id: 'resources', label: 'Resources', icon: '🏢', roles: ['FACULTY'] },
    { id: 'tickets', label: 'Tickets', icon: '🎫', roles: ['FACULTY'] },
    { id: 'exams', label: 'Exams', icon: '📝', roles: ['TEACHER'] },
    { id: 'orders', label: 'Orders', icon: '📦', roles: ['FACULTY', 'STUDENT'] },
    { id: 'workshops', label: 'Workshops', icon: '🎓', roles: ['TEACHER'] },
    { id: 'marketplace', label: 'Marketplace', icon: '🛒', roles: ['STUDENT'] },
    { id: 'reservations', label: 'Reservations', icon: '📅', roles: ['FACULTY', 'TEACHER', 'STUDENT'] },
    { id: 'myexams', label: 'My Exams', icon: '📚', roles: ['STUDENT'] },
    { id: 'notifications', label: 'Notifications', icon: '🔔', roles: ['FACULTY', 'TEACHER', 'STUDENT'] },
    { id: 'iot', label: 'IoT Dashboard', icon: '🌡️', roles: ['FACULTY', 'TEACHER', 'STUDENT'] },
  ].filter((link) => link.roles.includes(auth.role));

  return (
    <div className="page">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">✦</div>
          <span>
            SMART<span className="brand-accent">CAMPUS</span>
          </span>
        </div>
        <nav>
          {sidebarLinks.map((item) => (
            <div
              key={item.id}
              className={`nav-item ${activeTab === item.id ? 'active' : ''}`}
              onClick={() => setActiveTab(item.id)}
            >
              <span className="nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </div>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="nav-item">
            <span className="nav-icon">⚙️</span>
            <span>Settings</span>
          </div>
          <div className="nav-item danger" onClick={onLogout}>
            <span className="nav-icon">⎋</span>
            <span>Logout</span>
          </div>
        </div>
      </aside>

      <main className="content">
        <div className="topbar">
          <div>
            <h2>{sidebarLinks.find((l) => l.id === activeTab)?.label || 'Dashboard'}</h2>
            <p className="muted">
              {auth.username} • {auth.role} • {auth.tenantId}
            </p>
          </div>
          <div className="user-box">
            <div className="bubble">{auth.role}</div>
          </div>
        </div>

        <div style={{ padding: '24px' }}>
          {activeTab === 'overview' && <OverviewTab auth={auth} showToast={showToast} />}
          {activeTab === 'users' && <ManageUsersTab auth={auth} showToast={showToast} />}
          {activeTab === 'resources' && <ManageResourcesTab auth={auth} showToast={showToast} />}
          {activeTab === 'tickets' && <ManageTicketsTab auth={auth} showToast={showToast} />}
          {activeTab === 'exams' && <ManageExamsTab auth={auth} showToast={showToast} />}
          {activeTab === 'orders' && <OrdersTab auth={auth} showToast={showToast} />}
          {activeTab === 'workshops' && <WorkshopsTab auth={auth} showToast={showToast} />}
          {activeTab === 'marketplace' && <MarketplaceTab auth={auth} showToast={showToast} />}
          {activeTab === 'reservations' && <ReservationsTab auth={auth} showToast={showToast} />}
          {activeTab === 'myexams' && <MyExamsTab auth={auth} showToast={showToast} />}
          {activeTab === 'notifications' && <NotificationsTab auth={auth} showToast={showToast} />}
          {activeTab === 'iot' && <IotTab auth={auth} showToast={showToast} />}
        </div>
      </main>

      {/* Toast Notifications */}
      <div style={{ position: 'fixed', bottom: 24, right: 24, zIndex: 1000, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {toasts.map((toast) => (
          <div key={toast.id} className={`alert ${toast.type}`} style={{ minWidth: 300, boxShadow: 'var(--shadow-lg)' }}>
            <span>{toast.type === 'success' ? '✓' : toast.type === 'error' ? '✗' : 'ℹ'}</span>
            <span>{toast.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

type TabProps = {
  auth: AuthState;
  showToast: (message: string, type: Toast['type']) => void;
};

function OverviewTab({ auth, showToast }: TabProps) {
  const [stats, setStats] = useState<any>({});
  const [loading, setLoading] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}` };

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [ticketsRes, reservationsRes, examsRes, ordersRes] = await Promise.all([
          fetch(`${API_BASE}/marketplace/tickets`, { headers }).catch(() => null),
          fetch(`${API_BASE}/booking/reservations`, { headers }).catch(() => null),
          fetch(`${API_BASE}/exam/exams`, { headers }).catch(() => null),
          fetch(`${API_BASE}/order/orders`, { headers }).catch(() => null),
        ]);

        const tickets = ticketsRes?.ok ? await ticketsRes.json() : [];
        const reservations = reservationsRes?.ok ? await reservationsRes.json() : [];
        const exams = examsRes?.ok ? await examsRes.json() : [];
        const orders = ordersRes?.ok ? await ordersRes.json() : [];

        setStats({ tickets: tickets.length, reservations: reservations.length, exams: exams.length, orders: orders.length });
      } catch (error) {
        showToast('Failed to load statistics', 'error');
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, [auth.token]);

  const heroTitle = {
    FACULTY: 'Welcome, Faculty Admin! Design and manage your campus ecosystem.',
    TEACHER: 'Welcome, Teacher! Create engaging learning experiences.',
    STUDENT: 'Welcome, Student! Continue your learning journey.',
  }[auth.role];

  if (loading) {
    return (
      <div className="empty-state">
        <div className="loading" style={{ width: 40, height: 40, borderWidth: 4 }} />
        <p style={{ marginTop: 16 }}>Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="hero">
        <div className="eyebrow">Smart Campus Platform</div>
        <h1>{heroTitle}</h1>
        <p>Powered by microservices architecture with JWT authentication, Saga pattern, and Circuit Breaker resilience.</p>
      </div>

      <div className="grid grid-4">
        <div className="stat-card">
          <h3>🎫 Tickets</h3>
          <div className="stat-value">{stats.tickets || 0}</div>
          <div className="stat-label">Workshops & Conferences</div>
        </div>
        <div className="stat-card">
          <h3>📅 Reservations</h3>
          <div className="stat-value">{stats.reservations || 0}</div>
          <div className="stat-label">Room Bookings</div>
        </div>
        <div className="stat-card">
          <h3>📝 Exams</h3>
          <div className="stat-value">{stats.exams || 0}</div>
          <div className="stat-label">Scheduled Assessments</div>
        </div>
        <div className="stat-card">
          <h3>🛒 Orders</h3>
          <div className="stat-value">{stats.orders || 0}</div>
          <div className="stat-label">Completed Purchases</div>
        </div>
      </div>

      <div className="card mt-3">
        <h3>✨ Platform Features</h3>
        <div className="grid grid-3 mt-2">
          <div style={{ padding: 16, background: 'var(--bg-secondary)', borderRadius: 'var(--radius)' }}>
            <div style={{ fontSize: 24, marginBottom: 8 }}>🔄</div>
            <h4>Saga Pattern</h4>
            <p className="muted text-sm">Distributed transactions with compensating actions for reliable checkouts</p>
          </div>
          <div style={{ padding: 16, background: 'var(--bg-secondary)', borderRadius: 'var(--radius)' }}>
            <div style={{ fontSize: 24, marginBottom: 8 }}>🛡️</div>
            <h4>Circuit Breaker</h4>
            <p className="muted text-sm">Resilient communication preventing cascading failures</p>
          </div>
          <div style={{ padding: 16, background: 'var(--bg-secondary)', borderRadius: 'var(--radius)' }}>
            <div style={{ fontSize: 24, marginBottom: 8 }}>🔐</div>
            <h4>Multi-Tenancy</h4>
            <p className="muted text-sm">Strict tenant isolation with role-based access control</p>
          </div>
        </div>
      </div>
    </div>
  );
}

function ManageUsersTab({ auth, showToast }: TabProps) {
  const [users, setUsers] = useState<any[]>([]);
  const [selectedRole, setSelectedRole] = useState<Role>('TEACHER');
  const [newUser, setNewUser] = useState({ username: '', password: '', role: 'TEACHER' as Role, fullName: '' });
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}`, 'Content-Type': 'application/json' };

  const fetchUsers = useCallback(async () => {
    setFetching(true);
    try {
      const res = await fetch(`${API_BASE}/auth/faculty/users?role=${selectedRole}`, { headers });
      if (res.ok) {
        setUsers(await res.json());
      } else {
        showToast('Failed to load users', 'error');
      }
    } catch (error) {
      showToast('Error loading users', 'error');
    } finally {
      setFetching(false);
    }
  }, [selectedRole, auth.token]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const createUser = async () => {
    if (!newUser.username || !newUser.password) {
      showToast('Please fill in all required fields', 'warning');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/auth/faculty/users`, {
        method: 'POST',
        headers,
        body: JSON.stringify(newUser),
      });
      if (res.ok) {
        showToast(`User ${newUser.username} created successfully!`, 'success');
        setNewUser({ username: '', password: '', role: 'TEACHER', fullName: '' });
        fetchUsers();
      } else {
        const err = await res.json();
        showToast(err.message || 'Failed to create user', 'error');
      }
    } catch (error) {
      showToast('Error creating user', 'error');
    } finally {
      setLoading(false);
    }
  };

  const deleteUser = async (userId: number, username: string) => {
    if (!confirm(`Are you sure you want to delete user "${username}"?`)) return;
    
    try {
      const res = await fetch(`${API_BASE}/auth/faculty/users/${userId}`, {
        method: 'DELETE',
        headers,
      });
      if (res.ok) {
        showToast(`User ${username} deleted successfully!`, 'success');
        fetchUsers();
      } else {
        const err = await res.json();
        showToast(err.message || 'Failed to delete user', 'error');
      }
    } catch (error) {
      showToast('Error deleting user', 'error');
    }
  };

  return (
    <div>
      <div className="card mb-3">
        <h3>Create New User</h3>
        <div className="grid grid-2 mt-2">
          <div className="form-group">
            <label className="form-label">Username *</label>
            <input placeholder="Enter username" value={newUser.username} onChange={(e) => setNewUser({ ...newUser, username: e.target.value })} />
          </div>
          <div className="form-group">
            <label className="form-label">Password *</label>
            <input type="password" placeholder="Enter password" value={newUser.password} onChange={(e) => setNewUser({ ...newUser, password: e.target.value })} />
          </div>
          <div className="form-group">
            <label className="form-label">Full Name</label>
            <input placeholder="Enter full name" value={newUser.fullName} onChange={(e) => setNewUser({ ...newUser, fullName: e.target.value })} />
          </div>
          <div className="form-group">
            <label className="form-label">Role</label>
            <select value={newUser.role} onChange={(e) => setNewUser({ ...newUser, role: e.target.value as Role })}>
              <option value="TEACHER">Teacher</option>
              <option value="STUDENT">Student</option>
            </select>
          </div>
        </div>
        <button onClick={createUser} disabled={loading} className="mt-2">
          {loading ? <><span className="loading" /> Creating...</> : '✓ Create User'}
        </button>
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h3>User List</h3>
          <select value={selectedRole} onChange={(e) => setSelectedRole(e.target.value as Role)}>
            <option value="TEACHER">Teachers</option>
            <option value="STUDENT">Students</option>
          </select>
        </div>

        {fetching ? (
          <div className="empty-state">
            <div className="loading" />
            <p>Loading users...</p>
          </div>
        ) : users.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">👥</div>
            <p>No {selectedRole.toLowerCase()}s found</p>
          </div>
        ) : (
          <div className="table">
            <div className="table-header">
              <span>Username</span>
              <span>Full Name</span>
              <span>Role</span>
              <span>Tenant</span>
              <span>Actions</span>
            </div>
            {users.map((u) => (
              <div key={u.id} className="table-row">
                <span style={{ fontWeight: 600 }}>{u.username}</span>
                <span>{u.fullName || '—'}</span>
                <span>
                  <span className="chip primary">{u.role}</span>
                </span>
                <span className="muted">{u.tenantId}</span>
                <span>
                  <button
                    onClick={() => deleteUser(u.id, u.username)}
                    className="danger"
                    style={{ padding: '4px 8px', fontSize: '0.85rem' }}
                    title="Delete user"
                  >
                    🗑️ Delete
                  </button>
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ManageResourcesTab({ auth, showToast }: TabProps) {
  const [resources, setResources] = useState<any[]>([]);
  const [newResource, setNewResource] = useState({ name: '', type: 'CLASSROOM', capacity: 30 });
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}`, 'Content-Type': 'application/json' };

  const fetchResources = useCallback(async () => {
    setFetching(true);
    try {
      const res = await fetch(`${API_BASE}/booking/resources`, { headers });
      if (res.ok) {
        setResources(await res.json());
      }
    } catch (error) {
      showToast('Error loading resources', 'error');
    } finally {
      setFetching(false);
    }
  }, [auth.token]);

  useEffect(() => {
    fetchResources();
  }, [fetchResources]);

  const createResource = async () => {
    if (!newResource.name) {
      showToast('Please enter resource name', 'warning');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/booking/resources`, {
        method: 'POST',
        headers,
        body: JSON.stringify(newResource),
      });
      if (res.ok) {
        showToast(`Resource "${newResource.name}" created!`, 'success');
        setNewResource({ name: '', type: 'CLASSROOM', capacity: 30 });
        fetchResources();
      } else {
        showToast('Failed to create resource', 'error');
      }
    } catch (error) {
      showToast('Error creating resource', 'error');
    } finally {
      setLoading(false);
    }
  };

  const deleteResource = async (resourceId: number, name: string) => {
    if (!confirm(`Are you sure you want to delete resource "${name}"? All reservations for this resource will also be deleted.`)) return;
    
    try {
      const res = await fetch(`${API_BASE}/booking/resources/${resourceId}`, {
        method: 'DELETE',
        headers,
      });
      if (res.ok) {
        showToast(`Resource ${name} deleted successfully!`, 'success');
        fetchResources();
      } else {
        const err = await res.json().catch(() => ({}));
        showToast(err.message || 'Failed to delete resource', 'error');
      }
    } catch (error) {
      showToast('Error deleting resource', 'error');
    }
  };

  return (
    <div>
      <div className="card mb-3">
        <h3>Create New Resource</h3>
        <div className="grid grid-3 mt-2">
          <div className="form-group">
            <label className="form-label">Resource Name *</label>
            <input placeholder="e.g., Room 101" value={newResource.name} onChange={(e) => setNewResource({ ...newResource, name: e.target.value })} />
          </div>
          <div className="form-group">
            <label className="form-label">Type</label>
            <select value={newResource.type} onChange={(e) => setNewResource({ ...newResource, type: e.target.value })}>
              <option value="CLASSROOM">Classroom</option>
              <option value="LAB">Lab</option>
              <option value="AUDITORIUM">Auditorium</option>
              <option value="MEETING_ROOM">Meeting Room</option>
            </select>
          </div>
          <div className="form-group">
            <label className="form-label">Capacity</label>
            <input type="number" placeholder="30" value={newResource.capacity} onChange={(e) => setNewResource({ ...newResource, capacity: parseInt(e.target.value) || 0 })} />
          </div>
        </div>
        <button onClick={createResource} disabled={loading} className="mt-2">
          {loading ? <><span className="loading" /> Creating...</> : '✓ Create Resource'}
        </button>
      </div>

      <div className="card">
        <h3>Resources List</h3>
        {fetching ? (
          <div className="empty-state">
            <div className="loading" />
            <p>Loading resources...</p>
          </div>
        ) : resources.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🏢</div>
            <p>No resources found. Create your first one!</p>
          </div>
        ) : (
          <div className="table">
            <div className="table-header">
              <span>Name</span>
              <span>Type</span>
              <span>Capacity</span>
              <span>Status</span>
              <span>Actions</span>
            </div>
            {resources.map((r) => (
              <div key={r.id} className="table-row">
                <span style={{ fontWeight: 600 }}>{r.name}</span>
                <span>
                  <span className="chip">{r.type}</span>
                </span>
                <span>{r.capacity} people</span>
                <span>
                  <span className={`badge ${r.available !== false ? 'success' : 'warning'}`}>
                    {r.available !== false ? '✓ Available' : 'In Use'}
                  </span>
                </span>
                <span>
                  <button
                    onClick={() => deleteResource(r.id, r.name)}
                    className="danger"
                    style={{ padding: '4px 8px', fontSize: '0.85rem' }}
                    title="Delete resource"
                  >
                    🗑️ Delete
                  </button>
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ManageTicketsTab({ auth, showToast }: TabProps) {
  const [tickets, setTickets] = useState<any[]>([]);
  const [teachers, setTeachers] = useState<any[]>([]);
  const [newTicket, setNewTicket] = useState({ title: '', description: '', type: 'WORKSHOP', price: 100, inventory: 50, assignedTeacher: '' });
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}`, 'Content-Type': 'application/json' };

  const fetchTickets = useCallback(async () => {
    setFetching(true);
    try {
      const res = await fetch(`${API_BASE}/marketplace/tickets`, { headers });
      if (res.ok) {
        setTickets(await res.json());
      }
    } catch (error) {
      showToast('Error loading tickets', 'error');
    } finally {
      setFetching(false);
    }
  }, [auth.token]);

  useEffect(() => {
    fetchTickets();
    
    // Fetch teachers list
    const fetchTeachers = async () => {
      try {
        const res = await fetch(`${API_BASE}/auth/faculty/users?role=TEACHER`, { headers });
        if (res.ok) {
          setTeachers(await res.json());
        }
      } catch (error) {
        console.error('Error loading teachers');
      }
    };
    fetchTeachers();
  }, [fetchTickets]);

  const createTicket = async () => {
    if (!newTicket.title) {
      showToast('Please enter ticket title', 'warning');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/marketplace/tickets`, {
        method: 'POST',
        headers,
        body: JSON.stringify(newTicket),
      });
      if (res.ok) {
        showToast(`Ticket "${newTicket.title}" created!`, 'success');
        setNewTicket({ title: '', description: '', type: 'WORKSHOP', price: 100, inventory: 50, assignedTeacher: '' });
        fetchTickets();
      } else {
        showToast('Failed to create ticket', 'error');
      }
    } catch (error) {
      showToast('Error creating ticket', 'error');
    } finally {
      setLoading(false);
    }
  };

  const deleteTicket = async (ticketId: number, title: string) => {
    if (!confirm(`Are you sure you want to delete ticket "${title}"?`)) return;
    
    try {
      const res = await fetch(`${API_BASE}/marketplace/tickets/${ticketId}`, {
        method: 'DELETE',
        headers,
      });
      if (res.ok) {
        showToast(`Ticket ${title} deleted successfully!`, 'success');
        fetchTickets();
      } else {
        const err = await res.json().catch(() => ({}));
        showToast(err.message || 'Failed to delete ticket', 'error');
      }
    } catch (error) {
      showToast('Error deleting ticket', 'error');
    }
  };

  return (
    <div>
      <div className="card mb-3">
        <h3>Create New Ticket</h3>
        <div className="grid grid-2 mt-2">
          <div className="form-group">
            <label className="form-label">Title *</label>
            <input placeholder="e.g., React Workshop" value={newTicket.title} onChange={(e) => setNewTicket({ ...newTicket, title: e.target.value })} />
          </div>
          <div className="form-group">
            <label className="form-label">Type</label>
            <select value={newTicket.type} onChange={(e) => setNewTicket({ ...newTicket, type: e.target.value })}>
              <option value="WORKSHOP">Workshop</option>
              <option value="CONFERENCE">Conference</option>
            </select>
          </div>
          <div className="form-group" style={{ gridColumn: '1 / -1' }}>
            <label className="form-label">Description</label>
            <textarea rows={3} placeholder="Enter description" value={newTicket.description} onChange={(e) => setNewTicket({ ...newTicket, description: e.target.value })} />
          </div>
          <div className="form-group">
            <label className="form-label">Price ($)</label>
            <input type="number" placeholder="100" value={newTicket.price} onChange={(e) => setNewTicket({ ...newTicket, price: parseFloat(e.target.value) || 0 })} />
          </div>
          <div className="form-group">
            <label className="form-label">Inventory</label>
            <input type="number" placeholder="50" value={newTicket.inventory} onChange={(e) => setNewTicket({ ...newTicket, inventory: parseInt(e.target.value) || 0 })} />
          </div>
          <div className="form-group" style={{ gridColumn: '1 / -1' }}>
            <label className="form-label">Assigned Teacher (Optional)</label>
            <select value={newTicket.assignedTeacher} onChange={(e) => setNewTicket({ ...newTicket, assignedTeacher: e.target.value })}>
              <option value="">-- Select Teacher --</option>
              {teachers.map((t) => (
                <option key={t.id} value={t.username}>
                  {t.fullName} (@{t.username})
                </option>
              ))}
            </select>
          </div>
        </div>
        <button onClick={createTicket} disabled={loading} className="mt-2">
          {loading ? <><span className="loading" /> Creating...</> : '✓ Create Ticket'}
        </button>
      </div>

      <div className="card">
        <h3>Tickets List</h3>
        {fetching ? (
          <div className="empty-state">
            <div className="loading" />
            <p>Loading tickets...</p>
          </div>
        ) : tickets.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🎫</div>
            <p>No tickets found. Create your first workshop or conference!</p>
          </div>
        ) : (
          <div className="grid grid-3">
            {tickets.map((t) => (
              <div key={t.id} className="card" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 12 }}>
                  <span className="chip primary">{t.type}</span>
                  <span style={{ fontWeight: 700, fontSize: 18, color: 'var(--primary)' }}>${t.price}</span>
                </div>
                <h4>{t.title}</h4>
                <p className="muted" style={{ marginBottom: 12 }}>
                  {t.description || 'No description'}
                </p>
                {t.assignedTeacher && (
                  <div style={{ padding: '6px 10px', background: 'var(--success-bg)', color: 'var(--success)', borderRadius: 'var(--radius)', fontSize: 11, marginBottom: 8, fontWeight: 600 }}>
                    👤 Assigned to: {t.assignedTeacher}
                  </div>
                )}
                <div style={{ padding: '8px 12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius)', fontSize: 12, marginBottom: 12 }}>
                  <span className="muted">{t.inventory} seats remaining</span>
                </div>
                <button
                  onClick={() => deleteTicket(t.id, t.title)}
                  className="danger"
                  style={{ width: '100%', padding: '6px', fontSize: '0.85rem' }}
                  title="Delete ticket"
                >
                  🗑️ Delete Ticket
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ManageExamsTab({ auth, showToast }: TabProps) {
  const [exams, setExams] = useState<any[]>([]);
  const [courses, setCourses] = useState<any[]>([]);
  const [newExam, setNewExam] = useState({ courseId: 0, title: '', startTime: '', durationMinutes: 60 });
  const [newCourse, setNewCourse] = useState({ title: '', description: '' });
  const [selectedExam, setSelectedExam] = useState<any>(null);
  const [questions, setQuestions] = useState<any[]>([]);
  const [newQuestion, setNewQuestion] = useState({ questionText: '', options: ['', '', '', ''], correctOptionIndex: 0, points: 10 });
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [showCourseForm, setShowCourseForm] = useState(false);
  const headers = { Authorization: `Bearer ${auth.token}`, 'Content-Type': 'application/json' };

  const fetchData = useCallback(async () => {
    setFetching(true);
    try {
      const [examsRes, coursesRes] = await Promise.all([
        fetch(`${API_BASE}/exam/exams`, { headers }),
        fetch(`${API_BASE}/exam/courses`, { headers }),
      ]);
      if (examsRes.ok) setExams(await examsRes.json());
      if (coursesRes.ok) {
        const coursesData = await coursesRes.json();
        setCourses(coursesData);
        if (coursesData.length > 0) {
          setNewExam(prev => ({ ...prev, courseId: coursesData[0].id }));
        }
      }
    } catch (error) {
      showToast('Error loading data', 'error');
    } finally {
      setFetching(false);
    }
  }, [auth.token]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const createCourse = async () => {
    if (!newCourse.title) {
      showToast('Please enter course title', 'warning');
      return;
    }
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/exam/courses`, {
        method: 'POST',
        headers,
        body: JSON.stringify(newCourse),
      });
      if (res.ok) {
        showToast(`Course "${newCourse.title}" created!`, 'success');
        setNewCourse({ title: '', description: '' });
        setShowCourseForm(false);
        fetchData();
      } else {
        showToast('Failed to create course', 'error');
      }
    } catch (error) {
      showToast('Error creating course', 'error');
    } finally {
      setLoading(false);
    }
  };

  const createExam = async () => {
    if (!newExam.title || !newExam.startTime) {
      showToast('Please fill in all fields', 'warning');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/exam/exams`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ ...newExam, startTime: new Date(newExam.startTime).toISOString() }),
      });
      if (res.ok) {
        showToast(`Exam "${newExam.title}" created!`, 'success');
        setNewExam({ courseId: 0, title: '', startTime: '', durationMinutes: 60 });
        fetchData();
      } else {
        showToast('Failed to create exam', 'error');
      }
    } catch (error) {
      showToast('Error creating exam', 'error');
    } finally {
      setLoading(false);
    }
  };

  const deleteExam = async (examId: number, title: string) => {
    if (!confirm(`Are you sure you want to delete exam "${title}"?`)) return;
    
    try {
      const res = await fetch(`${API_BASE}/exam/exams/${examId}`, {
        method: 'DELETE',
        headers,
      });
      if (res.ok) {
        showToast(`Exam ${title} deleted successfully!`, 'success');
        if (selectedExam?.id === examId) {
          setSelectedExam(null);
          setQuestions([]);
        }
        fetchData();
      } else {
        const err = await res.json().catch(() => ({}));
        showToast(err.message || 'Failed to delete exam', 'error');
      }
    } catch (error) {
      showToast('Error deleting exam', 'error');
    }
  };

  const manageQuestions = async (exam: any) => {
    setSelectedExam(exam);
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/exam/questions/exam/${exam.id}`, { headers });
      if (res.ok) {
        setQuestions(await res.json());
      } else {
        setQuestions([]);
      }
    } catch (error) {
      showToast('Error loading questions', 'error');
      setQuestions([]);
    } finally {
      setLoading(false);
    }
  };

  const addQuestion = async () => {
    if (!newQuestion.questionText || newQuestion.options.some(o => !o)) {
      showToast('Please fill all fields and options', 'warning');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/exam/questions`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ ...newQuestion, examId: selectedExam.id }),
      });
      if (res.ok) {
        showToast('Question added successfully!', 'success');
        setNewQuestion({ questionText: '', options: ['', '', '', ''], correctOptionIndex: 0, points: 10 });
        manageQuestions(selectedExam);
      } else {
        showToast('Failed to add question', 'error');
      }
    } catch (error) {
      showToast('Error adding question', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {courses.length === 0 && !showCourseForm && (
        <div className="card mb-3" style={{ background: 'var(--warning-bg)', borderColor: 'var(--warning)' }}>
          <p>⚠️ You need to create a course first before creating exams.</p>
          <button onClick={() => setShowCourseForm(true)} style={{ marginTop: '8px' }}>
            + Create First Course
          </button>
        </div>
      )}

      {showCourseForm && (
        <div className="card mb-3">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>Create New Course</h3>
            <button onClick={() => setShowCourseForm(false)} style={{ padding: '4px 12px', fontSize: '0.9rem' }}>
              Cancel
            </button>
          </div>
          <div className="grid grid-2 mt-2">
            <div className="form-group">
              <label className="form-label">Course Title *</label>
              <input placeholder="e.g., Data Structures" value={newCourse.title} onChange={(e) => setNewCourse({ ...newCourse, title: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">Description</label>
              <input placeholder="Course description" value={newCourse.description} onChange={(e) => setNewCourse({ ...newCourse, description: e.target.value })} />
            </div>
          </div>
          <button onClick={createCourse} disabled={loading} className="mt-2">
            {loading ? <><span className="loading" /> Creating...</> : '✓ Create Course'}
          </button>
        </div>
      )}

      {courses.length > 0 && (
        <div className="card mb-3">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>Create New Exam</h3>
            {!showCourseForm && (
              <button onClick={() => setShowCourseForm(true)} style={{ padding: '4px 12px', fontSize: '0.9rem' }}>
                + New Course
              </button>
            )}
          </div>
          <div className="grid grid-3 mt-2">
            <div className="form-group">
              <label className="form-label">Course *</label>
              <select value={newExam.courseId} onChange={(e) => setNewExam({ ...newExam, courseId: parseInt(e.target.value) })}>
                <option value={0}>Select Course</option>
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.title}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Exam Title *</label>
              <input placeholder="e.g., Midterm Exam" value={newExam.title} onChange={(e) => setNewExam({ ...newExam, title: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">Start Time *</label>
              <input type="datetime-local" value={newExam.startTime} onChange={(e) => setNewExam({ ...newExam, startTime: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">Duration (minutes) *</label>
              <input type="number" min="1" placeholder="60" value={newExam.durationMinutes} onChange={(e) => setNewExam({ ...newExam, durationMinutes: parseInt(e.target.value) || 60 })} />
            </div>
          </div>
          <button onClick={createExam} disabled={loading || newExam.courseId === 0} className="mt-2">
            {loading ? <><span className="loading" /> Creating...</> : '✓ Create Exam'}
          </button>
        </div>
      )}

      <div className="card">
        <h3>Exams List</h3>
        {fetching ? (
          <div className="empty-state">
            <div className="loading" />
            <p>Loading exams...</p>
          </div>
        ) : exams.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📝</div>
            <p>No exams found. Create your first exam!</p>
          </div>
        ) : (
          <div className="table">
            <div className="table-header">
              <span>Title</span>
              <span>Course ID</span>
              <span>Start Time</span>
              <span>Actions</span>
            </div>
            {exams.map((e) => (
              <div key={e.id} className="table-row">
                <span style={{ fontWeight: 600 }}>{e.title}</span>
                <span>
                  <span className="chip">Course #{e.courseId}</span>
                </span>
                <span className="muted">{new Date(e.startTime).toLocaleString()}</span>
                <span style={{ display: 'flex', gap: '8px' }}>
                  <button
                    onClick={() => manageQuestions(e)}
                    style={{ padding: '4px 8px', fontSize: '0.85rem' }}
                    title="Manage Questions"
                  >
                    📝 Questions
                  </button>
                  <button
                    onClick={() => deleteExam(e.id, e.title)}
                    className="danger"
                    style={{ padding: '4px 8px', fontSize: '0.85rem' }}
                    title="Delete exam"
                  >
                    🗑️ Delete
                  </button>
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {selectedExam && (
        <div className="card mt-3">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3>Manage Questions for "{selectedExam.title}"</h3>
            <button onClick={() => { setSelectedExam(null); setQuestions([]); }} style={{ padding: '4px 12px', fontSize: '0.9rem' }}>✕ Close</button>
          </div>

          <div className="card mb-3" style={{ background: 'var(--bg-secondary)' }}>
            <h4>Add New Question</h4>
            <div className="form-group mt-2">
              <label className="form-label">Question Text *</label>
              <textarea 
                rows={3} 
                placeholder="Enter your question here..." 
                value={newQuestion.questionText} 
                onChange={(e) => setNewQuestion({ ...newQuestion, questionText: e.target.value })} 
              />
            </div>
            
            <div className="grid grid-2 mt-2">
              {newQuestion.options.map((opt, idx) => (
                <div key={idx} className="form-group">
                  <label className="form-label">Option {idx + 1} *</label>
                  <input 
                    placeholder={`Option ${idx + 1}`} 
                    value={opt} 
                    onChange={(e) => {
                      const newOpts = [...newQuestion.options];
                      newOpts[idx] = e.target.value;
                      setNewQuestion({ ...newQuestion, options: newOpts });
                    }} 
                  />
                </div>
              ))}
            </div>
            
            <div className="grid grid-2 mt-2">
              <div className="form-group">
                <label className="form-label">Correct Answer</label>
                <select value={newQuestion.correctOptionIndex} onChange={(e) => setNewQuestion({ ...newQuestion, correctOptionIndex: parseInt(e.target.value) })}>
                  {newQuestion.options.map((_opt, idx) => (
                    <option key={idx} value={idx}>Option {idx + 1}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Points</label>
                <input 
                  type="number" 
                  min="1" 
                  value={newQuestion.points} 
                  onChange={(e) => setNewQuestion({ ...newQuestion, points: parseInt(e.target.value) || 10 })} 
                />
              </div>
            </div>
            
            <button onClick={addQuestion} disabled={loading} className="mt-2">
              {loading ? <><span className="loading" /> Adding...</> : '✓ Add Question'}
            </button>
          </div>

          <div>
            <h4>Questions ({questions.length})</h4>
            {questions.length === 0 ? (
              <div className="empty-state">
                <div className="empty-state-icon">❓</div>
                <p>No questions yet. Add your first question above!</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '16px' }}>
                {questions.map((q, idx) => (
                  <div key={q.id} className="card" style={{ padding: '16px', background: 'var(--bg-secondary)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <strong>Q{idx + 1}. {q.questionText}</strong>
                      <span className="chip success">{q.points} pts</span>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginTop: '8px' }}>
                      {q.options.map((option: string, optIdx: number) => (
                        <div 
                          key={optIdx} 
                          style={{ 
                            padding: '8px 12px', 
                            background: optIdx === q.correctOptionIndex ? 'var(--success-bg)' : 'var(--bg)', 
                            border: optIdx === q.correctOptionIndex ? '2px solid var(--success)' : '1px solid var(--border)',
                            borderRadius: 'var(--radius)',
                            fontSize: '0.9rem'
                          }}
                        >
                          {optIdx === q.correctOptionIndex && '✓ '}{option}
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function WorkshopsTab({ auth, showToast }: TabProps) {
  const [tickets, setTickets] = useState<any[]>([]);
  const [selectedTicket, setSelectedTicket] = useState<any>(null);
  const [registrations, setRegistrations] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingRegs, setLoadingRegs] = useState(false);
  const headers = { Authorization: `Bearer ${auth.token}` };

  useEffect(() => {
    const fetchTickets = async () => {
      try {
        const res = await fetch(`${API_BASE}/marketplace/tickets/my`, { headers });
        if (res.ok) {
          setTickets(await res.json());
        }
      } catch (error) {
        showToast('Error loading workshops', 'error');
      } finally {
        setLoading(false);
      }
    };
    fetchTickets();
  }, [auth.token]);

  const viewRegistrations = async (ticket: any) => {
    setSelectedTicket(ticket);
    setLoadingRegs(true);
    try {
      const res = await fetch(`${API_BASE}/orders/by-ticket/${ticket.id}`, { headers });
      if (res.ok) {
        setRegistrations(await res.json());
      } else {
        setRegistrations([]);
      }
    } catch (error) {
      showToast('Error loading registrations', 'error');
      setRegistrations([]);
    } finally {
      setLoadingRegs(false);
    }
  };

  if (loading) {
    return (
      <div className="empty-state">
        <div className="loading" />
        <p>Loading workshops...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="card">
        <h3>My Assigned Workshops & Conferences</h3>
        {tickets.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🎓</div>
            <p>No workshops or conferences assigned to you yet.</p>
          </div>
        ) : (
          <div className="grid grid-3 mt-2">
            {tickets.map((t) => (
              <div key={t.id} className="card" style={{ padding: '20px' }}>
                <span className="chip primary">{t.type}</span>
                <h4 className="mt-1">{t.title}</h4>
                <p className="muted mt-1">{t.description || 'No description'}</p>
                <div style={{ marginTop: 12, padding: '8px 12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius)', fontSize: 12, marginBottom: 12 }}>
                  <span className="muted">{t.inventory} seats remaining • ${t.price}</span>
                </div>
                <button 
                  onClick={() => viewRegistrations(t)}
                  style={{ width: '100%', padding: '8px', fontSize: '0.9rem' }}
                >
                  👥 View Registrations
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {selectedTicket && (
        <div className="card mt-3">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3>Registrations for "{selectedTicket.title}"</h3>
            <button onClick={() => setSelectedTicket(null)} style={{ padding: '4px 12px', fontSize: '0.9rem' }}>✕ Close</button>
          </div>
          {loadingRegs ? (
            <div className="empty-state">
              <div className="loading" />
              <p>Loading registrations...</p>
            </div>
          ) : registrations.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">👤</div>
              <p>No registrations yet.</p>
            </div>
          ) : (
            <div className="table">
              <table>
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>User</th>
                    <th>Quantity</th>
                    <th>Status</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {registrations.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>{order.userId}</td>
                      <td>{order.items.find((i: any) => i.ticketId === selectedTicket.id)?.quantity || 0}</td>
                      <td><span className="chip success">{order.status}</span></td>
                      <td>{new Date(order.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function MarketplaceTab({ auth, showToast }: TabProps) {
  const [tickets, setTickets] = useState<any[]>([]);
  const [cart, setCart] = useState<{ ticketId: number; quantity: number }[]>([]);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}`, 'Content-Type': 'application/json' };

  const fetchTickets = useCallback(async () => {
    setFetching(true);
    try {
      const res = await fetch(`${API_BASE}/marketplace/tickets`, { headers });
      if (res.ok) {
        setTickets(await res.json());
      }
    } catch (error) {
      showToast('Error loading tickets', 'error');
    } finally {
      setFetching(false);
    }
  }, [auth.token]);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  const addToCart = (ticketId: number) => {
    const existing = cart.find((c) => c.ticketId === ticketId);
    if (existing) {
      setCart(cart.map((c) => (c.ticketId === ticketId ? { ...c, quantity: c.quantity + 1 } : c)));
    } else {
      setCart([...cart, { ticketId, quantity: 1 }]);
    }
    showToast('Added to cart!', 'success');
  };

  const checkout = async () => {
    if (cart.length === 0) return;
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/orders/checkout`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ items: cart }),
      });
      if (res.ok) {
        showToast('Order placed successfully! (Saga completed)', 'success');
        setCart([]);
        fetchTickets();
      } else {
        const err = await res.json();
        showToast(`Checkout failed: ${err.error || 'Unknown error'}`, 'error');
      }
    } catch (error) {
      showToast('Error during checkout', 'error');
    } finally {
      setLoading(false);
    }
  };

  const totalAmount = cart.reduce((sum, item) => {
    const ticket = tickets.find((t) => t.id === item.ticketId);
    return sum + (ticket?.price || 0) * item.quantity;
  }, 0);

  return (
    <div>
      <div className="card mb-3">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3>🛒 Shopping Cart ({cart.length})</h3>
          <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--primary)' }}>${totalAmount.toFixed(2)}</div>
        </div>
        {cart.length > 0 ? (
          <>
            <div className="table mt-2">
              <div className="table-header">
                <span>Ticket</span>
                <span>Quantity</span>
                <span>Price</span>
              </div>
              {cart.map((c, idx) => {
                const ticket = tickets.find((t) => t.id === c.ticketId);
                return (
                  <div key={idx} className="table-row">
                    <span style={{ fontWeight: 600 }}>{ticket?.title || `Ticket #${c.ticketId}`}</span>
                    <span>{c.quantity}x</span>
                    <span>${((ticket?.price || 0) * c.quantity).toFixed(2)}</span>
                  </div>
                );
              })}
            </div>
            <button onClick={checkout} disabled={loading} className="mt-2">
              {loading ? <><span className="loading" /> Processing...</> : '✓ Checkout (Saga Pattern)'}
            </button>
          </>
        ) : (
          <div className="empty-state">
            <div className="empty-state-icon">🛒</div>
            <p>Your cart is empty. Browse tickets below!</p>
          </div>
        )}
      </div>

      <div className="card">
        <h3>Available Tickets</h3>
        {fetching ? (
          <div className="empty-state">
            <div className="loading" />
            <p>Loading marketplace...</p>
          </div>
        ) : tickets.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🎫</div>
            <p>No tickets available at the moment.</p>
          </div>
        ) : (
          <div className="grid grid-3 mt-2">
            {tickets.map((t) => (
              <div key={t.id} className="card" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 12 }}>
                  <span className="chip primary">{t.type}</span>
                  <span style={{ fontWeight: 700, fontSize: 20, color: 'var(--primary)' }}>${t.price}</span>
                </div>
                <h4>{t.title}</h4>
                <p className="muted" style={{ marginBottom: 12 }}>
                  {t.description || 'No description'}
                </p>
                <div style={{ padding: '8px 12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius)', fontSize: 12, marginBottom: 12 }}>
                  <span className="muted">{t.inventory} seats left</span>
                </div>
                <button onClick={() => addToCart(t.id)} style={{ width: '100%' }}>
                  Add to Cart
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ReservationsTab({ auth, showToast }: TabProps) {
  const [resources, setResources] = useState<any[]>([]);
  const [reservations, setReservations] = useState<any[]>([]);
  const [newReservation, setNewReservation] = useState({ resourceId: 0, startTime: '', endTime: '' });
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}`, 'Content-Type': 'application/json' };

  const fetchData = useCallback(async () => {
    setFetching(true);
    try {
      const reservationsEndpoint = auth.role === 'FACULTY' 
        ? '/booking/reservations' 
        : '/booking/reservations/my';
      const [resourcesRes, reservationsRes] = await Promise.all([
        fetch(`${API_BASE}/booking/resources`, { headers }),
        fetch(`${API_BASE}${reservationsEndpoint}`, { headers }),
      ]);
      if (resourcesRes.ok) setResources(await resourcesRes.json());
      if (reservationsRes.ok) setReservations(await reservationsRes.json());
    } catch (error) {
      showToast('Error loading data', 'error');
    } finally {
      setFetching(false);
    }
  }, [auth.token, auth.role]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const createReservation = async () => {
    if (!newReservation.resourceId || !newReservation.startTime || !newReservation.endTime) {
      showToast('Please fill in all fields', 'warning');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/booking/reservations`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          resourceId: newReservation.resourceId,
          startTime: new Date(newReservation.startTime).toISOString(),
          endTime: new Date(newReservation.endTime).toISOString(),
        }),
      });
      if (res.ok) {
        showToast('Reservation created successfully!', 'success');
        setNewReservation({ resourceId: 0, startTime: '', endTime: '' });
        fetchData();
      } else {
        const err = await res.json();
        showToast(err.error || 'Failed to create reservation', 'error');
      }
    } catch (error) {
      showToast('Error creating reservation', 'error');
    } finally {
      setLoading(false);
    }
  };

  const cancelReservation = async (reservationId: number) => {
    if (!confirm('Are you sure you want to cancel this reservation?')) return;
    
    try {
      const res = await fetch(`${API_BASE}/booking/reservations/${reservationId}`, {
        method: 'DELETE',
        headers,
      });
      if (res.ok) {
        showToast('Reservation cancelled successfully!', 'success');
        fetchData();
      } else {
        const err = await res.json().catch(() => ({}));
        showToast(err.message || 'Failed to cancel reservation', 'error');
      }
    } catch (error) {
      showToast('Error cancelling reservation', 'error');
    }
  };

  return (
    <div>
      <div className="card mb-3">
        <h3>Create New Reservation</h3>
        <div className="grid grid-3 mt-2">
          <div className="form-group">
            <label className="form-label">Resource *</label>
            <select value={newReservation.resourceId} onChange={(e) => setNewReservation({ ...newReservation, resourceId: parseInt(e.target.value) })}>
              <option value={0}>Select Resource</option>
              {resources.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name} ({r.type})
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label className="form-label">Start Time *</label>
            <input type="datetime-local" value={newReservation.startTime} onChange={(e) => setNewReservation({ ...newReservation, startTime: e.target.value })} />
          </div>
          <div className="form-group">
            <label className="form-label">End Time *</label>
            <input type="datetime-local" value={newReservation.endTime} onChange={(e) => setNewReservation({ ...newReservation, endTime: e.target.value })} />
          </div>
        </div>
        <button onClick={createReservation} disabled={loading} className="mt-2">
          {loading ? <><span className="loading" /> Creating...</> : '✓ Create Reservation (Concurrency Safe)'}
        </button>
      </div>

      <div className="card">
        <h3>My Reservations</h3>
        {fetching ? (
          <div className="empty-state">
            <div className="loading" />
            <p>Loading reservations...</p>
          </div>
        ) : reservations.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📅</div>
            <p>No reservations found. Create your first one!</p>
          </div>
        ) : (
          <div className="table">
            <div className="table-header">
              <span>Resource</span>
              {auth.role === 'FACULTY' && <span>User</span>}
              <span>Start Time</span>
              <span>End Time</span>
              <span>Actions</span>
            </div>
            {reservations.map((r) => (
              <div key={r.id} className="table-row">
                <span style={{ fontWeight: 600 }}>{resources.find((res) => res.id === r.resourceId)?.name || `Resource #${r.resourceId}`}</span>
                {auth.role === 'FACULTY' && <span className="muted">{r.userId || 'Unknown'}</span>}
                <span className="muted">{new Date(r.startTime).toLocaleString()}</span>
                <span className="muted">{new Date(r.endTime).toLocaleString()}</span>
                <span>
                  <button
                    onClick={() => cancelReservation(r.id)}
                    className="danger"
                    style={{ padding: '4px 8px', fontSize: '0.85rem' }}
                    title="Cancel reservation"
                  >
                    ❌ Cancel
                  </button>
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function MyExamsTab({ auth, showToast }: TabProps) {
  const [exams, setExams] = useState<any[]>([]);
  const [activeExam, setActiveExam] = useState<any>(null);
  const [questions, setQuestions] = useState<any[]>([]);
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [attemptId, setAttemptId] = useState<number | null>(null);
  const [timeLeft, setTimeLeft] = useState<number>(0);
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}`, 'Content-Type': 'application/json' };

  const fetchExams = useCallback(async () => {
    setFetching(true);
    try {
      const res = await fetch(`${API_BASE}/exam/exams`, { headers });
      if (res.ok) {
        setExams(await res.json());
      }
    } catch (error) {
      showToast('Error loading exams', 'error');
    } finally {
      setFetching(false);
    }
  }, [auth.token]);

  useEffect(() => {
    fetchExams();
  }, [fetchExams]);

  // Timer countdown
  useEffect(() => {
    if (activeExam && timeLeft > 0) {
      const timer = setInterval(() => {
        setTimeLeft(prev => {
          if (prev <= 1) {
            handleSubmit();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
      return () => clearInterval(timer);
    }
  }, [activeExam, timeLeft]);

  const startExam = async (exam: any) => {
    setLoading(true);
    try {
      const [attemptRes, questionsRes] = await Promise.all([
        fetch(`${API_BASE}/exam/exams/${exam.id}/start`, { method: 'POST', headers }),
        fetch(`${API_BASE}/exam/exams/${exam.id}/questions`, { headers })
      ]);

      if (!attemptRes.ok) {
        const errorData = await attemptRes.json().catch(() => ({}));
        showToast(errorData.error || 'Failed to start exam', 'error');
        return;
      }

      const attemptData = await attemptRes.json();
      const questionsData = questionsRes.ok ? await questionsRes.json() : [];

      setActiveExam(exam);
      setQuestions(questionsData);
      setAttemptId(attemptData.id);
      setTimeLeft(exam.durationMinutes * 60);
      setAnswers({});
      showToast('Exam started! Good luck! 🎓', 'success');
    } catch (error) {
      showToast('Error starting exam', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!attemptId) return;

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/exam/exams/attempts/${attemptId}/submit`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ answers }),
      });

      if (res.ok) {
        const attemptResult = await res.json();
        
        // Fetch detailed result
        const resultRes = await fetch(`${API_BASE}/exam/exams/attempts/${attemptId}/result`, { headers });
        if (resultRes.ok) {
          const resultData = await resultRes.json();
          setResult(resultData);
        }
        
        showToast(`Exam submitted! Score: ${attemptResult.score}/${attemptResult.maxScore}`, 'success');
        setActiveExam(null);
        setQuestions([]);
        setAnswers({});
        setTimeLeft(0);
      } else {
        showToast('Failed to submit exam', 'error');
      }
    } catch (error) {
      showToast('Error submitting exam', 'error');
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  if (result) {
    return (
      <div>
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>📊 Exam Result</h3>
            <button onClick={() => { setResult(null); fetchExams(); }}>← Back to Exams</button>
          </div>
          
          <div style={{ padding: '24px', background: 'var(--success-bg)', borderRadius: 'var(--radius)', marginTop: '16px', textAlign: 'center' }}>
            <h2 style={{ fontSize: '3rem', color: 'var(--success)', margin: 0 }}>
              {result.attempt.score}/{result.attempt.maxScore}
            </h2>
            <p style={{ fontSize: '1.2rem', color: 'var(--success)', marginTop: '8px' }}>
              {((result.attempt.score / result.attempt.maxScore) * 100).toFixed(1)}%
            </p>
          </div>

          <div style={{ marginTop: '24px' }}>
            <h4>Your Answers:</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '12px' }}>
              {result.answers.map((ans: any, idx: number) => {
                const question = questions.find(q => q.id === ans.questionId);
                return (
                  <div key={ans.id} className="card" style={{ 
                    padding: '16px', 
                    background: ans.isCorrect ? 'var(--success-bg)' : 'var(--error-bg)',
                    border: `2px solid ${ans.isCorrect ? 'var(--success)' : 'var(--error)'}`
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <strong>Q{idx + 1}</strong>
                      <span>{ans.isCorrect ? '✓ Correct' : '✗ Wrong'}</span>
                    </div>
                    {question && (
                      <p style={{ marginTop: '8px', fontSize: '0.9rem' }}>
                        Your answer: <strong>{question.options[ans.selectedOptionIndex] || 'Not answered'}</strong>
                      </p>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (activeExam) {
    return (
      <div>
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>📝 {activeExam.title}</h3>
            <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
              <div style={{ 
                fontSize: '1.5rem', 
                fontWeight: 700, 
                color: timeLeft < 300 ? 'var(--error)' : 'var(--primary)',
                padding: '8px 16px',
                background: timeLeft < 300 ? 'var(--error-bg)' : 'var(--bg-secondary)',
                borderRadius: 'var(--radius)'
              }}>
                ⏱️ {formatTime(timeLeft)}
              </div>
              <button 
                onClick={handleSubmit} 
                disabled={loading}
                style={{ padding: '8px 16px', fontSize: '1rem' }}
              >
                {loading ? <span className="loading" /> : 'Submit Exam'}
              </button>
            </div>
          </div>

          <div style={{ marginTop: '24px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {questions.map((q, idx) => (
              <div key={q.id} className="card" style={{ padding: '20px', background: 'var(--bg-secondary)' }}>
                <div style={{ marginBottom: '12px' }}>
                  <strong style={{ fontSize: '1.1rem' }}>Q{idx + 1}. {q.questionText}</strong>
                  <span className="chip" style={{ marginLeft: '12px' }}>{q.points} pts</span>
                </div>
                
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginTop: '16px' }}>
                  {q.options.map((option: string, optIdx: number) => (
                    <button
                      key={optIdx}
                      onClick={() => setAnswers(prev => ({ ...prev, [q.id]: optIdx }))}
                      style={{
                        padding: '12px',
                        textAlign: 'left',
                        background: answers[q.id] === optIdx ? 'var(--primary)' : 'var(--bg)',
                        color: answers[q.id] === optIdx ? 'white' : 'inherit',
                        border: answers[q.id] === optIdx ? '2px solid var(--primary)' : '1px solid var(--border)',
                        borderRadius: 'var(--radius)',
                        cursor: 'pointer',
                        fontWeight: answers[q.id] === optIdx ? 600 : 400
                      }}
                    >
                      {String.fromCharCode(65 + optIdx)}. {option}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>Available Exams</h3>
      {fetching ? (
        <div className="empty-state">
          <div className="loading" />
          <p>Loading exams...</p>
        </div>
      ) : exams.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">📚</div>
          <p>No exams available at the moment.</p>
        </div>
      ) : (
        <div className="grid grid-2">
          {exams.map((e) => (
            <div key={e.id} className="card" style={{ padding: '20px' }}>
              <h4>{e.title}</h4>
              <div style={{ marginTop: '8px', fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                <p>📅 Start: {new Date(e.startTime).toLocaleString()}</p>
                <p>⏱️ Duration: {e.durationMinutes} minutes</p>
              </div>
              <button 
                onClick={() => startExam(e)} 
                disabled={loading} 
                style={{ marginTop: '12px', width: '100%' }}
              >
                {loading ? <span className="loading" /> : 'Start Exam'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function OrdersTab({ auth, showToast }: TabProps) {
  const [orders, setOrders] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}` };

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const endpoint = auth.role === 'STUDENT' ? '/orders/my' : '/orders';
      const res = await fetch(`${API_BASE}${endpoint}`, { headers });
      if (res.ok) {
        setOrders(await res.json());
      } else {
        showToast('Failed to load orders', 'error');
      }
    } catch (error) {
      showToast('Error loading orders', 'error');
    } finally {
      setLoading(false);
    }
  }, [auth.token, auth.role]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  return (
    <div>
      <div className="card">
        <h3>{auth.role === 'FACULTY' ? 'All Orders' : 'My Orders'}</h3>
        {loading ? (
          <div className="empty-state">
            <div className="loading" />
            <p>Loading orders...</p>
          </div>
        ) : orders.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📦</div>
            <p>No orders found.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {orders.map((order) => (
              <div key={order.id} className="card" style={{ padding: 20 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 12 }}>
                  <div>
                    <span style={{ fontWeight: 600, fontSize: 16 }}>Order #{order.id}</span>
                    {auth.role === 'FACULTY' && <span className="muted" style={{ marginLeft: 12 }}>by {order.userId}</span>}
                  </div>
                  <span className={`badge ${order.status === 'CONFIRMED' ? 'success' : order.status === 'FAILED' ? 'error' : 'warning'}`}>
                    {order.status}
                  </span>
                </div>
                <div style={{ marginBottom: 12 }}>
                  <p className="muted" style={{ fontSize: 12, marginBottom: 8 }}>
                    Created: {new Date(order.createdAt).toLocaleString()}
                  </p>
                  {order.items && order.items.length > 0 && (
                    <div>
                      <p style={{ fontWeight: 600, fontSize: 14, marginBottom: 8 }}>Items:</p>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        {order.items.map((item: any, idx: number) => (
                          <div key={idx} style={{ padding: '8px 12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius)', fontSize: 13 }}>
                            <span>Ticket ID: {item.ticketId}</span>
                            <span style={{ marginLeft: 16 }}>Quantity: {item.quantity}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function NotificationsTab({ auth, showToast }: TabProps) {
  const [notifications, setNotifications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}` };

  useEffect(() => {
    const fetchNotifications = async () => {
      try {
        const endpoint = auth.role === 'FACULTY' 
          ? `/notifications` 
          : `/notifications/my`;
        const res = await fetch(`${API_BASE}${endpoint}`, { headers });
        if (res.ok) {
          setNotifications(await res.json());
        }
      } catch (error) {
        showToast('Error loading notifications', 'error');
      } finally {
        setLoading(false);
      }
    };
    fetchNotifications();
  }, [auth.token, auth.role]);

  if (loading) {
    return (
      <div className="empty-state">
        <div className="loading" />
        <p>Loading notifications...</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>Notifications</h3>
      {notifications.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🔔</div>
          <p>No notifications yet.</p>
        </div>
      ) : (
        <div className="table">
          <div className="table-header">
            <span>Message</span>
            <span>User</span>
            <span>Time</span>
          </div>
          {notifications.map((n) => (
            <div key={n.id} className="table-row">
              <span style={{ fontWeight: 600 }}>{n.message}</span>
              <span className="muted">{n.user}</span>
              <span className="muted">{new Date(n.createdAt).toLocaleString()}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function IotTab({ auth, showToast }: TabProps) {
  const [temperatures, setTemperatures] = useState<any[]>([]);
  const [shuttle, setShuttle] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const headers = { Authorization: `Bearer ${auth.token}` };

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [tempsRes, shuttleRes] = await Promise.all([
          fetch(`${API_BASE}/iot/iot/temperatures`, { headers }).catch(() => null),
          fetch(`${API_BASE}/iot/iot/shuttle`, { headers }).catch(() => null),
        ]);
        if (tempsRes?.ok) setTemperatures(await tempsRes.json());
        if (shuttleRes?.ok) setShuttle(await shuttleRes.json());
      } catch (error) {
        showToast('Error loading IoT data', 'error');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
    const interval = setInterval(fetchData, 5000);
    return () => clearInterval(interval);
  }, [auth.token]);

  if (loading) {
    return (
      <div className="empty-state">
        <div className="loading" />
        <p>Loading IoT sensors...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="card mb-3">
        <h3>🌡️ Temperature Sensors</h3>
        <p className="muted mb-2">Real-time classroom temperature monitoring (updates every 5 seconds)</p>
        {temperatures.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🌡️</div>
            <p>No sensor data available</p>
          </div>
        ) : (
          <div className="grid grid-4 mt-2">
            {temperatures.slice(-8).map((t, idx) => (
              <div key={idx} className="stat-card">
                <h3>{t.room}</h3>
                <div className="stat-value">{t.value.toFixed(1)}°C</div>
                <div className="stat-label">{new Date(t.timestamp).toLocaleTimeString()}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      {shuttle && (
        <div className="card">
          <h3>🚐 Shuttle Tracking</h3>
          <p className="muted mb-2">Real-time campus shuttle location</p>
          <div style={{ padding: 24, background: 'var(--bg-secondary)', borderRadius: 'var(--radius-lg)' }}>
            <div className="grid grid-3">
              <div>
                <div className="muted text-sm">Latitude</div>
                <div style={{ fontSize: 20, fontWeight: 600, marginTop: 4 }}>{shuttle.lat.toFixed(5)}</div>
              </div>
              <div>
                <div className="muted text-sm">Longitude</div>
                <div style={{ fontSize: 20, fontWeight: 600, marginTop: 4 }}>{shuttle.lng.toFixed(5)}</div>
              </div>
              <div>
                <div className="muted text-sm">Speed</div>
                <div style={{ fontSize: 20, fontWeight: 600, marginTop: 4 }}>{shuttle.speedKph.toFixed(1)} km/h</div>
              </div>
            </div>
            <div className="muted text-sm" style={{ marginTop: 16 }}>
              Last updated: {new Date(shuttle.timestamp).toLocaleString()}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
