/** 조건부 className 병합용 초경량 헬퍼 (clsx 대체) */
export function cn(...parts: Array<string | false | null | undefined>): string {
	return parts.filter(Boolean).join(" ");
}
