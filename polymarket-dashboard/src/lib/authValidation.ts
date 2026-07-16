export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_REQUIREMENTS =
  `Password must be at least ${PASSWORD_MIN_LENGTH} characters`;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidUsername(username: string): boolean {
  return username.trim().length > 0;
}

export function isValidEmail(email: string): boolean {
  return EMAIL_PATTERN.test(email.trim());
}

export function isValidPassword(password: string): boolean {
  return password.length >= PASSWORD_MIN_LENGTH;
}
