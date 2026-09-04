# Wyniki eksperymentów

Tu są materiały z eksperymentów opisanych w dokumentacji projektu. Pliki są kopią wyników z katalogu `target/experiments`

## Najważniejsze pliki

- `figures/testgen-coverage.svg` - tabela z wynikami raportów wygenerowanych przez `testgen`, osobno dla uruchomienia bez profilu i z profilem runtime.
- `figures/generators-comparison.svg` - tabela porównująca `testgen`, EvoSuite i Randoop. Główną metryką w tym zestawieniu jest pokrycie gałęzi mierzone przez JaCoCo.
- `raw/testgen/no-profile` - raporty `report.txt` z naszego programu dla uruchomień bez profilu runtime.
- `raw/testgen/with-profile-50` - raporty `report.txt` z naszego programu dla uruchomień z profilem runtime.

## Dane wygenerowane przez jacoco i evosuite

- `summary.csv` -  zestawienie wyników EvoSuite i Randoop.
- `raw/jacoco-testgen/summary.csv` - raport JaCoCo dla testów wygenerowanych przez `testgen`.
- `raw/jacoco-evosuite/<projekt>/jacoco.csv` - raport JaCoCo dla Evosuite
- `raw/jacoco-randoop/<projekt>/jacoco.csv` - raport JaCoCo dla testów Randoop
- `raw/evosuite-internal/<projekt>/statistics.csv` - wewnętrzny raport EvoSuite

