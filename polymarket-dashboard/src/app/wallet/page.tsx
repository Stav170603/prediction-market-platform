'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { CreditCard, Receipt } from 'lucide-react';
import { getMyWallet, getMyWalletTransactions } from '@/services/walletService';
import { useAuth } from '@/contexts/AuthContext';
import { LoadingSpinner, ErrorBoundary } from '@/components/ui/Loading';

function toNumber(value: number | string | null | undefined): number {
  if (value === null || value === undefined) return 0;
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatMoney(value: number | string): string {
  return `${toNumber(value).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })} pts`;
}

export default function WalletPage() {
  const router = useRouter();
  const { currentUser, isAuthInitialized } = useAuth();

  useEffect(() => {
    if (isAuthInitialized && !currentUser) {
      router.replace('/login');
    }
  }, [currentUser, isAuthInitialized, router]);

  const walletQuery = useQuery({
    queryKey: ['wallet', currentUser?.userId],
    queryFn: getMyWallet,
    enabled: isAuthInitialized && Boolean(currentUser?.userId),
    retry: false,
  });

  const transactionsQuery = useQuery({
    queryKey: ['wallet-transactions', currentUser?.userId],
    queryFn: getMyWalletTransactions,
    enabled: isAuthInitialized && Boolean(currentUser?.userId),
    retry: false,
  });

  if (!isAuthInitialized || !currentUser) return <LoadingSpinner />;

  if (walletQuery.error || transactionsQuery.error) {
    return <ErrorBoundary error={(walletQuery.error || transactionsQuery.error) as Error} />;
  }

  const transactions = transactionsQuery.data ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold text-slate-900 dark:text-white">Wallet</h1>
        <p className="mt-2 text-slate-600 dark:text-slate-400">
          Balance and transaction history for {currentUser.username}
        </p>
      </div>

      {walletQuery.isLoading ? (
        <LoadingSpinner />
      ) : (
        <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-200">
              <CreditCard className="w-5 h-5" />
            </div>
            <div>
              <p className="text-sm text-slate-600 dark:text-slate-400">Available balance</p>
              <p className="text-3xl font-bold text-slate-900 dark:text-white">
                {formatMoney(walletQuery.data?.balance ?? currentUser.walletBalance)}
              </p>
            </div>
          </div>
        </div>
      )}

      <div>
        <div className="flex items-center gap-2 mb-4">
          <Receipt className="w-5 h-5 text-slate-500" />
          <h2 className="text-2xl font-bold text-slate-900 dark:text-white">Transactions</h2>
        </div>

        {transactionsQuery.isLoading ? (
          <LoadingSpinner />
        ) : transactions.length === 0 ? (
          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6 text-slate-600 dark:text-slate-400">
            No wallet transactions yet.
          </div>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900">
                  <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">Type</th>
                  <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">Description</th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Amount</th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Balance</th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Date</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((transaction) => (
                  <tr key={transaction.transactionId} className="border-b border-slate-200 dark:border-slate-700">
                    <td className="px-4 py-3 font-medium text-slate-900 dark:text-white">{transaction.type}</td>
                    <td className="px-4 py-3 text-slate-600 dark:text-slate-400">
                      {transaction.description || 'Transaction'}
                    </td>
                    <td className="px-4 py-3 text-right font-medium">{formatMoney(transaction.amount)}</td>
                    <td className="px-4 py-3 text-right">{formatMoney(transaction.balanceAfter)}</td>
                    <td className="px-4 py-3 text-right text-xs text-slate-600 dark:text-slate-400">
                      {new Date(transaction.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
