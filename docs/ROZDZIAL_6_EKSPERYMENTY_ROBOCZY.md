# 6. Ocena eksperymentalna

W rozdziale oceniam działanie generatora `testgen` na dwóch bibliotekach Java i projekcie testowym Fit Bench. Sprawdzam uzyskane pokrycie, czas generowania oraz to, czy zapisane testy kompilują się i przechodzą poprawnie. Badam wpływ profilu argumentów i budżetu kandydatów na wyniki. Do porównania z istniejącymi już generatorami - Randoop i EvoSuite, używam narzędzia do pokrycia gałęzi JaCoCo.

## 6.1. Cel i zakres eksperymentów

Eksperymenty miały sprawdzić, czy przygotowany generator potrafi tworzyć testy pokrywające istotną część badanego kodu, oraz czy wartości argumentów z wcześniejszych wykonań pomagają w tym zadaniu. Przyjąłem następujące pytania:

1. Czy dołączenie profilu zwiększa pokrycie w porównaniu z generowaniem bez profilu?
2. Jak zmienia się wynik, gdy profil pochodzi z coraz większej części testów projektu?
3. Jak zwiększenie budżetu kandydatów wpływa na uzyskane pokrycie?
4. Ile nowych celów zostaje pokrytych po dodaniu profilu, a ile wcześniej pokrywanych celów nie zostaje osiągniętych przy tym samym budżecie?
5. Jak pokrycie uzyskane przez `testgen` wypada na tle Randoopa i EvoSuite?
6. Jak zmienia się czas generowania przy zwiększaniu pokrycia?


## 6.2. Badane projekty

Wybrałem własny projekt testowy Fit Bench oraz dwie biblioteki Apache Commons: Codec i Validator. Biblioteki pozwalają sprawdzić generator na kodzie, którego nie przygotowałem na potrzeby pracy. Udostępniają też własne testy, więc mogłem zebrać profile bez generowania dodatkowych testów innym narzędziem.

**Commons Codec** zawiera mechanizmy kodowania i dekodowania. Występują w nim
operacje na napisach, tablicach i danych binarnych. Taki zakres pozwala sprawdzić genera tor na bibliotece, w której nie każdą metodę da się wywołać przy obecnych ograniczeniach.

**Commons Validator** służy do sprawdzania poprawności danych, m.in. adresów, dat, liczb i numerów kontrolnych. Oprócz prostych warunków występują w nim też zależności
od formatu danych oraz bardziej złożone przygotowanie obiektów.


**Fit Bench** jest projektem przygotowanym na potrzeby generatora, wygenerowanym przez narzędzie sztucznej inteligencji. Wiele warunków zależy bezpośrednio od argumentów, wartości liczbowych i wartości typów wyliczeniowych.

Fit Bench zawiera głównie konstrukcje obsługiwane przez mój generator.  Pozwala sprawdzić cały proces generowania testów, ale nie pokazuje, jak generator poradzi sobie z dowolną aplikacją Java.

Fit Bench uruchomiłem przy tych samych budżetach i ziarnach co biblioteki, ale z innym źródłem profilu: testami wygenerowanymi wcześniej przez EvoSuite. Wyniki podaję dla każdego projektu osobno, bez obliczania wspólnej średniej.

### Zakres analizy trzech projektów
| Właściwość | Codec | Validator | Fit Bench |
| --- | ---: | ---: | ---: |
| Przeanalizowane klasy | 70 | 67 | 25 |
| Wykryte metody | 547 | 472 | 126 |
| Metody z przygotowanymi kandydatami | 179 | 308 | 125 |
| Odsetek metod z kandydatami | 32,7% | 65,3% | 99,2% |
| Wyznaczone cele pokrycia | 1486 | 981 | 715 |
| Cele w metodach z kandydatami | 253 | 292 | 707 |

Liczba klas w tabeli dotyczy analizy wykonanej przez `testgen`, nie wszystkich typów i plików klas w projekcie. Cele wyznaczono podczas analizy statycznej, przed wykonaniem kandydatów.

