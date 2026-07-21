import { Icon } from "@/components/ui/Icon";
import {
  CREATOR_APPLICATION_STATUS_BG,
  CREATOR_APPLICATION_STATUS_ICON,
  CREATOR_APPLICATION_STATUS_LABEL,
  CREATOR_APPLICATION_STATUS_TEXT_COLOR,
} from "@/features/creator/creatorApplicationStatus";
import type { CreatorApplication } from "@/features/creator/types";

interface CreatorApplicationStatusProps {
  application: CreatorApplication | null;
}

//크리에이터 전환 신청 상태 카드.
export function CreatorApplicationStatus({ application }: CreatorApplicationStatusProps) {
  return (
    <div className="mt-2 min-h-[48px] w-full">
      {application && (
        <div
          className={`w-full rounded-lg px-4 py-3 text-left ${CREATOR_APPLICATION_STATUS_BG[application.status]}`}
        >
          <div className="flex items-center gap-2">
            <Icon
              name={CREATOR_APPLICATION_STATUS_ICON[application.status]}
              className={`text-[18px] ${CREATOR_APPLICATION_STATUS_TEXT_COLOR[application.status]}`}
            />
            <p className="text-body-md text-on-surface">
              크리에이터 전환 신청 상태:{" "}
              <span className={`font-bold ${CREATOR_APPLICATION_STATUS_TEXT_COLOR[application.status]}`}>
                {CREATOR_APPLICATION_STATUS_LABEL[application.status]}
              </span>
            </p>
          </div>
          {application.status === "REJECTED" && application.rejectReason && (
            <p className="mt-1 text-body-md text-secondary">사유: {application.rejectReason}</p>
          )}
        </div>
      )}
    </div>
  );
}