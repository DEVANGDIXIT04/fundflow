# AWS Deployment Checklist (free tier)

Everything below stays inside the AWS free tier **if your account is under 12
months old and you pick the sizes shown**. Items marked 💰 can cost money if
sized differently or left running past the free-tier window.

## 0. One-time account setup (manual — browser)

1. Sign up at <https://aws.amazon.com/free> (needs a card; you won't be charged
   within free-tier limits).
2. In the console: IAM → Users → Create user `fundflow-ci` →
   attach policies `AmazonEC2ContainerRegistryPowerUser` (push/pull images).
3. Create an **access key** for that user (Use case: CLI) and run locally:

   ```
   aws configure
   # paste Access Key ID + Secret, region: ap-south-1, output: json
   ```

## 1. ECR repository

```
aws ecr create-repository --repository-name fundflow \
    --image-scanning-configuration scanOnPush=true
```

Free tier: 500 MB/month private storage. Our image is ~374 MB → keep only the
latest few tags (lifecycle policy below avoids 💰 storage creep):

```
aws ecr put-lifecycle-policy --repository-name fundflow --lifecycle-policy-text '{
  "rules": [{"rulePriority": 1, "description": "keep last 5 images",
    "selection": {"tagStatus": "any", "countType": "imageCountMoreThan", "countNumber": 5},
    "action": {"type": "expire"}}]}'
```

## 2. Key pair + security groups

```
aws ec2 create-key-pair --key-name fundflow-key \
    --query "KeyMaterial" --output text > fundflow-key.pem
```

App security group — **only 22/80/443 in**, nothing else:

```
aws ec2 create-security-group --group-name fundflow-app-sg \
    --description "FundFlow EC2: ssh + http + https"
aws ec2 authorize-security-group-ingress --group-name fundflow-app-sg \
    --protocol tcp --port 22  --cidr <YOUR_IP>/32     # ssh from your IP only
aws ec2 authorize-security-group-ingress --group-name fundflow-app-sg \
    --protocol tcp --port 80  --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-name fundflow-app-sg \
    --protocol tcp --port 443 --cidr 0.0.0.0/0
```

DB security group — Postgres reachable **only from the app SG**, never public:

```
aws ec2 create-security-group --group-name fundflow-db-sg \
    --description "FundFlow RDS: postgres from app sg only"
aws ec2 authorize-security-group-ingress --group-name fundflow-db-sg \
    --protocol tcp --port 5432 --source-group fundflow-app-sg
```

## 3. EC2 instance (💰 if not t2.micro/t3.micro)

```
aws ec2 run-instances \
    --image-id <latest Amazon Linux 2023 AMI for your region> \
    --instance-type t3.micro \
    --key-name fundflow-key \
    --security-groups fundflow-app-sg \
    --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=fundflow}]'
```

Find the AMI: `aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query "Parameter.Value" --output text`

On the instance (`ssh -i fundflow-key.pem ec2-user@<public-dns>`):

```
sudo dnf install -y docker nginx
sudo systemctl enable --now docker nginx
sudo usermod -aG docker ec2-user   # then re-login
```

Give the instance ECR pull rights the clean way: IAM role
`fundflow-ec2-role` with `AmazonEC2ContainerRegistryReadOnly`, attached as an
instance profile (avoids putting CI keys on the box).

## 4. RDS PostgreSQL (💰 if not db.t3.micro / 20 GB)

```
aws rds create-db-instance \
    --db-instance-identifier fundflow-db \
    --engine postgres --engine-version 16 \
    --db-instance-class db.t3.micro \
    --allocated-storage 20 --storage-type gp2 \
    --master-username fundflow --master-user-password '<STRONG_PASSWORD>' \
    --db-name fundflow \
    --vpc-security-group-ids <fundflow-db-sg id> \
    --no-publicly-accessible \
    --backup-retention-period 1
```

Wait for `available`, then note the endpoint:
`aws rds describe-db-instances --db-instance-identifier fundflow-db --query "DBInstances[0].Endpoint.Address"`

## 5. Deploy

Locally (or let Jenkins do it — same script):

```
export AWS_ACCOUNT_ID=<12 digits> AWS_REGION=ap-south-1
export EC2_HOST=<public-dns> SSH_KEY_PATH=./fundflow-key.pem
export DB_URL="jdbc:postgresql://<rds-endpoint>:5432/fundflow"
export DB_USERNAME=fundflow DB_PASSWORD='<STRONG_PASSWORD>'
./scripts/deploy.sh latest
```

## 6. Jenkins hookup

Jenkins → Manage Jenkins:
- **Credentials** (Secret text unless noted): `aws-access-key-id`,
  `aws-secret-access-key`, `rds-db-url`, `rds-db-username`, `rds-db-password`,
  and `ec2-ssh-key` (kind **Secret file**, upload the .pem).
- **System → Global properties → Environment variables**: `AWS_ACCOUNT_ID`,
  `EC2_HOST`.

The Push-to-ECR and Deploy stages activate automatically on the next build.

## 7. Teardown (when done showing it off — avoids all 💰)

```
aws rds delete-db-instance --db-instance-identifier fundflow-db --skip-final-snapshot
aws ec2 terminate-instances --instance-ids <id>
aws ecr delete-repository --repository-name fundflow --force
```
