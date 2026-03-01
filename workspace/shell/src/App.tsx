import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Alignment, Button, Classes, Navbar, Spinner, H2, Icon } from '@blueprintjs/core';
import '@blueprintjs/core/lib/css/blueprint.css';
import '@blueprintjs/icons/lib/css/blueprint-icons.css';

// @ts-ignore - The module federation remote entry will provide this
const RemoteCodeApp = React.lazy(() => import('codeRemote/CodeApp'));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
    mutations: {
      onError: (error: any) => {
        if (error.message.includes('401') || error.status === 401) {
          localStorage.removeItem('token');
          window.location.href = '/';
        }
      }
    }
  }
});

function ShellApp() {
  const [token, setToken] = React.useState(localStorage.getItem('token') || '');
  const [username, setUsername] = React.useState('');
  const [password, setPassword] = React.useState('');
  const [loading, setLoading] = React.useState(false);
  const [loginError, setLoginError] = React.useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setLoginError('');
    try {
      const res = await fetch('http://localhost:8080/realms/nebula/protocol/openid-connect/token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          client_id: 'workspace',
          grant_type: 'password',
          username,
          password
        })
      });
      if (!res.ok) throw new Error('Invalid credentials');
      const data = await res.json();
      localStorage.setItem('token', data.access_token);
      setToken(data.access_token);
    } catch (err: any) {
      setLoginError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    setToken('');
  };

  if (!token) {
    return (
      <div style={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center', backgroundColor: '#f5f8fa' }}>
        <div style={{ padding: 40, background: 'white', borderRadius: 8, boxShadow: '0 4px 6px rgba(0,0,0,0.1)', width: 400 }}>
          <H2 style={{ textAlign: 'center', marginBottom: 20 }}>Welcome to Nebula</H2>
          <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: 15 }}>
            <div className={Classes.INPUT_GROUP}>
              <Icon icon="user" style={{ margin: '7px 7px' }} />
              <input type="text" className={Classes.INPUT} placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} required />
            </div>
            <div className={Classes.INPUT_GROUP}>
              <Icon icon="lock" style={{ margin: '7px 7px' }} />
              <input type="password" className={Classes.INPUT} placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} required />
            </div>
            {loginError && <div style={{ color: 'red', fontSize: 14 }}>{loginError}</div>}
            <Button type="submit" intent="primary" large loading={loading} fill>Log In</Button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100%', backgroundColor: '#f5f8fa' }}>

          <Navbar className={Classes.DARK}>
            <Navbar.Group align={Alignment.LEFT}>
              <Navbar.Heading>Nebula Workspace</Navbar.Heading>
              <Navbar.Divider />
              <Link to="/"><Button className={Classes.MINIMAL} icon="home" text="Home" /></Link>
              <Link to="/repos"><Button className={Classes.MINIMAL} icon="git-repo" text="Code" /></Link>
              <Button className={Classes.MINIMAL} icon="database" text="Datasets" disabled />
              <Button className={Classes.MINIMAL} icon="build" text="Builds" disabled />
            </Navbar.Group>

            <Navbar.Group align={Alignment.RIGHT}>
              <Button className={Classes.MINIMAL} icon="log-out" text="Log Out" onClick={handleLogout} />
            </Navbar.Group>
          </Navbar>

          <div style={{ flex: 1, overflow: 'auto' }}>
            <Suspense fallback={<div style={{ padding: 50, textAlign: 'center' }}><Spinner size={50} /></div>}>
              <Routes>
                <Route path="/" element={
                  <div style={{ padding: 40, textAlign: 'center' }}>
                    <H2>Welcome to Nebula Foundry</H2>
                    <p className="bp5-text-large bp5-text-muted">Distributed Data Analytics Platform</p>
                    <div style={{ marginTop: 20 }}>
                      <Link to="/repos">
                        <Button intent="primary" large icon="git-repo">Open Code Workspace</Button>
                      </Link>
                    </div>
                  </div>
                } />

                {/* Mount the Code MFE here */}
                {/* 
                  Since CodeApp manages its own nested sub-routes (e.g. /repos and /repos/:id),
                  we mount it with a wildcard route `/*` or specifically for `/repos/*`
                */}
                <Route path="/repos/*" element={<RemoteCodeApp />} />
              </Routes>
            </Suspense>
          </div>
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default ShellApp;
