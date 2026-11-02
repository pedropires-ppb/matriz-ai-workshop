// User-facing text, kept in one place. The language is English (Assumption A-028, OQ-013).
// The texts for signing in and for ended sessions are the team's (ADR-009 decision 1).
export const messages = {
  applicationName: 'Matriz',
  loginName: 'Login name',
  password: 'Password',
  signIn: 'Sign in',
  signOut: 'Sign out',
  signedInAs: (displayName: string) => `Signed in as ${displayName}`,
  myProjects: 'My projects',
  incorrectCredentials: 'The login name or password is incorrect.',
  credentialsRequired: 'A login name and a password are required.',
  unreachable: 'Matriz could not be reached. Please try again.',
  signedOut: 'You have signed out.',
  sessionEnded: 'Your session has ended. Please sign in again.',
} as const;