Przygotowany kandydat jest opisem wywołania metody. Sam utworzenie kandydata  nie gwarantuje, że wywołanie się powiedzie, ani że pokryje wszystkie cele w tej metodzie.

## 6.3. Przygotowanie profili argumentów

### Źródło danych

Profil zawiera wartości argumentów metod i konstruktorów zaobserwowane podczas wykonania badanego kodu. Dla Codec i Validatora zebrałem go przez uruchomienie istniejących testów bibliotek, na kopii ich kodu z dodaną instrumentacją argumentów. Dla Fit Bench wykorzystałem 509 metod testowych wygenerowanych wcześniej przez EvoSuite 1.2.0. Powstałe pliki były następnie opcjonalnym wejściem do `testgen`.

Testy EvoSuite uruchomiłem przez JUnit 4.13.2. Treść metod testowych pozostała bez zmian, ale inne warunki wykonania mogły wpłynąć na zebrane wartości. Testy autorów bibliotek uruchomiłem przez JUnit Jupiter 5.14.4.

Wszystkie profile w eksperymencie pochodzą z zewnętrznych testów, a nie z rzeczywistego używania aplikacji. Dla Codec i Validatora wykorzystałem testy autorów bibliotek, a dla Fit Bench testy wygenerowane przez EvoSuite. Są to więc profile syntetyczne. Pozwalają sprawdzić przydatność wartości z wcześniejszych wykonań, ale nie pokazują, jakie dane  występują
podczas zwykłego korzystania z kodu.


Generator odczytuje z profilu wpisy `hint`, czyli podpowiedzi wartości dla poszczególnych parametrów metod i konstruktorów. Nie odtwarza historii obiektów ani kolejności wywołań z testów źródłowych. Choć profil zawiera także całe zestawy argumentów, w badanej wersji generator nie wykorzystuje ich do odtwarzania całych scenariuszy, czy sekwencji metod.

### Cztery zakresy profilu

Dla każdego projektu przygotowałem profile z 25%, 50%, 75% i 100% metod testowych. Losowałem całe metody testowe, a nie wiersze z gotowego profilu. 

Identyfikatory metod zostały uporządkowane, a następnie jednokrotnie przetasowane. Z otrzymanej listy wybrałem kolejne, coraz większe fragmenty. Zestaw 25% zawiera się więc w zestawie 50%, ten w 75%, a ostatni w zestawie 100%. Liczbę metod zaokrąglałem w górę.

W ten sposób sprawdzam, co daje stopniowe rozszerzanie jednego zestawu testów. Nie losuję kilku różnych zestawów po 25% czy 50%, więc wyniki dotyczą tylko wybranych zestawów.

Procent odnosi się wyłącznie do liczby wybranych metod testowych. Nie oznacza takiego samego procentu pokrycia kodu, liczby wywołań, ani liczby wartości w profilu.

### Wyłączenia

Rejestrator zapisuje najwyżej 100 różnych wartości dla konkretnego argumentu. Profil 100% powstaje zatem z całego przyjętego zestawu testów, ale nie zawiera wszystkich wartości zaobserwowanych podczas ich wykonania. 

