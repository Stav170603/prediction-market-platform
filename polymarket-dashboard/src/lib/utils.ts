// Utility functions
import { type ClassValue, clsx } from 'clsx';

export function cn(...inputs: ClassValue[]) {
  return clsx(inputs);
}

export function formatPrice(price: number): string {
  return `${price.toFixed(4)} pts`;
}

export function formatVolume(volume: number): string {
  if (volume >= 1000000) {
    return `${(volume / 1000000).toFixed(1)}M pts`;
  }
  if (volume >= 1000) {
    return `${(volume / 1000).toFixed(1)}K pts`;
  }
  return `${volume.toFixed(0)} pts`;
}

export function formatLiquidity(liquidity: number): string {
  if (liquidity >= 1000000) {
    return `${(liquidity / 1000000).toFixed(1)}M pts`;
  }
  if (liquidity >= 1000) {
    return `${(liquidity / 1000).toFixed(1)}K pts`;
  }
  return `${liquidity.toFixed(0)} pts`;
}

export function getCountdownText(endDate: string | undefined): string {
  if (!endDate) return 'Unknown';
  
  const end = new Date(endDate);
  const now = new Date();
  const diff = end.getTime() - now.getTime();
  
  if (diff < 0) return 'Ended';
  
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  
  if (days > 0) return `${days}d ${hours}h`;
  return `${hours}h`;
}

export function getStateColor(state: string): string {
  switch (state) {
    case 'active':
      return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
    case 'closed':
      return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200';
    case 'resolved':
      return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
    case 'cancelled':
      return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
    default:
      return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200';
  }
}

export function getStateLabel(state: string): string {
  return state.charAt(0).toUpperCase() + state.slice(1);
}
