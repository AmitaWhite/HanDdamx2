/** ISO 타임스탬프를 "YYYY.MM.DD" 형태로 변환. */
export function formatDateLabel(iso: string): string {
	const d = new Date(iso);
	return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
}

/**
 * "YYYY-MM-DD"(date input 값)를 그 날짜의 24시(=23:59:59.999, 로컬 타임존 기준) ISO 문자열로 변환.
 * datetime-local의 시간 선택 UI가 브라우저·OS마다 다르게 렌더돼 안 보이는 경우가 있어,
 * 투표 종료일은 날짜만 고르고 항상 그 날 자정에 마감되도록 고정한다.
 */
export function endOfDayToIso(dateStr: string): string {
	const [year, month, day] = dateStr.split("-").map(Number);
	const endOfDay = new Date(year, month - 1, day, 23, 59, 59, 999);
	return endOfDay.toISOString();
}

/** ISO 타임스탬프에서 로컬 타임존 기준 "YYYY-MM-DD" 날짜만 추출 (date input 값 채우기용). */
export function toDateInputValue(iso: string): string {
	const d = new Date(iso);
	const pad = (n: number) => String(n).padStart(2, "0");
	return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
