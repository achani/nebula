import React from 'react';
import { BrowserRouter, Routes, Route, useNavigate, useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Button, Card, Elevation, InputGroup, Spinner, H3, NonIdealState, Callout, Intent } from '@blueprintjs/core';
import '@blueprintjs/core/lib/css/blueprint.css';

// Configure a local router if running in standalone mode, otherwise it will use the Shell's router
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      meta: {
        onError: (error: any) => {
          if (error.message.includes('401')) {
            localStorage.removeItem('token');
            window.location.href = '/';
          }
        }
      }
    }
  }
});

const fetchRepos = async (projectId: string) => {
  const token = localStorage.getItem('token') || '';
  const res = await fetch(`/api/repos?projectId=${projectId}`, { headers: { 'Authorization': `Bearer ${token}` } });
  if (!res.ok) throw new Error('Failed to load repos');
  return res.json();
};

const createRepo = async (repo: { projectId: string, name: string }) => {
  const token = localStorage.getItem('token') || '';
  const res = await fetch('/api/repos', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(repo)
  });
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(errorData.message || 'Failed to create repo');
  }
  return res.json();
};

const launchIde = async (repoId: string) => {
  const token = localStorage.getItem('token') || '';
  const res = await fetch(`/api/repos/${repoId}/ide/launch`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(errorData.message || `Failed to launch IDE (${res.status})`);
  }
  return res.json();
};

export const RepositoryBrowser: React.FC = () => {
  // For now we hardcode a proxy project ID or assume it exists
  const [projectId] = React.useState('00000000-0000-0000-0000-000000000001');
  const [newRepoName, setNewRepoName] = React.useState('');
  const [uiError, setUiError] = React.useState<string | null>(null);
  const navigate = useNavigate();

  const { data: repos, isLoading, error, refetch } = useQuery({
    queryKey: ['repos', projectId],
    queryFn: () => fetchRepos(projectId),
  });

  const createMutation = useMutation({
    mutationFn: createRepo,
    onSuccess: () => {
      setNewRepoName('');
      setUiError(null);
      refetch();
    },
    onError: (err: Error) => {
      console.error('Create Repo Error:', err);
      setUiError(err.message);
    }
  });

  const handleCreate = () => {
    if (!newRepoName) {
      setUiError('Please enter a repository name');
      return;
    }
    setUiError(null);
    createMutation.mutate({ projectId, name: newRepoName });
  };

  if (isLoading) return <Spinner size={50} />;

  const displayError = uiError || (error as Error)?.message;

  return (
    <div style={{ padding: '20px' }}>
      {displayError && (
        <Callout intent={Intent.DANGER} title="Error" style={{ marginBottom: 20 }}>
          {displayError}
        </Callout>
      )}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <H3>Repositories</H3>
        <div style={{ display: 'flex', gap: 10 }}>
          <InputGroup placeholder="New repository name..." value={newRepoName} onChange={e => setNewRepoName(e.target.value)} />
          <Button intent="primary" icon="plus" text="Create Repo" onClick={handleCreate} loading={createMutation.isPending} />
        </div>
      </div>

      {repos?.length === 0 ? (
        <NonIdealState icon="git-repo" title="No repositories" description="Create one to get started" />
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '20px' }}>
          {repos?.map((repo: any) => (
            <Card key={repo.id} interactive={true} elevation={Elevation.ONE} onClick={() => navigate(repo.id)}>
              <h4 className="bp5-heading"><Link to={repo.id}>{repo.name}</Link></h4>
              <p>{repo.description || "No description provided."}</p>
              <p className="bp5-text-muted">Branch: {repo.defaultBranch}</p>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};

export const IdeLauncher: React.FC = () => {
  const { id } = useParams();
  const [showIde, setShowIde] = React.useState(false);
  const [ideUrl, setIdeUrl] = React.useState('');
  const [uiError, setUiError] = React.useState<string | null>(null);

  const launchMutation = useMutation({
    mutationFn: () => launchIde(id!),
    onSuccess: (data) => {
      setIdeUrl(data.proxyUrl);
      setShowIde(true);
      setUiError(null);
    },
    onError: (err: Error) => {
      setUiError(err.message);
    }
  });

  return (
    <div style={{ padding: 20, height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {uiError && (
        <Callout intent={Intent.DANGER} title="Launch Failed" style={{ marginBottom: 20 }}>
          {uiError}
        </Callout>
      )}
      <div style={{ marginBottom: 20 }}>
        <H3>Code Server IDE Runtime</H3>
        {!showIde ? (
          <Button
            intent={Intent.PRIMARY}
            icon="code"
            text="Launch IDE Container"
            onClick={() => launchMutation.mutate()}
            loading={launchMutation.isPending}
            large={true}
          />
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: 15 }}>
            <Button intent={Intent.DANGER} icon="stop" text="Stop Session" onClick={() => setShowIde(false)} />
            <Callout intent={Intent.SUCCESS} icon="info-sign" title="IDE Ready" style={{ flex: 1 }}>
              Use password <strong>nebula</strong> to log in to the code-server.
            </Callout>
          </div>
        )}
      </div>

      {showIde && ideUrl && (
        <div style={{ flex: 1, border: '1px solid #ccc' }}>
          {/* We embed the IDE in an iframe so the user doesn't leave the Workspace */}
          <iframe src={ideUrl} width="100%" height="100%" frameBorder="0" title="IDE"></iframe>
        </div>
      )}
    </div>
  );
};

// The default export for the MFE module
export default function CodeApp({ standalone = false }) {
  const AppContent = (
    <QueryClientProvider client={queryClient}>
      <Routes>
        <Route path="/" element={<RepositoryBrowser />} />
        <Route path=":id" element={<IdeLauncher />} />
      </Routes>
    </QueryClientProvider>
  );

  // If standalone, we provide our own router, else assume host provides one
  if (standalone) {
    return <BrowserRouter>{AppContent}</BrowserRouter>;
  }

  return AppContent;
}
