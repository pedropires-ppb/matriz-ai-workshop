import { type FormEvent, useState } from 'react';
import { signIn } from './api';
import { Button, Field, Notice } from './components';
import type { Session } from './http';
import { messages } from './messages';
import { useSubmit } from './session';

/** Why the sign-in screen is shown again: the person signed out, or the session ended. */
export type SignInNotice = 'signedOut' | 'sessionEnded';

interface Props {
  notice: SignInNotice | null;
  onSignedIn: (session: Session) => void;
}

/** The sign-in screen (US-013, EX-014; ADR-009 decision 1). */
export default function SignInScreen({ notice, onSignedIn }: Props) {
  const [loginName, setLoginName] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [noticeShown, setNoticeShown] = useState(true);
  const { busy, submit } = useSubmit();

  function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void submit(async () => {
      setNoticeShown(false);
      if (loginName === '' || password === '') {
        setError(messages.credentialsRequired);
        return;
      }
      setError(null);
      const result = await signIn(loginName, password);
      if (result.outcome === 'signedIn') {
        onSignedIn(result.session);
      } else if (result.outcome === 'incorrect') {
        setPassword('');
        setError(messages.incorrectCredentials);
      } else if (result.outcome === 'required') {
        setError(messages.credentialsRequired);
      } else {
        setError(messages.unreachable);
      }
    });
  }

  return (
    <section aria-labelledby="sign-in-heading" className="screen narrow">
      <h2 id="sign-in-heading">{messages.signIn}</h2>
      {notice !== null && noticeShown && (
        <Notice kind="success">{notice === 'signedOut' ? messages.signedOut : messages.sessionEnded}</Notice>
      )}
      <form noValidate onSubmit={send} className="form panel">
        <Field
          id="login-name"
          name="loginName"
          label={messages.loginName}
          autoComplete="username"
          autoFocus
          value={loginName}
          onChange={setLoginName}
        />
        <Field
          id="password"
          name="password"
          type="password"
          label={messages.password}
          autoComplete="current-password"
          value={password}
          onChange={setPassword}
        />
        {error !== null && <Notice kind="error">{error}</Notice>}
        <div className="actions">
          <Button type="submit" variant="primary" busy={busy}>
            {messages.signIn}
          </Button>
        </div>
      </form>
    </section>
  );
}
