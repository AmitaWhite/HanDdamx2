import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { useAuth } from "./AuthContext";

/** ADMIN role이 아닌 사용자가 관리자 전용 화면에 접근하면 홈으로 돌려보낸다. */
export function AdminOnlyRoute({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  if (!user) return <Navigate to={paths.login} replace />;
  if (user.role !== "ADMIN") return <Navigate to={paths.home} replace />;
  return <>{children}</>;
}
