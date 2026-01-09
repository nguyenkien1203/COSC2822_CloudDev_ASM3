# AWS Deployment Guide

This guide describes how to deploy the Restaurant Microservices to AWS using CloudFormation.

## 1. Prerequisites
- AWS Account (Learner Lab compatible)
- AWS CLI installed and configured
- Docker installed
- Existing VPC (optional, but recommended for Learner Lab to reuse resources)

## 2. Infrastructure Deployment (`aws-infra.yaml`)
This template deploys the shared infrastructure:
- ECS Cluster
- Application Load Balancer (ALB)
- RDS PostgreSQL
- ElastiCache Redis
- SQS Queues

**Deploy Command:**
```bash
aws cloudformation deploy \
  --template-file aws-infra.yaml \
  --stack-name restaurant-infra \
  --parameter-overrides \
    DBPassword=<YOUR_DB_PASSWORD> \
  --capabilities CAPABILITY_NAMED_IAM
```
*Tip: In Learner Lab, if you don't have secondary subnets, you can reuse the same subnet IDs or create new ones if permissions allow.*

## 3. Build and Push Images
You need to build Docker images for each service and push them to ECR.
1. **Create Repositories:**
   ```bash
   aws ecr create-repository --repository-name auth-service
   aws ecr create-repository --repository-name menu-service
   aws ecr create-repository --repository-name order-service
   aws ecr create-repository --repository-name profile-service
   aws ecr create-repository --repository-name reservation-service
   ```

2. **Authenticate Docker:**
   ```bash
   aws ecr get-login-password --region <REGION> | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com
   ```

3. **Build and Push:**
   (Run from project root)
   ```bash
   # Example for Auth Service
   ./gradlew :auth-service:bootBuildImage --imageName=<ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/auth-service:latest
   docker push <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/auth-service:latest
   
   # Repeat for others...
   ```

## 4. Service Deployment (`aws-services.yaml`)
This template deploys the ECS Services and Task Definitions.

**Deploy Command:**
```bash
aws cloudformation deploy \
  --template-file aws-services.yaml \
  --stack-name restaurant-services \
  --parameter-overrides \
    InfraStackName=restaurant-infra \
    DBPassword=<YOUR_DB_PASSWORD> \
    AuthServiceImage=<URI_FROM_STEP_3> \
    MenuServiceImage=<URI_FROM_STEP_3> \
    OrderServiceImage=<URI_FROM_STEP_3> \
    ProfileServiceImage=<URI_FROM_STEP_3> \
    ReservationServiceImage=<URI_FROM_STEP_3> \
  --capabilities CAPABILITY_NAMED_IAM
```

## 5. Verification
After deployment, get the Load Balancer DNS from the outputs:
```bash
aws cloudformation describe-stacks --stack-name restaurant-services --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue" --output text
```
Visit that URL (with the appropriate API paths, e.g., `/api/v1/auth/...`) to test your services.
