/** ISO 타임스탬프를 "YYYY.MM.DD" 형태로 변환. */
export function formatDateLabel(iso: string): string {
	const d = new Date(iso);
	return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
}