W bibliotekach Apache, wybór obejmował klasy pasujące do wzorca `.*Test`, z wyłączeniem `.*PerformanceTest`. W Codec dodatkowo wyłączyłem metodę `MurmurHash3Test.Przetwarza ona tablicę o rozmiarze około 2 GB i koszt czasowy był zbyt duży.

Tabela przedstawia liczebności przygotowanych profili.

| Projekt | Udział metod testowych | Wybrane metody | Zapisane podpowiedzi argumentów |
| --- | ---: | ---: | ---: |
| Codec | 25% | 300 | 8592 |
| Codec | 50% | 599 | 13866 |
| Codec | 75% | 898 | 15844 |
| Codec | 100% | 1197 | 16750 |
| Validator | 25% | 173 | 5995 |
| Validator | 50% | 345 | 7769 |
| Validator | 75% | 517 | 8951 |
| Validator | 100% | 689 | 10053 |
| Fit Bench | 25% | 128 | 685 |
| Fit Bench | 50% | 255 | 1153 |
| Fit Bench | 75% | 382 | 1559 |
| Fit Bench | 100% | 509 | 1858 |




## 6.4. Konfiguracja eksperymentów


Dla każdego projektu uruchomiłem generator bez profilu, oraz z każdym z czterech przygotowanych profili. Każdy wariant sprawdziłem przy dwóch konfiguracjach budżetu.

### Konfiguracja eksperymentów
| Parametr                                       | Mniejszy budżet | Większy budżet |
|------------------------------------------------| ---: | ---: |
| Maksymalna liczba kandydatów na metodę         | 120 | 500 |
| Liczba losowych kandydatów początkowych        | 30 | 125 |
| Liczba losowych kandydatów poddawanych mutacji | 3 | 3 |
| Ziarna generatora                              | 0, 1, 2, 3, 4 | 0, 1, 2, 3, 4 |

Mutacjom podlega kandydat bazowy - czyli kandydat z wartościami argumentów pochodzących z puli bazowej, oraz trzech wybranych kandydatów losowych - czyli kandydatów z losowymi wartościami argumentów uzyskanymi z analizy statycznej, oraz z profilu.  Limit na metodę obejmuje ich wszystkich łącznie, a także warianty powstałe przez mutacje.

Po dołączeniu profilu, połowa prób tworzenia losowych kandydatów korzysta z wartości bazowych i podpowiedzi statycznych, a druga połowa zarówno z wartości bazowych, podpowiedzi statycznych, i  wartości profilowych. Dla 30 prób podział wynosi 15 i 15, a dla 125 prób: 62 i 63. Bez profilu wszystkie próby korzystają z wartości bazowych i statycznych.

Przy większym budżecie zwiększam zarówno limit kandydatów, jak i liczbę prób losowych, zachowując ich proporcję.  Podział pul i liczba losowych kandydatów poddawanych mutacji pozostają takie same.

### Powtórzenia i wspólne ziarna

Każdy wariant - czyli wariant bez profilu, oraz z czterema różnymi profilami, uruchomiłem pięć razy, z ziarnami od 0 do 4. 


Łącznie przeprowadziłem 150 uruchomień: trzy projekty, dwa budżety, pięć wariantów profilu i pięć ziaren. Sto uruchomień dotyczyło bibliotek, a pięćdziesiąt Fit Bench. Każdy wynik bez profilu porównuję z czterema wynikami z profilem. 

### Zapis wyników i późniejszy pomiar testów

Każde uruchomienie ma osobny katalog z raportem, zapisanymi testami i zbiorem identyfikatorów pokrytych celów. 

Po wygenerowaniu testów kompiluję je i uruchamiam z JaCoCo na oryginalnym kodzie badanego projektu . Każdy zestaw mierzę osobno. 

Pomiar zapisanych testów wykonano przez JaCoCo 0.8.15 i JUnit Jupiter 5.14.4 na Javie 21.  Wszystkie 150 pomiarów zakończono -  problemy z asercjami Codec opisuję w podrozdziale 6.9.

## 6.5. Metryki i sposób przedstawiania wyników

### Pokrycie podczas generowania - pomiar wewnętrzny `testgen`

Podczas generowania mierzę przede wszystkim **pokrycie celów w metodach z przygotowanymi kandydatami**. Liczę, jaka część celów w tych metodach została trafiona. Wynik pokazuje pokrycie w metodach, dla których generator był w stanie przygotować wywołania zgodnie ze swoimi ograniczeniami. 

Zapisuję też pokrycie wszystkich celów wyznaczonych przez `testgen`, również w metodach bez przygotowanych kandydatów. Takie metody, do których nasz generator teoretycznie nie ma dostepu podczas przygotowywania kandydatów, mogą zostać wywołane przez inne metody podczas wykonania testu. Obie metryki różnią się zatem zakresem uwzględnionego kodu. Pokrycie całego projektu przez zapisane testy mierzę osobno narzędziem JaCoCo.

Pomiar wewnętrzny pokrycia gałęzi odbywa się podczas wykonywania kandydatów, przed zapisaniem testów.

Raporty `testgen` zawierają również następujące informacje:

- **Liczba wykonanych kandydatów** obejmuje wszystkich wykonanych kandydatów, nie tylko zapisane testy.
- **Liczba wybranych kandydatów** określa, ilu kandydatów wybrano do zapisania jako testy.
- **Liczba nieudanych wykonań kandydatów** określa, w ilu przypadkach wystąpił problem z przygotowaniem lub wykonaniem kandydata.

Podaję również czas generowania, aby pokazać, ile kosztuje uzyskana poprawa pokrycia. 

Dla każdej konfiguracji przedstawiam średnią z pięciu uruchomień,oraz najmniejszy i największy wynik. Wszystkie wyniki jednostkowe są w w repozytorium projektu. 

### Nowe i utracone cele

Dla danego projektu, budżetu i ziarna, porównuję zbiór celów pokrytych bez profilu, oznaczony jako B, ze zbiorem pokrytym z profilem, oznaczonym jako P. Liczę:

- nowe cele: liczba elementów P \\ B;
- utracone cele: liczba elementów B \\ P;
- wspólne cele: liczba elementów przecięcia B i P.

Liczba nowych celów minus liczba utraconych daje łączną zmianę pokrycia. Obliczam te wartości dla każdej pary uruchomień, a następnie podaję średnią z pięciu par. 

Przykładowo 40 nowych i 10 utraconych celów oznacza wzrost o 30, ale pokazuje także zmianę pokrytych miejsc. Sam wzrost o 30 nie ujawniłby utraty części wcześniejszego pokrycia przy wykorzystaniu samej analizy statycznej oraz wartości bazowych argumentów.

Porównanie wykorzystuje identyfikatory wewnętrznych celów `testgen`, nie liczniki JaCoCo.

### Pokrycie zapisanych testów

Główną wspólną metryką jest pokrycie gałęzi JaCoCo. Obliczam je jako udział pokrytych gałęzi wśród wszystkich gałęzi uwzględnionych w pomiarze.
.

Wyniku JaCoCo nie porównuję bezpośrednio z procentem pokrycia wewnętrznego `testgen`: narzędzia inaczej wyznaczają cele i mierzą je na innych etapach. Obok pokrycia podaję wyniki wykonania testów. Test zakończony nieudaną asercją również wykonuje kod, ale nie jest gotowym, poprawnym testem regresyjnym. 

## 6.6. Eksperyment - wpływ profilu na pokrycie w metodach z kandydatami

Najpierw porównuję warianty - przy mniejszym budżecie 120 kandydatów i 30 prób losowych. Tabela pokazuje średnie pokrycie celów w metodach, dla których generator był w tanie przygotować kandydatów, z pięciu ziaren.

| Profil | Codec | Validator | Fit Bench |
| --- | ---: | ---: | ---: |
| Bez profilu | 57,31% | 52,67% | 86,62% |
| 25% | 74,70% | 63,63% | 89,42% |
| 50% | 81,50% | 65,82% | 89,70% |
| 75% | 85,30% | 70,68% | 89,22% |
| 100% | 85,85% | 71,71% | 89,45% |

Profil zwiększył średnie pokrycie we wszystkich projektach. Dla pełnego profilu wzrost wyniósł 28,54 punktu procentowego dla Codec, 19,04 dla Validatora i 2,83 dla Fit Bench.

Fit Bench już bez profilu osiągał wysokie pokrycie. Wiele jego warunków zależy bezpośrednio od argumentów i prostych stałych, co odpowiada możliwościom/ograniczeniom generatora. Profil nadal pomagał, ale pozostawało mniej niepokrytych celów niż w bibliotekach.

Większy profil nie dawał jednakowej poprawy we wszystkich projektach. W Validatorze wyraźniejszy wzrost wystąpił między 50% a 75%, natomiast w Fit Bench wyniki czterech profili były zbliżone. 

Poniżej podaję minimum, maksimum i odchylenie standardowe dla wszystkich wariantów profilu przy budżecie 120 kandydatów. 

**Minimum i maksimum pokrycia [%]**

| Profil | Codec | Validator | Fit Bench |
| --- | ---: | ---: | ---: |
| Bez profilu | 57,31–57,31% | 52,40–52,74% | 86,00–87,55% |
| 25% | 73,91–75,49% | 63,01–64,04% | 87,98–90,95% |
| 50% | 79,05–83,79% | 65,41–66,44% | 89,25–90,52% |
| 75% | 83,79–87,75% | 70,21–71,23% | 87,13–90,66% |
| 100% | 84,98–86,56% | 71,23–72,60% | 87,27–90,52% |

**Odchylenie standardowe [p.p.]**

| Profil | Codec | Validator | Fit Bench |
| --- | ---: | ---: | ---: |
| Bez profilu | 0,00 | 0,15 | 0,74 |
| 25% | 0,74 | 0,38 | 1,16 |
| 50% | 2,19 | 0,38 | 0,50 |
| 75% | 1,54 | 0,46 | 1,30 |
| 100% | 0,71 | 0,57 | 1,27 |

Odchylenie standardowe obliczono z pięciu uruchomień, z dzielnikiem n − 1, i podano w punktach procentowych (p.p.). Profil był taki sam we wszystkich powtórzeniach, więc odchylenie nie obejmuje różnic między losowaniami testów do profilu.

Wysokie pokrycie w metodach z kandydatami nie oznacza obsługi całego projektu. W szczególności 85,85% dla Codec dotyczy 253 celów w 179 metodach dla których generator był w stanie przygotować kandydatów, a nie wszystkich 1486 celów ani wszystkich 547 wykrytych metod.

## 6.7. Eksperyment - wpływ budżetu i koszt generowania

Przy większym budżecie, czyli 500 kandydatach i 125 próbach losowych, otrzymałem następujące średnie pokrycie celów w metodach z kandydatami:

| Profil | Codec | Validator | Fit Bench |
| --- | ---: | ---: | ---: |
| Bez profilu | 57,31% | 52,95% | 89,96% |
| 25% | 81,34% | 63,97% | 93,61% |
| 50% | 86,96% | 66,64% | 93,41% |
| 75% | 90,67% | 71,30% | 94,06% |
| 100% | 90,59% | 73,22% | 93,78% |

W Codec większy budżet bez profilu nie zmienił średniego pokrycia w metodach z kandydatami: pozostało ono na poziomie 57,31%. Z pełnym profilem wynik wzrósł z 85,85% do 90,59%. W Validatorze poprawa była mniejsza: bez profilu z 52,67% do 52,95%, a z pełnym profilem z 71,71% do 73,22%. 

W Fit Bench większy budżet pomagał także bez profilu: średnia wzrosła z 86,62% do 89,96%. Z pełnym profilem wzrosła z 89,45% do 93,78%. Możliwe, że wartości potrzebne do pokrycia kolejnych celów były już dostępne, ale mniejszy budżet nie wystarczał do sprawdzenia odpowiednich kombinacji.

Tabela przedstawia koszt generowania bez profilu i z pełnym profilem. Wszystkie liczby są średnimi z pięciu uruchomień; czas nie obejmuje zbierania profilu ani wykonania zapisanych testów z JaCoCo.

| Projekt | Budżet | Profil | Wykonani kandydaci | Wybrane testy | Czas [s] |
| --- | ---: | ---: | ---: | ---: | ---: |
| Codec | 120 | brak | 5089,6 | 144,0 | 10,68 |
| Codec | 120 | 100% | 5686,0 | 217,4 | 11,76 |
| Codec | 500 | brak | 6500,4 | 148,0 | 12,56 |
| Codec | 500 | 100% | 9203,4 | 241,0 | 17,20 |
| Validator | 120 | brak | 7900,2 | 126,0 | 13,79 |
| Validator | 120 | 100% | 9306,0 | 173,4 | 15,05 |
| Validator | 500 | brak | 13927,6 | 127,6 | 19,98 |
| Validator | 500 | 100% | 18298,0 | 181,6 | 23,07 |
| Fit Bench | 120 | brak | 14014,2 | 298,2 | 7,64 |
| Fit Bench | 120 | 100% | 14139,0 | 314,4 | 7,73 |
| Fit Bench | 500 | brak | 55729,0 | 308,6 | 22,64 |
| Fit Bench | 500 | 100% | 56473,0 | 340,4 | 23,02 |

Zwiększenie limitu kandydatów nie powoduje proporcjonalnego wzrostu ich rzeczywistej liczby. Część metod ma ograniczoną pulę wartości i wariantów, a duplikaty są pomijane. Dodatkowe wykonania nie muszą również pokrywać nowych celów. Dla Validatora z pełnym profilem liczba kandydatów wzrosła z około 9306 do 18298, podczas gdy pokrycie w metodach z kandydatami wzrosło o około 1,51 punktu procentowego. Znacznie większa liczba wykonań przyniosła więc niewielką poprawę pokrycia.

### Nowe i utracone cele

Sam procent pokrycia nie pokazuje, czy po dodaniu profilu generator nadal trafia te same cele. Dlatego liczę również cele nowe i utracone. Tabela przedstawia średnie z pięciu par uruchomień bez profilu i z profilem 100%. Uwzględnia **wszystkie wewnętrzne cele testgen**, nie tylko cele w metodach dla których generator był w stanie przygotować kandydatów. Nie są to liczniki gałęzi JaCoCo.

| Projekt | Budżet | Nowe cele | Utracone cele | Bilans |
| --- | ---: | ---: | ---: | ---: |
| Codec | 120 | 193,2 | 2,2 | 191,0 |
| Codec | 500 | 225,0 | 1,0 | 224,0 |
| Validator | 120 | 166,8 | 1,0 | 165,8 |
| Validator | 500 | 174,6 | 0,6 | 174,0 |
| Fit Bench | 120 | 33,6 | 13,4 | 20,2 |
| Fit Bench | 500 | 34,0 | 7,0 | 27,0 |

W obu bibliotekach nowe cele zdecydowanie przeważały nad utraconymi.

W Fit Bench utrata była większa. Przy budżecie 120 pełny profil dawał średnio 33,6 nowych celów, ale tracił 13,4. Przy budżecie 500 liczba nowych celów była podobna, 34,0, natomiast utrata zmniejszyła się do 7,0. 

Dodanie profilu nie tylko pomaga pokryć nowe cele: część wcześniej pokrywanych celów może pozostać nieosiągnięta. Prawdopodobnym powodem jest podział  budżetu między dwie pule. Po dołączeniu profilu, mniej prób losowych korzysta wyłącznie z podpowiedzi statycznych i wartości bazowych. W Fit Bench, podpowiedzi statyczne dobrze odpowiadają warunkom występującym w kodzie, dlatego ograniczenie budżetu dla kombinacji z analizy statycznej może zmniejszyć pokrycie. 

## 6.8. Eksperyment - pomiar rzeczywistego pokrycia JaCoCo i porównanie z innymi narzędziami

### Wynik wykonania

Pomiar objął wszystkie 150 zestawów.


| Projekt | Zestawy | Wykonania testów | Udane | Nieudane |
| --- | ---: | ---: | ---: | ---: |
| Codec | 50 | 9910 | 9620 | 290 |
| Validator | 50 | 7762 | 7762 | 0 |
| Fit Bench | 50 | 16078 | 16078 | 0 |

Są to sumy wykonań ze wszystkich konfiguracji, nie liczba unikalnych testów jednego projektu. W każdym zestawie Codec nie przeszło od 5 do 8 testów; wszystkie niepowodzenia dotyczyły metod `Crypt`, `Md5Crypt` i `UnixCrypt`. Metody te  mogą zwracać różne wyniki przy tych samych argumentach, podczas gdy generator zapisuje konkretny wynik w asercji. 

Pokrycie JaCoCo dlaCodec obejmuje także kod wykonany przez te testy zakończone nieudaną asercją. Nie jest to zatem pomiar wyłącznie poprawnych testów regresyjnych. 

### Pokrycie gałęzi całego projektu

 Poniższe wyniki są średnimi z pięciu osobno wykonanych zestawów testów.

Budżet 120 kandydatów, 30 prób losowych:

| Profil | Codec | Validator | Fit Bench |
| --- | ---: | ---: | ---: |
| Bez profilu | 36,41% | 20,62% | 75,46% |
| 25% | 41,29% | 30,30% | 78,13% |
| 50% | 46,10% | 31,35% | 78,19% |
| 75% | 47,78% | 35,64% | 77,99% |
| 100% | 48,66% | 36,56% | 78,21% |

Budżet 500 kandydatów, 125 prób losowych:

| Profil | Codec | Validator | Fit Bench |
| --- | ---: | ---: | ---: |
| Bez profilu | 36,56% | 20,68% | 79,20% |
| 25% | 42,32% | 30,80% | 82,13% |
| 50% | 48,87% | 31,93% | 82,39% |
| 75% | 49,87% | 35,94% | 82,79% |
| 100% | 50,50% | 37,05% | 82,23% |

JaCoCo potwierdziło korzyść z profilu, którą już zaobserwowaliśmy wcześniej w wewnętrznym pomiarze. Przy większym budżecie, profil 100% poprawił średnie pokrycie względem braku profilu o 13,94 punktu procentowego w Codec, 16,37 w Validatorze i 3,03 w Fit Bench. 

Poniżej podaję minimum, maksimum i odchylenie standardowe pokrycia JaCoCo dla wszystkich wariantów profilu przy budżecie 500 kandydatów.

**Minimum i maksimum pokrycia [%]**

| Profil | Codec | Validator | Fit Bench |
| --- | ---: | ---: | ---: |
| Bez profilu | 36,45–36,65% | 20,52–20,82% | 78,41–79,90% |
| 25% | 41,49–42,72% | 30,60–30,95% | 81,09–83,38% |
| 50% | 47,88–49,50% | 31,66–32,17% | 82,09–83,18% |
| 75% | 49,11–50,42% | 35,71–36,22% | 81,89–83,18% |
| 100% | 49,94–50,97% | 36,88–37,44% | 81,39–83,38% |

**Odchylenie standardowe [p.p.]**

| Profil | Codec | Validator | Fit Bench |
| --- | ---: | ---: | ---: |
| Bez profilu | 0,07 | 0,11 | 0,59 |
| 25% | 0,49 | 0,14 | 0,91 |
| 50% | 0,66 | 0,21 | 0,46 |
| 75% | 0,57 | 0,19 | 0,54 |
| 100% | 0,48 | 0,22 | 0,78 |

Odchylenie standardowe obliczono z pięciu uruchomień, z dzielnikiem n − 1, i podano w punktach procentowych (p.p.). Profil był taki sam we wszystkich powtórzeniach, więc odchylenie nie obejmuje różnic między losowaniami testów do profilu.

W Fit Bench przy większym budżecie profil 100% dał nieco niższą średnią niż profil 75%: 82,23% wobec 82,79%. Różnica wynosi około 0,56 punktu procentowego. Może wynikać z losowania, lub z małej użyteczności dodatkowych informacji otrzymywanych z profilu z profilu 100% względem profilu 75%.

### Porównanie z Randoopem i EvoSuite

Wyniki `testgen` porównuję z Randoopem i EvoSuite za pomocą pokrycia gałęzi mierzonego przez JaCoCo. 

Dla `testgen` podaję wyniki przy budżecie 500 kandydatów i 125 prób losowych, bez profilu i z profilem 100%. Oba warianty pokazuję oddzielnie, ponieważ wariant profilowy korzysta z dodatkowych danych z wcześniejszych wykonań.


| Projekt | testgen bez profilu, średnia z 5 | testgen z profilem 100%, średnia z 5 | Randoop, średnia z 5 | EvoSuite, średnia z 5 |
| --- | ---: | ---: |---------------------:|----------------------:|
| Codec | 36,56% | 50,50% |                66,0% |                 83,6% |
| Validator | 20,68% | 37,05% |                52,2% |                 56,7% |
| Fit Bench | 79,20% | 82,23% |                58,8% |                 98,6% |

W tym eksperymencie, profil Fit Bench pochodzi z wcześniej wygenerowanych testów EvoSuite. Wariant `testgen` z profilem korzysta więc z danych dostarczonych przez porównywane narzędzie. 

W eksperymencie, Evosuite i Randoop zostały uruchomione pięć razy dla każdego projektu, z ziarnami od 0 do 4. Randoop 4.3.4 otrzymuje budżet 180 sekund na projekt. EvoSuite 1.2.0 korzysta z kryterium `BRANCH` i budżetu wyszukiwania 10 sekund na klasę w obu bibliotekach, oraz 30 sekund na klasę w Fit Bench. Każdy zapisany zestaw wymaga potem kolejnego wykonania z pomiarem JaCoCo.

Narzędzia mają różne limity: `testgen` ogranicza liczbę kandydatów na metodę, Randoop czas generowania dla projektu, a EvoSuite czas generowania dla klasy. Porównuję więc wyniki konkretnych konfiguracji, nie wyniki uzyskane przy takim samym czasie pracy. 

W dotychczasowym zestawieniu EvoSuite osiąga najwyższe pokrycie we wszystkich trzech projektach. Randoop uzyskuje wynik wyższy od obu wariantów `testgen` w bibliotekach, natomiast niższy w Fit Bench. Profil zmniejsza różnicę względem pozostałych narzędzi. Wynik Fit Bench pokazuje, że prosta strategia  może wystarczyć do pokrycia znacznej części kodu, jeśli projekt korzysta głównie z konstrukcji mieszczących się w ograniczeniach mojego generatora . 



## 6.10. Ograniczenia i wnioski

### Ograniczenia

Profile pochodzą z testów, nie z obserwacji użytkowników, czyli są to profile syntetyczne. Profil 100% oznacza cały przyjęty zestaw testów, a nie wszystkie możliwe dane. 

Przy zwiększaniu budżetu zmieniałem jednocześnie limit kandydatów i liczbę prób losowych.  Eksperyment nie oddziela całkowicie wpływu danych z profilu, od jakości losowania i mutacji. 

Wysokie pokrycie nie oznacza jeszcze, że asercje są właściwe ani że program spełnia wymagania biznesowe. Pokazują to nieudane asercje Codec. 

### Wnioski

Eksperymenty wykazały, że w badanych projektach profil argumentów poprawiał średnie pokrycie zarówno w wewnętrznym pomiarze, jak i podczas wykonania zapisanych testów z JaCoCo. Największa poprawa względem braku profilu wystąpiła w bibliotekach. W Fit Bench wariant bez profilu osiągał już wysoki wynik,  dlatego dodatkowa korzyść była mniejsza.

Większy budżet pomagał lepiej wykorzystać dostępne argumenty, ale wzrost liczby wykonywanych kandydatów nie był proporcjonalny do wzrostu pokrycia. W bibliotekach bez profilu poprawa była niewielka. Warto więc rozwijać sposób doboru wartości i ich kombinacji, a nie tylko zwiększać liczbę prób. 

### Materiały pomiarowe

Raporty i wygenerowane przez eksperymenty pliku znajdują się w katalogu `docs/experiments/2026-09/` projektu. `summary.csv` zawiera średnie i zakresy z pięciu ziaren, a katalogi `codec`, `validator` i `fit-bench` wyniki konkretnych pomiarów. Dla każdego projektu, budżetu, profilu i ziarna istnieje raport generatora, spis pokrytych celów, raport JaCoCo, i wynik wykonania testów.

---

