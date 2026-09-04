# Wyniki eksperymentów

Ten katalog zawiera surowe pliki CSV użyte do odczytania wyników pokrycia w eksperymentach.
Pliki są kopią wyników z katalogu `target/experiments`, aby wyniki nie opierały się tylko na ręcznie przepisanych liczbach.

## Struktura

- `raw/jacoco-evosuite/<projekt>/jacoco.csv` - raport JaCoCo dla testów wygenerowanych przez EvoSuite.
- `raw/jacoco-randoop/<projekt>/jacoco.csv` - raport JaCoCo dla testów wygenerowanych przez Randoop.
- `raw/evosuite-internal/<projekt>/statistics.csv` - wewnętrzny raport EvoSuite z procesu generowania testów.
- `summary.csv` - krótkie zestawienie liczb odczytanych z powyższych plików.

## Jak liczono JaCoCo branch coverage

W pliku `jacoco.csv` używane są kolumny:

- `BRANCH_MISSED`
- `BRANCH_COVERED`

Wynik jest liczony jako:

```text
BRANCH_COVERED / (BRANCH_MISSED + BRANCH_COVERED)
```

## Jak liczono wewnętrzne pokrycie EvoSuite

W pliku `statistics.csv` używane są kolumny:

- `Total_Goals`
- `Covered_Goals`

Wynik jest liczony jako:

```text
Covered_Goals / Total_Goals
```

To nie jest ta sama metryka co JaCoCo. JaCoCo liczy branche z bytecode'u przy uruchomieniu gotowych testów, a EvoSuite raportuje własne cele pokrycia z procesu generowania.
Dlatego do porównania narzędzi podstawową metryką powinno być JaCoCo, a wynik wewnętrzny EvoSuite może być pokazany pomocniczo.
