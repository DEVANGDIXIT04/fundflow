#!/usr/bin/env bash
# Deploys a FundFlow image from ECR onto the EC2 host over SSH.
#
# Usage:   ./scripts/deploy.sh [image-tag]     (default: latest)
#
# Required environment (locally or injected by Jenkins):
#   AWS_ACCOUNT_ID   12-digit account id
#   AWS_REGION       e.g. ap-south-1
#   EC2_HOST         public DNS or IP of the EC2 instance
#   SSH_KEY_PATH     path to the EC2 key pair .pem
#   DB_URL           jdbc:postgresql://<rds-endpoint>:5432/fundflow
#   DB_USERNAME      RDS master user
#   DB_PASSWORD      RDS master password
set -euo pipefail

TAG="${1:-latest}"
: "${AWS_ACCOUNT_ID:?set AWS_ACCOUNT_ID}"
: "${AWS_REGION:?set AWS_REGION}"
: "${EC2_HOST:?set EC2_HOST}"
: "${SSH_KEY_PATH:?set SSH_KEY_PATH}"
: "${DB_URL:?set DB_URL}"
: "${DB_USERNAME:?set DB_USERNAME}"
: "${DB_PASSWORD:?set DB_PASSWORD}"

ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE="${ECR_REGISTRY}/fundflow:${TAG}"

echo "Deploying ${IMAGE} to ${EC2_HOST}"

# The heredoc is unquoted on purpose: ${IMAGE}, ${DB_*} etc. are expanded
# HERE and travel to the instance inside the script body.
ssh -i "${SSH_KEY_PATH}" -o StrictHostKeyChecking=accept-new "ec2-user@${EC2_HOST}" bash -s <<EOF
set -euo pipefail

# The instance role (or instance-profile credentials) authorizes the pull.
aws ecr get-login-password --region ${AWS_REGION} \
    | docker login --username AWS --password-stdin ${ECR_REGISTRY}

docker pull ${IMAGE}

docker rm -f fundflow 2>/dev/null || true

# DDL_AUTO=update creates the schema on first boot against an empty RDS and
# evolves it afterwards; SQL_INIT_MODE=never keeps dev seed data out of prod.
docker run -d --name fundflow --restart unless-stopped \
    -p 8080:8080 \
    -e DB_URL='${DB_URL}' \
    -e DB_USERNAME='${DB_USERNAME}' \
    -e DB_PASSWORD='${DB_PASSWORD}' \
    -e DDL_AUTO=update \
    -e SQL_INIT_MODE=never \
    ${IMAGE}

# Fail the deploy if the app doesn't come up.
for i in \$(seq 1 30); do
    if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "FundFlow is healthy."
        exit 0
    fi
    sleep 2
done
echo "FundFlow did not become healthy in time" >&2
docker logs --tail 50 fundflow >&2
exit 1
EOF
