'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Eye, EyeOff, UserPlus } from 'lucide-react';
import { toast } from 'sonner';
import { register } from '@/services/authService';
import {
  isValidEmail,
  isValidPassword,
  isValidUsername,
  PASSWORD_MIN_LENGTH,
  PASSWORD_REQUIREMENTS,
} from '@/lib/authValidation';

export default function RegisterPage() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [touched, setTouched] = useState({
    username: false,
    email: false,
    password: false,
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const usernameValid = isValidUsername(username);
  const emailValid = isValidEmail(email);
  const passwordValid = isValidPassword(password);
  const formValid = usernameValid && emailValid && passwordValid;

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitted(true);

    if (!formValid) {
      return;
    }

    setIsSubmitting(true);

    try {
      await register({
        username: username.trim(),
        email: email.trim(),
        password,
      });
      toast.success('Registration successful.');
      router.push('/markets');
    } catch {
      // API failures are surfaced by the shared HTTP interceptor.
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto">
      <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6 shadow-sm">
        <div className="flex items-center gap-3 mb-6">
          <div className="p-2 rounded-lg bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-200">
            <UserPlus className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Register</h1>
            <p className="text-sm text-slate-600 dark:text-slate-400">Create a dashboard account</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
              Username
            </label>
            <input
              type="text"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              onBlur={() => setTouched((fields) => ({ ...fields, username: true }))}
              aria-invalid={(touched.username || submitted) && !usernameValid}
              aria-describedby={(touched.username || submitted) && !usernameValid ? 'username-error' : undefined}
              className="w-full px-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 text-slate-900 dark:text-white placeholder-slate-400"
              required
            />
            {(touched.username || submitted) && !usernameValid && (
              <p id="username-error" className="mt-1 text-sm text-red-600 dark:text-red-400">
                Username is required
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              onBlur={() => setTouched((fields) => ({ ...fields, email: true }))}
              aria-invalid={!emailValid && (email.length > 0 || touched.email || submitted)}
              aria-describedby={!emailValid && (email.length > 0 || touched.email || submitted) ? 'email-error' : undefined}
              className="w-full px-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 text-slate-900 dark:text-white placeholder-slate-400"
              required
            />
            {!emailValid && (email.length > 0 || touched.email || submitted) && (
              <p id="email-error" className="mt-1 text-sm text-red-600 dark:text-red-400">
                Please enter a valid email address
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
              Password
            </label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                onBlur={() => setTouched((fields) => ({ ...fields, password: true }))}
                minLength={PASSWORD_MIN_LENGTH}
                aria-invalid={!passwordValid && (password.length > 0 || touched.password || submitted)}
                aria-describedby={
                  !passwordValid && (password.length > 0 || touched.password || submitted)
                    ? 'password-requirements'
                    : undefined
                }
                className="w-full px-4 py-2 pr-11 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 text-slate-900 dark:text-white placeholder-slate-400"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword((visible) => !visible)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                aria-pressed={showPassword}
                className="absolute inset-y-0 right-0 flex items-center px-3 text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
              >
                {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
            {!passwordValid && (password.length > 0 || touched.password || submitted) && (
              <p id="password-requirements" className="mt-1 text-sm text-red-600 dark:text-red-400">
                {PASSWORD_REQUIREMENTS}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting || !formValid}
            className="w-full px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:opacity-60 disabled:cursor-not-allowed text-white font-medium transition-colors"
          >
            {isSubmitting ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <p className="mt-4 text-sm text-slate-600 dark:text-slate-400 text-center">
          Already have an account?{' '}
          <Link href="/login" className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium">
            Login
          </Link>
        </p>
      </div>
    </div>
  );
}
