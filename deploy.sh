#!/bin/bash

# ===================================
# UCM Connect API - Deployment Script
# ===================================
# This script builds and deploys the Spring Boot application to AWS Elastic Beanstalk

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="ucm-connect-api"
ENV_NAME="ucm-connect-prod"
REGION="eu-central-1"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}UCM Connect API Deployment Script${NC}"
echo -e "${GREEN}========================================${NC}"

# Step 1: Check if AWS EB CLI is installed
echo -e "\n${YELLOW}[1/6] Checking AWS EB CLI...${NC}"
if ! command -v eb &> /dev/null; then
    echo -e "${RED}ERROR: AWS EB CLI is not installed${NC}"
    echo "Install it with: pip install awsebcli"
    exit 1
fi
echo -e "${GREEN}✓ AWS EB CLI found${NC}"

# Step 2: Clean previous builds
echo -e "\n${YELLOW}[2/6] Cleaning previous builds...${NC}"
./mvnw clean
echo -e "${GREEN}✓ Cleaned${NC}"

# Step 3: Run tests
echo -e "\n${YELLOW}[3/6] Running tests...${NC}"
./mvnw test
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Tests failed. Fix tests before deploying.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ All tests passed${NC}"

# Step 4: Build JAR
echo -e "\n${YELLOW}[4/6] Building application JAR...${NC}"
./mvnw package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ JAR built successfully${NC}"

# Verify JAR exists
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}ERROR: JAR file not found in target/${NC}"
    exit 1
fi
echo -e "${GREEN}✓ JAR file: $JAR_FILE${NC}"

# Step 5: Check EB initialization
echo -e "\n${YELLOW}[5/6] Checking Elastic Beanstalk configuration...${NC}"
if [ ! -d ".elasticbeanstalk" ]; then
    echo -e "${YELLOW}No EB configuration found. Initializing...${NC}"
    echo -e "${YELLOW}You will need to configure EB interactively.${NC}"
    eb init --region $REGION
fi
echo -e "${GREEN}✓ EB configured${NC}"

# Step 6: Deploy to Elastic Beanstalk
echo -e "\n${YELLOW}[6/6] Deploying to Elastic Beanstalk...${NC}"
echo -e "${YELLOW}Environment: $ENV_NAME${NC}"
echo -e "${YELLOW}Region: $REGION${NC}"

read -p "Are you sure you want to deploy? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Deployment cancelled${NC}"
    exit 1
fi

# Deploy
eb deploy $ENV_NAME
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Deployment failed${NC}"
    exit 1
fi

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}✓ Deployment successful!${NC}"
echo -e "${GREEN}========================================${NC}"

# Show environment status
echo -e "\n${YELLOW}Checking environment status...${NC}"
eb status $ENV_NAME

# Show application URL
echo -e "\n${GREEN}Application URL:${NC}"
eb status $ENV_NAME | grep "CNAME"

echo -e "\n${YELLOW}Useful commands:${NC}"
echo -e "  eb logs $ENV_NAME           - View recent logs"
echo -e "  eb health $ENV_NAME         - Check health status"
echo -e "  eb ssh $ENV_NAME            - SSH into instance"
echo -e "  eb open $ENV_NAME           - Open app in browser"
echo -e "  eb config $ENV_NAME         - Edit environment configuration"

echo -e "\n${GREEN}Done!${NC}"
