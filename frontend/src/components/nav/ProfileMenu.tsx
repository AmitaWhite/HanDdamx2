import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Icon } from "@/components/ui/Icon";
import { useAuth } from "@/features/auth/AuthContext";

/** 헤더 우측 아바타 클릭 시 열리는 프로필 드롭다운(마이페이지/로그아웃). 시안엔 없어 새로 설계. */
export function ProfileMenu() {
	const { user, logout } = useAuth();
	const navigate = useNavigate();
	const [open, setOpen] = useState(false);
	const rootRef = useRef<HTMLDivElement>(null);

	useEffect(() => {
		function onPointerDown(e: PointerEvent) {
			if (rootRef.current && !rootRef.current.contains(e.target as Node))
				setOpen(false);
		}
		function onKeyDown(e: KeyboardEvent) {
			if (e.key === "Escape") setOpen(false);
		}
		document.addEventListener("pointerdown", onPointerDown);
		document.addEventListener("keydown", onKeyDown);
		return () => {
			document.removeEventListener("pointerdown", onPointerDown);
			document.removeEventListener("keydown", onKeyDown);
		};
	}, []);

	async function handleLogout() {
		setOpen(false);
		await logout();
		navigate(paths.landing, { replace: true });
	}

	return (
		<div ref={rootRef} className="relative ml-1">
			<button
				type="button"
				aria-label="프로필 메뉴"
				aria-expanded={open}
				onClick={() => setOpen((v) => !v)}
			>
				<Avatar
					src={user?.profileImageUrl ?? undefined}
					fallbackText={user?.nickname}
					size={36}
					className="ring-1 ring-outline-variant"
				/>
			</button>

			{open && (
				<div className="absolute right-0 top-full z-50 mt-2 min-w-[180px] rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-2 shadow-card-hover">
					{user?.role === "CREATOR" && (
						<Link
							to={paths.creator(user.memberId)}
							onClick={() => setOpen(false)}
							className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-label-md font-label-md text-on-surface hover:bg-surface-container-low"
						>
							<Icon name="storefront" className="text-[20px] text-secondary" />
							내 홈
						</Link>
					)}
					<Link
						to={paths.mypage}
						onClick={() => setOpen(false)}
						className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-label-md font-label-md text-on-surface hover:bg-surface-container-low"
					>
						<Icon name="person" className="text-[20px] text-secondary" />
						마이페이지
					</Link>
					<div className="my-1 h-px bg-outline-variant/50" />
					<button
						type="button"
						onClick={handleLogout}
						className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-label-md font-label-md text-on-surface hover:bg-surface-container-low"
					>
						<Icon name="logout" className="text-[20px] text-secondary" />
						로그아웃
					</button>
				</div>
			)}
		</div>
	);
}
