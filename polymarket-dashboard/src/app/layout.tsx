import type { Metadata } from 'next';
import { Header } from '@/components/layout/Header';
import { Providers } from './providers';
import './globals.css';

export const metadata: Metadata = {
  title: 'Polymarket Dashboard',
  description: 'Real-time visualization of Polymarket prediction markets',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <Providers>
          <Header />
          <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {children}
          </main>
        </Providers>
      </body>
    </html>
  );
}
