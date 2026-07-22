import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { getMyCreatorProfile, updateCreatorProfile, updateSubscriptionPrice, } from "@/features/creator/creatorApi";
import type { CreatorProfile } from "@/features/creator/types";
import { cn } from "@/lib/cn";

const BENEFITS_MAX_LENGTH = 300;

interface CreatorPriceModalProps {
	open: boolean;
	creator: CreatorProfile;
	onClose: () => void;
	onSaved: (updated: CreatorProfile) => void;
}

// 구독료·유료 구독 혜택 설명 수정 모달
export function CreatorPriceModal({ open, creator, onClose, onSaved }: CreatorPriceModalProps) {
	const [priceInput, setPriceInput] = useState("");
	const [benefitsInput, setBenefitsInput] = useState("");
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [saved, setSaved] = useState(false);

	useEffect(() => {
		if (!open) return;
		setPriceInput(String(creator.subscriptionPrice));
		setBenefitsInput(creator.benefitsDescription ?? "");
		setError(null);
		setSaved(false);
	}, [open]);

	useEffect(() => {
		if (!open) return;

		function onKeyDown(e: KeyboardEvent) {
			if (e.key === "Escape" && !saving) onClose();
		}

		window.addEventListener("keydown", onKeyDown);
		return () => window.removeEventListener("keydown", onKeyDown);
	}, [open, saving, onClose]);

	if (!open) return null;

	// 부분 성공 여부와 무관하게 서버 최신 상태로 화면을 맞춤
	async function syncFromServer() {
		try {
			onSaved(await getMyCreatorProfile());
		} catch {
			/* noop */
		}
	}

	async function handleSave() {
		if (priceInput.trim() === "") {
			setError("구독료를 입력해주세요.");
			return;
		}
		const price = Number(priceInput);
		if (!Number.isInteger(price) || price < 0) {
			setError("0 이상의 정수를 입력해주세요.");
			return;
		}

		const trimmedBenefits = benefitsInput.trim() || null;
		const priceChanged = price !== creator.subscriptionPrice;
		const benefitsChanged = trimmedBenefits !== (creator.benefitsDescription?.trim() || null);

		if (!priceChanged && !benefitsChanged) {
			onClose();
			return;
		}

		setError(null);
		setSaving(true);
		try {
			if (priceChanged) await updateSubscriptionPrice(price);
			if (benefitsChanged) await updateCreatorProfile(creator.introduction, trimmedBenefits);
			await syncFromServer();
			setSaved(true);
		} catch (err) {
			setError(err instanceof Error ? err.message : "구독료 변경에 실패했습니다.");
			await syncFromServer();
		} finally {
			setSaving(false);
		}
	}

	return (
		<div
			className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
			onClick={() => {
				if (!saving) onClose();
			}}
		>
			<Card className="w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
				<h2 className="mb-4 text-headline-sm font-display text-on-surface">유료 구독 관리</h2>

				<label htmlFor="creator-subscription-price" className="mb-1.5 block text-label-md font-label-md text-on-surface">
					월 구독료
				</label>
				<div
					className={cn(
						"flex h-11 items-center rounded border bg-surface-container-lowest pl-4 pr-3 focus-within:ring-1",
						error
							? "border-error focus-within:border-error focus-within:ring-error"
							: "border-outline-variant focus-within:border-on-surface focus-within:ring-on-surface",
					)}
				>
					<input
						id="creator-subscription-price"
						type="number"
						min={0}
						value={priceInput}
						disabled={saving}
						onChange={(e) => {
							setPriceInput(e.target.value);
							setSaved(false);
						}}
						className="w-full bg-transparent text-body-md text-on-surface focus:outline-none"
					/>
					<span className="ml-2 shrink-0 text-body-md text-secondary">원</span>
				</div>
				{error && <p className="mt-1.5 text-caption font-caption text-error">{error}</p>}

				<div className="mb-1.5 mt-4 flex items-center justify-between">
					<label htmlFor="creator-benefits-description" className="text-label-md font-label-md text-on-surface">
						유료 구독 혜택 설명
					</label>
					<span className="text-caption font-caption text-secondary">
						{benefitsInput.length}/{BENEFITS_MAX_LENGTH}
					</span>
				</div>
				<textarea
					id="creator-benefits-description"
					rows={4}
					maxLength={BENEFITS_MAX_LENGTH}
					placeholder="유료 구독자에게 제공하는 혜택을 소개해주세요"
					value={benefitsInput}
					disabled={saving}
					onChange={(e) => {
						setBenefitsInput(e.target.value);
						setSaved(false);
					}}
					className="w-full rounded border border-outline-variant bg-surface-container-lowest p-3 text-body-md text-on-surface placeholder:text-outline focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
				/>

				{saved && !error && (
					<p role="status" className="mt-3 rounded bg-primary/10 px-4 py-3 text-label-md font-label-md text-primary">
						저장되었습니다.
					</p>
				)}

				<div className="mt-4 flex gap-2">
					<Button variant="primary" size="sm" onClick={() => void handleSave()} disabled={saving}>
						{saving ? "저장 중..." : "저장"}
					</Button>
					<Button variant="secondary" size="sm" onClick={onClose} disabled={saving}>
						{saved ? "닫기" : "취소"}
					</Button>
				</div>
			</Card>
		</div>
	);
}