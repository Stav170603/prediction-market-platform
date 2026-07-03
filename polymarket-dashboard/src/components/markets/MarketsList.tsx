'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { ChevronUp, ChevronDown } from 'lucide-react';
import {
  formatPrice,
  formatVolume,
  getCountdownText,
  getStateColor,
  getStateLabel,
} from '@/lib/utils';
import { Market } from '@/lib/api';

interface MarketsListProps {
  markets: Market[];
  isLoading?: boolean;
}

type SortKey = 'volume' | 'price' | 'category';
type FilterState = 'all' | 'active' | 'closed' | 'resolved';

export function MarketsList({ markets, isLoading = false }: MarketsListProps) {
  const [displayMarkets, setDisplayMarkets] = useState(markets);
  const [sortKey, setSortKey] = useState<SortKey>('volume');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [filterState, setFilterState] = useState<FilterState>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 20;

  useEffect(() => {
    let filtered = [...markets];

    // Apply filter
    if (filterState !== 'all') {
      filtered = filtered.filter((m) => m.state === filterState);
    }

    // Apply search
    if (searchTerm) {
      filtered = filtered.filter(
        (m) =>
          m.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
          m.description?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Apply sort
    filtered.sort((a, b) => {
      let aVal: number | string = 0;
      let bVal: number | string = 0;

      if (sortKey === 'volume') {
        aVal = a.volume24h || 0;
        bVal = b.volume24h || 0;
      } else if (sortKey === 'price') {
        aVal = a.yesPrice;
        bVal = b.yesPrice;
      } else if (sortKey === 'category') {
        aVal = a.category || '';
        bVal = b.category || '';
      }

      if (typeof aVal === 'string') {
        return sortOrder === 'asc'
          ? aVal.localeCompare(bVal as string)
          : (bVal as string).localeCompare(aVal);
      }

      return sortOrder === 'asc' ? (aVal as number) - (bVal as number) : (bVal as number) - (aVal as number);
    });

    setDisplayMarkets(filtered);
    setCurrentPage(1);
  }, [markets, sortKey, sortOrder, filterState, searchTerm]);

  const paginatedMarkets = displayMarkets.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const totalPages = Math.ceil(displayMarkets.length / itemsPerPage);

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortOrder('desc');
    }
  };

  const SortIcon = ({ active }: { active: boolean }) => {
    if (!active) return null;
    return sortOrder === 'asc' ? (
      <ChevronUp className="w-4 h-4" />
    ) : (
      <ChevronDown className="w-4 h-4" />
    );
  };

  if (isLoading) {
    return <div className="text-center py-12">Loading markets...</div>;
  }

  return (
    <div className="space-y-4">
      {/* Controls */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <input
          type="text"
          placeholder="Search markets..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="px-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white placeholder-slate-400"
        />

        <select
          value={filterState}
          onChange={(e) => setFilterState(e.target.value as FilterState)}
          className="px-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white"
        >
          <option value="all">All States</option>
          <option value="active">Active</option>
          <option value="closed">Closed</option>
          <option value="resolved">Resolved</option>
        </select>

        <select
          value={sortKey}
          onChange={(e) => setSortKey(e.target.value as SortKey)}
          className="px-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white"
        >
          <option value="volume">Sort by Volume</option>
          <option value="price">Sort by Price</option>
          <option value="category">Sort by Category</option>
        </select>
      </div>

      {/* Table */}
      <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900">
              <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">
                Title
              </th>
              <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">
                Category
              </th>
              <th className="px-4 py-3 text-center font-semibold text-slate-900 dark:text-white cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800"
                  onClick={() => toggleSort('price')}>
                <div className="flex items-center justify-center gap-1">
                  Yes/No Price
                  <SortIcon active={sortKey === 'price'} />
                </div>
              </th>
              <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800"
                  onClick={() => toggleSort('volume')}>
                <div className="flex items-center justify-end gap-1">
                  24h Volume
                  <SortIcon active={sortKey === 'volume'} />
                </div>
              </th>
              <th className="px-4 py-3 text-center font-semibold text-slate-900 dark:text-white">
                State
              </th>
              <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">
                Countdown
              </th>
              <th className="px-4 py-3 text-center font-semibold text-slate-900 dark:text-white">
                Action
              </th>
            </tr>
          </thead>
          <tbody>
            {paginatedMarkets.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-slate-600 dark:text-slate-400">
                  No markets found
                </td>
              </tr>
            ) : (
              paginatedMarkets.map((market) => (
                <tr
                  key={market.id}
                  className="border-b border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-900 transition-colors"
                >
                  <td className="px-4 py-3">
                    <div className="font-medium text-slate-900 dark:text-white truncate">
                      {market.title}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-400">
                    {market.category || 'N/A'}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <div className="flex items-center justify-center gap-2">
                      <span className="text-green-600 font-medium">
                        {formatPrice(market.yesPrice)}
                      </span>
                      <span className="text-slate-400">/</span>
                      <span className="text-red-600 font-medium">
                        {formatPrice(market.noPrice)}
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-right text-slate-600 dark:text-slate-400">
                    {market.volume24h ? formatVolume(market.volume24h) : 'N/A'}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${getStateColor(market.state)}`}>
                      {getStateLabel(market.state)}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right text-slate-600 dark:text-slate-400 text-xs">
                    {getCountdownText(market.endDate)}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <Link
                      href={`/market/${market.id}`}
                      className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
                    >
                      View
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-slate-600 dark:text-slate-400">
            Showing {(currentPage - 1) * itemsPerPage + 1} to{' '}
            {Math.min(currentPage * itemsPerPage, displayMarkets.length)} of{' '}
            {displayMarkets.length}
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-900 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Previous
            </button>
            <div className="flex items-center gap-2">
              {Array.from({ length: Math.min(5, totalPages) }).map((_, i) => {
                let page = i + 1;
                if (totalPages > 5 && currentPage > 3) {
                  page = currentPage - 2 + i;
                }
                if (page > totalPages) return null;
                return (
                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className={`px-3 py-2 rounded-lg transition-colors ${
                      currentPage === page
                        ? 'bg-blue-600 text-white'
                        : 'border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-900'
                    }`}
                  >
                    {page}
                  </button>
                );
              })}
            </div>
            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-900 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
