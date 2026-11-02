import { type FormEvent, useState } from 'react';
import { type Session, signIn } from './api';
import { messages } from './messages';

/** Why the sign-in screen is shown again: the person signed out, or the session ended. */
export type Notice = 'signedOut' | 'sessionEnded';

interface Props {
  notice: Notice | null;
  onSignedIn: (session: Session) => void;
}

/** The sign-in screen (US-013, EX-014; ADR-009 decision 1). */
export default function SignInScreen({ notice, onSignedIn }: Props) {
  const [loginName, setLoginName] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [noticeShown, setNoticeShown] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNoticeShown(false);
    if (loginName === '' || password === '') {
      setError(messages.credentialsRequired);
      return;
    }
    setSubmitting(true);
    setError(null);
    const result = await signIn(loginName, password);
    if (result.outcome === 'signedIn') {
      onSignedIn(result.session);
      return;
    }
    setSubmitting(false);
    if (result.outcome === 'incorrect') {
      setPassword('');
      setError(messages.incorrectCredentials);
    } else if (result.outcome === 'required') {
      setError(messages.credentialsRequired);
    } else {
      setError(messages.unreachable);
    }
  }

  return (
    <section aria-labelledby="sign-in-heading">
      <h2 id="sign-in-heading">{messages.signIn}</h2>
      {notice !== null && noticeShown && (
        <p role="status">{notice === 'signedOut' ? messages.signedOut : messages.sessionEnded}</p>
      )}
      <form noValidate onSubmit={submit}>
        <p>
          <label htmlFor="login-name">{messages.loginName}</label>
          <input
            id="login-name"
            name="loginName"
            autoComplete="username"
            autoFocus
            value={loginName}
            onChange={(event) => setLoginName(event.target.value)}
          />
        </p>
        <p>
          <label htmlFor="password">{messages.password}</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </p>
        {error !== null && <p role="alert">{error}</p>}
        <button type="submit" disabled={submitting}>
          {messages.signIn}
        </button>
      </form>
    </section>
  );
}
