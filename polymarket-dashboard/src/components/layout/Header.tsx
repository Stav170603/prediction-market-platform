'use client';

import Link from 'next/link';
import { Briefcase, LogOut, Medal, Shield, TrendingUp, User, Wallet } from 'lucide-react';
import { ThemeToggle } from './ThemeToggle';
import { useAuth } from '@/contexts/AuthContext';

export function Header() {
  const { currentUser, isAuthInitialized, logout } = useAuth();

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link href="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
            <TrendingUp className="w-6 h-6 text-blue-600" />
            <span className="text-xl font-bold text-slate-900 dark:text-white">
              Polymarket Dashboard
            </span>
          </Link>
          
          <nav className="hidden md:flex items-center gap-8">
            <Link href="/" className="text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
              Overview
            </Link>
            <Link href="/markets" className="text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
              Markets
            </Link>
            {!isAuthInitialized ? null : currentUser ? (
              <>
                <Link href="/portfolio" className="flex items-center gap-1 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
                  <Briefcase className="w-4 h-4" />
                  Portfolio
                </Link>
                <Link href="/wallet" className="flex items-center gap-1 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
                  <Wallet className="w-4 h-4" />
                  Wallet
                </Link>
                <Link href="/profile" className="flex items-center gap-1 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
                  <User className="w-4 h-4" />
                  Profile
                </Link>
                <Link href="/leaderboard" className="flex items-center gap-1 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
                  <Medal className="w-4 h-4" />
                  Leaderboard
                </Link>
                <Link
                  href="/trade-history"
                  className="text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors"
                >
                  Trade History
                </Link>
                {currentUser.role === 'ADMIN' && (
                  <Link href="/admin" className="flex items-center gap-1 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
                    <Shield className="w-4 h-4" />
                    Admin
                  </Link>
                )}
                <button
                  type="button"
                  onClick={handleLogout}
                  className="flex items-center gap-1 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link href="/login" className="text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
                  Login
                </Link>
                <Link href="/register" className="text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white transition-colors">
                  Register
                </Link>
              </>
            )}
          </nav>
          
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
