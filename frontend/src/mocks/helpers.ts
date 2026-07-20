/** 목데이터 전용 이미지 헬퍼. 실제 이미지 API 연동 전까지 전 화면이 공유. */
export function mockImg(seed: string, w = 800, h = 600) {
	return `https://picsum.photos/seed/${seed}/${w}/${h}`;
}
