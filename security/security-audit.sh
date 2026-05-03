#!/bin/bash
# UCM Connect API — Bezpečnostný audit
# Spustenie: ./security-audit.sh

set -e
DATE=$(date +%Y-%m-%d)
mkdir -p target/security-reports

echo "=== Bezpečnostný audit — $DATE ==="
echo ""

# 1. SpotBugs + Find Security Bugs
echo "[1/2] SpotBugs (statická analýza)..."
./mvnw compile spotbugs:spotbugs -q 2>/dev/null && \
  cp target/spotbugsXml.xml "target/security-reports/spotbugs-$DATE.xml" && \
  echo "  OK — report: target/security-reports/spotbugs-$DATE.xml" || \
  echo "  CHYBA"

# 2. Závislosti — verzie
echo "[2/2] Kontrola verzií závislostí..."
./mvnw dependency:tree -q 2>/dev/null | grep -E ":(compile|runtime)" | sed 's/.*[+\\|]-//' | sort -u > "target/security-reports/dependencies-$DATE.txt"
echo "  OK — report: target/security-reports/dependencies-$DATE.txt"

echo ""
echo "Hotovo. Skontrolujte reporty v target/security-reports/"
