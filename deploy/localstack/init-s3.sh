#!/bin/bash
# =========================================================
# LocalStack 준비 완료 시 실행되는 초기화 훅.
# (컨테이너의 /etc/localstack/init/ready.d/ 에 마운트하면 기동 후 자동 실행된다.)
#
# 백엔드 앱은 S3 버킷을 자동 생성하지 않으므로, 여기서 미리 만들어 둔다.
# 버킷 이름은 compose가 넘겨주는 AWS_S3_BUCKET 환경변수를 따른다(기본값 handdam-local).
# =========================================================
set -e

BUCKET="${AWS_S3_BUCKET:-handdam-local}"

echo "[init-s3] ensuring S3 bucket exists: ${BUCKET}"

# 이미 있으면 무시(idempotent).
awslocal s3 mb "s3://${BUCKET}" 2>/dev/null || true

echo "[init-s3] done."
