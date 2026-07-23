import { Alert } from "@/components/ui/Alert";

interface QnaPostAlertBannerProps {
	saveError?: string | null;
	deleteError?: string | null;
	imageError?: string | null;
	answerError?: string | null;
	commentError?: string | null;
	imageUploadFailed?: boolean;
}

export function QnaPostAlertBanner({
	saveError,
	deleteError,
	imageError,
	answerError,
	commentError,
	imageUploadFailed,
}: QnaPostAlertBannerProps) {
	const message =
		saveError ??
		deleteError ??
		imageError ??
		answerError ??
		commentError ??
		(imageUploadFailed
			? "글은 등록됐지만 이미지가 저장되지 않았습니다. 아래에서 다시 추가해 주세요."
			: null);

	if (!message) return null;

	return (
		<div className="mb-4">
			<Alert>{message}</Alert>
		</div>
	);
}
