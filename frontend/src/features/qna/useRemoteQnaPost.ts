import { useEffect, useMemo, useState, type Dispatch, type SetStateAction } from "react";
import { useLocation } from "react-router-dom";
import { useAuth } from "@/features/auth/AuthContext";
import {
	type BoardAnswerResponse,
	type BoardCommentResponse,
	type BoardPostResponse,
	getBoardAnswer,
	getBoardComments,
	getPremiumBoardPost,
} from "@/features/board/boardApi";
import { ApiError } from "@/lib/api";

export interface QnaPostPermissions {
	isAuthor: boolean;
	isBoardOwner: boolean;
	canEdit: boolean;
	canWriteAnswer: boolean;
	canManageAnswer: boolean;
	canWriteComment: boolean;
}

interface UseRemoteQnaPostResult {
	post: BoardPostResponse | null;
	setPost: Dispatch<SetStateAction<BoardPostResponse | null>>;
	answer: BoardAnswerResponse | null;
	setAnswer: Dispatch<SetStateAction<BoardAnswerResponse | null>>;
	comments: BoardCommentResponse[];
	setComments: Dispatch<SetStateAction<BoardCommentResponse[]>>;
	loading: boolean;
	loadError: string | null;
	imageUploadFailed: boolean;
	permissions: QnaPostPermissions;
}

export function useRemoteQnaPost(postId: number): UseRemoteQnaPostResult {
	const location = useLocation();
	const { user } = useAuth();

	const [post, setPost] = useState<BoardPostResponse | null>(null);
	const [answer, setAnswer] = useState<BoardAnswerResponse | null>(null);
	const [comments, setComments] = useState<BoardCommentResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [loadError, setLoadError] = useState<string | null>(null);

	const imageUploadFailed =
		(location.state as { imageUploadFailed?: boolean } | null)
			?.imageUploadFailed === true;

	useEffect(() => {
		let cancelled = false;
		setLoading(true);
		setLoadError(null);

		Promise.all([
			getPremiumBoardPost(postId),
			getBoardAnswer(postId).catch(() => null),
			getBoardComments(postId).catch(() => [] as BoardCommentResponse[]),
		])
			.then(([data, answerData, commentData]) => {
				if (cancelled) return;
				setPost(data);
				setAnswer(answerData);
				setComments(commentData);
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				setLoadError(
					err instanceof ApiError
						? err.message
						: "게시글을 불러오지 못했습니다.",
				);
				setPost(null);
				setAnswer(null);
				setComments([]);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
		};
	}, [postId]);

	const permissions = useMemo<QnaPostPermissions>(() => {
		const isAuthor = !!user && !!post && user.memberId === post.memberId;
		const isBoardOwner = !!user && !!post && user.memberId === post.creatorId;
		return {
			isAuthor,
			isBoardOwner,
			canEdit: isAuthor && post?.status === "WAITING",
			canWriteAnswer: isBoardOwner && post?.status === "WAITING" && !answer,
			canManageAnswer: isBoardOwner && !!answer,
			canWriteComment: !!user,
		};
	}, [user, post, answer]);

	return {
		post,
		setPost,
		answer,
		setAnswer,
		comments,
		setComments,
		loading,
		loadError,
		imageUploadFailed,
		permissions,
	};
}
