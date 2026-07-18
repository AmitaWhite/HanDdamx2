/**
 * 백엔드 검증 규칙 미러링 (SignupRequest / PasswordValidator).
 * 서버 왕복 없이 즉시 피드백을 주기 위함이며, 최종 판정은 서버가 한다.
 * 규칙이 바뀌면 백엔드와 함께 갱신할 것.
 */

const NICKNAME_RE = /^[a-zA-Z0-9가-힣]{2,20}$/;
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TRIPLE_RE = /(.)\1{2,}/; // 동일 문자 3회 연속

const PASSWORD_MIN = 8;
const PASSWORD_MAX = 20;

/** 비밀번호 도움말 (입력 힌트로 노출) */
export const PASSWORD_HINT =
	"8~20자, 대문자·소문자·숫자·특수문자 중 3가지 이상 조합 (같은 문자 3번 연속 불가)";

export function validateEmail(email: string): string | null {
	if (!email.trim()) return "이메일을 입력해 주세요.";
	if (!EMAIL_RE.test(email)) return "올바른 이메일 형식이 아닙니다.";
	return null;
}

export function validateNickname(nickname: string): string | null {
	if (!nickname.trim()) return "닉네임을 입력해 주세요.";
	if (!NICKNAME_RE.test(nickname))
		return "닉네임은 2~20자의 한글, 영문, 숫자만 가능합니다.";
	return null;
}

export function validatePassword(password: string): string | null {
	if (!password) return "비밀번호를 입력해 주세요.";
	if (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
		return `비밀번호는 ${PASSWORD_MIN}~${PASSWORD_MAX}자여야 합니다.`;
	}
	if (TRIPLE_RE.test(password)) {
		return "같은 문자를 3번 이상 연속으로 사용할 수 없습니다.";
	}
	const kinds = [/[A-Z]/, /[a-z]/, /[0-9]/, /[^A-Za-z0-9]/].filter((re) =>
		re.test(password),
	).length;
	if (kinds < 3) {
		return "대문자·소문자·숫자·특수문자 중 3가지 이상을 조합해 주세요.";
	}
	return null;
}

export function validatePasswordConfirm(
	password: string,
	confirm: string,
): string | null {
	if (!confirm) return "비밀번호를 한 번 더 입력해 주세요.";
	if (password !== confirm) return "비밀번호가 일치하지 않습니다.";
	return null;
}
