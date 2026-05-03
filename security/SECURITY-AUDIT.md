# Bezpečnostný audit — UCM Connect API

## Spustenie

```bash
chmod +x security-audit.sh
./security-audit.sh
```

Skript spustí SpotBugs s Find Security Bugs (statická analýza bytecodu) a vygeneruje zoznam všetkých závislostí s verziami. Reporty sa uložia do `target/security-reports/`.

## Čo kontrolovať

1. **SpotBugs report** (`spotbugs-YYYY-MM-DD.xml`) — skontrolovať nové nálezy s HIGH závažnosťou
2. **Závislosti** (`dependencies-YYYY-MM-DD.txt`) — porovnať verzie s najnovšími dostupnými, skontrolovať či Spring Boot nemá novšiu verziu s bezpečnostnými záplatami

## Odporúčaná frekvencia

| Aktivita | Frekvencia |
|---|---|
| `./security-audit.sh` | Týždenne |
| Aktualizácia Spring Boot | Mesačne |
| Kontrola Spring Security Advisories | Priebežne |

## Aktualizácia závislostí

Pri nájdení CVE v závislosti — aktualizovať verziu Spring Boot v `pom.xml`. Spring Boot dependency management automaticky aktualizuje tranzitívne závislosti.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>NOVA_VERZIA</version>
</parent>
```
