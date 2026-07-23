/** ISO 타임스탬프를 "방금/n분 전/n시간 전/n일 전" 형태로 변환. */
export function formatRelativeTime(iso: string): string {
	const diffMs = Date.now() - new Date(iso).getTime();
	if (diffMs < 0 || diffMs < 60_000) return "방금";

	const minutes = Math.floor(diffMs / 60_000);
	if (minutes < 60) return `${minutes}분 전`;

	const hours = Math.floor(minutes / 60);
	if (hours < 24) return `${hours}시간 전`;

	const days = Math.floor(hours / 24);
	if (days < 7) return `${days}일 전`;

	const weeks = Math.floor(days / 7);
	if (weeks < 5) return `${weeks}주 전`;

	const months = Math.floor(days / 30);
	if (months < 12) return `${months}개월 전`;

	return `${Math.floor(days / 365)}년 전`;
}
