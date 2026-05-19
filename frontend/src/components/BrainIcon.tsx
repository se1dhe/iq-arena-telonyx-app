import type { ReactElement, SVGProps } from 'react';
import type { BrainIconName } from '../data/icons';

type BrainIconProps = SVGProps<SVGSVGElement> & {
  name: BrainIconName;
};

const paths: Record<BrainIconName, ReactElement> = {
  arena: (
    <>
      <path d="M8 21h16M12 21l3-10M20 21l-3-10M11 11h10" />
      <path d="M15 7h2M13 25h6" />
    </>
  ),
  atom: (
    <>
      <ellipse cx="16" cy="16" rx="10" ry="4.2" />
      <ellipse cx="16" cy="16" rx="10" ry="4.2" transform="rotate(60 16 16)" />
      <ellipse cx="16" cy="16" rx="10" ry="4.2" transform="rotate(120 16 16)" />
      <circle cx="16" cy="16" r="1.7" />
    </>
  ),
  archive: (
    <>
      <path d="M7 9.5h18v16H7z" />
      <path d="M10 6.5h12v3H10zM11 14h10M11 18h7" />
    </>
  ),
  bolt: <path d="M17 3 7 18h8l-1 11 10-16h-8z" />,
  constellation: (
    <>
      <circle cx="8" cy="11" r="2" />
      <circle cx="19" cy="7" r="2" />
      <circle cx="24" cy="19" r="2" />
      <circle cx="12" cy="24" r="2" />
      <path d="m9.7 10.2 7.6-2.5M20 8.8l3.2 8.3M22.2 20.1 13.8 23M12.8 22.3 8.8 12.9" />
    </>
  ),
  duel: (
    <>
      <path d="m7 23 7-7M10 26l7-7M18 6l8 8" />
      <path d="m25 23-7-7M22 26l-7-7M14 6l-8 8" />
      <path d="M11 9 9 7M21 9l2-2" />
    </>
  ),
  globe: (
    <>
      <circle cx="16" cy="16" r="10" />
      <path d="M6 16h20M16 6c3 3 4.5 6.4 4.5 10S19 23 16 26M16 6c-3 3-4.5 6.4-4.5 10S13 23 16 26" />
    </>
  ),
  logic: (
    <>
      <path d="M8 8h16v16H8z" />
      <path d="M12 12h8M12 16h5M12 20h8" />
      <path d="M24 8 28 4M8 24l-4 4" />
    </>
  ),
  profile: (
    <>
      <circle cx="16" cy="12" r="5" />
      <path d="M7 26c1.8-5 5-7.5 9-7.5S23.2 21 25 26" />
    </>
  ),
  progress: (
    <>
      <path d="M7 24V12M16 24V6M25 24V16" />
      <path d="M5 24h22" />
    </>
  ),
  share: (
    <>
      <circle cx="9" cy="16" r="3" />
      <circle cx="23" cy="8" r="3" />
      <circle cx="23" cy="24" r="3" />
      <path d="m11.8 14.6 8.4-5.2M11.8 17.4l8.4 5.2" />
    </>
  ),
  spark: (
    <>
      <path d="M16 4v8M16 20v8M4 16h8M20 16h8" />
      <path d="m8 8 5 5M19 19l5 5M24 8l-5 5M13 19l-5 5" />
    </>
  ),
  sun: (
    <>
      <circle cx="16" cy="16" r="5" />
      <path d="M16 3v5M16 24v5M3 16h5M24 16h5M6.8 6.8l3.5 3.5M21.7 21.7l3.5 3.5M25.2 6.8l-3.5 3.5M10.3 21.7l-3.5 3.5" />
    </>
  )
};

export function BrainIcon({ name, ...props }: BrainIconProps) {
  return (
    <svg viewBox="0 0 32 32" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...props}>
      {paths[name]}
    </svg>
  );
}
