# QRaft – Plan de testare manuală

Flow complet de testare pentru fiecare ecran, cu cazuri **pozitive** (P) și **negative** (N).
Bifează coloana ✓ la rulare și notează observațiile în dreptul cazului.

## 0. Pregătire

| Ce | Detalii |
|---|---|
| Dispozitive | 1 telefon fizic (necesar pentru scanare) + opțional emulator / al doilea telefon pentru partajare |
| Conturi | **A** = proprietar, **B** = al doilea cont (folosit la partajare). Ambele trebuie create o dată în aplicație |
| Instalare curată | Dezinstalează aplicația înainte de prima rulare a planului |
| Rețea | Testele marcate 📶 cer comutarea Wi-Fi / date mobile (sau Airplane mode) |
| Unelte | Logcat filtrat pe `ro.upt.qraft` (tag-uri `SyncManager`, `FirestoreDataSource`), [Firebase Console → Firestore](https://console.firebase.google.com/project/qraft-anollex/firestore) |
| Date de test | Inventar „Laborator” → locații „Raft A” (rădăcină), „Sertar 1” (în Raft A) → item „Multimetru” |

Convenție coduri: locații `xxx-000-000` (prefix = primele 3 consoane ale inventarului), itemi `000-000-000-000`.

---

## 1. Autentificare / Creare cont

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| AUTH-P1 | „Nu ai cont? Creează unul” → email nou valid + parolă ≥ 6 caractere → „Creează cont” | Spinner pe buton, apoi ecranul „Inventarele mele” (gol) | |
| AUTH-P2 | Deconectare → introdu contul de la P1 → „Autentificare” | Se deschide „Inventarele mele” cu datele contului | |
| AUTH-P3 | Închide aplicația complet și redeschide-o (cont logat) | Pornește direct în „Inventarele mele”, fără să apară ecranul de login | |
| AUTH-P4 | Comută între „Nu ai cont?” / „Ai deja cont?” | Titlul și textul butonului se schimbă (Autentificare ↔ Creare cont) | |
| AUTH-P5 | Login cu emailul scris cu majuscule (`Test@Example.com`) | Login reușit (Firebase nu ține cont de majuscule la email) | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| AUTH-N1 | Câmpuri goale sau doar email / doar parolă | Butonul principal este dezactivat | |
| AUTH-N2 | Login cu parolă greșită | Mesaj roșu „Email sau parolă greșită.”, rămâi pe ecran | |
| AUTH-N3 | Login cu email inexistent | „Email sau parolă greșită.” (Firebase nu dezvăluie dacă emailul există) | |
| AUTH-N4 | Creare cont cu email deja folosit | „Există deja un cont cu acest email.” | |
| AUTH-N5 | Creare cont cu parolă < 6 caractere | „Parola trebuie să aibă cel puțin 6 caractere.” | |
| AUTH-N6 | Email fără format valid (`abc`) | „Adresa de email nu este validă.” | |
| AUTH-N7 📶 | Fără internet → login | „Nu există conexiune la internet.”, aplicația nu se blochează | |
| AUTH-N9 | Tastatura deschisă | Formularul urcă deasupra tastaturii; butonul rămâne vizibil | |
| AUTH-N8 | Apasă rapid de mai multe ori pe buton | O singură cerere (butonul e dezactivat cât timp rulează) | |

---

## 2. Inventarele mele

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| INV-P1 | Cont nou, fără inventare | Mesaj „Nu ai încă niciun inventar. Apasă + pentru a crea unul.” | |
| INV-P2 | „+” → „Laborator” → Creează | Rândul „Laborator / Prefix: lbr” apare în listă | |
| INV-P3 | Tastează în dialog numele | Previzualizare live: „Locațiile vor avea coduri de forma lbr-000-001” | |
| INV-P4 | Atinge rândul inventarului | Se deschide ecranul inventarului | |
| INV-P5 | Atinge ⚙ pe rând | Se deschide „Setări și partajare” | |
| INV-P6 | Atinge 🔍 (Scanează) | Se deschide scannerul | |
| INV-P7 | Inventar partajat cu B (vezi §10) → loghează-te cu B | Inventarul apare la B cu chip de rol (Editare / Vizualizare); la A apare „· partajat cu 1” | |
| INV-P8 | Deconectare → „Deconectare” în dialog | Dialog „Te deconectezi?”; după confirmare revii la login; butonul Înapoi nu te întoarce în listă | |
| INV-P9 | Deconectare → Renunță | Rămâi logat în „Inventarele mele” | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| INV-N1 | Dialog: nume gol sau doar spații | „Creează” dezactivat | |
| INV-N2 | Dialog → Renunță / atinge în afara dialogului | Nu se creează nimic | |
| INV-N3 | Nume fără consoane suficiente: „Rai”, „Aia” | Prefix `rxx`, respectiv `xxx` | |
| INV-N4 | Nume cu diacritice „Șopron 2 - Mobilă” | Prefix `spr` (diacriticele și cifrele sunt ignorate) | |
| INV-N5 | Nume foarte lung (> 60 caractere) | Se salvează; textul nu strică layout-ul | |
| INV-N6 📶 | Offline → creează inventar | Apare imediat în listă; se urcă în Firestore după reconectare | |
| INV-N7 📶 | Offline: modifică un item → Deconectare | Dialogul avertizează „Ai N modificări care nu au ajuns încă în cloud…” și butonul devine roșu „Deconectează-te oricum” | |

---

## 3. Ecranul inventarului (rădăcina – lista de locații)

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| ROOT-P1 | Inventar fără locații | Mesaj „Inventarul nu are încă locații…” + buton „Locație” | |
| ROOT-P2 | „Locație” → completează → Salvează | Locația apare în secțiunea „Locații” cu QR și cod `lbr-000-001` | |
| ROOT-P3 | Cu cel puțin o locație → „Adaugă” | Meniu cu „Item” și „Locație” | |
| ROOT-P4 | „Adaugă” → Item | Formular „Item nou” cu locația **necompletată** | |
| ROOT-P5 | Atinge o locație | Se deschid detaliile locației | |
| ROOT-P6 | 🔍 din bara de sus | Se deschide scannerul | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| ROOT-N1 | Inventar fără locații | Nu există opțiunea „Item” (un item are nevoie de o locație) | |
| ROOT-N2 | Cont cu rol Vizualizare | Niciun buton de adăugare | |
| ROOT-N3 | Creează item din rădăcină → Salvează | Se deschide direct locația aleasă, cu itemul în listă; Înapoi duce la rădăcină | |
| ROOT-N4 | Sub-locațiile nu apar la rădăcină | La rădăcină apar doar locațiile fără părinte | |

---

## 4. Detalii locație

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| LOC-P1 | Deschide „Raft A” | Antet cu QR, cod, descriere; secțiunile „Sub-locații” și „Itemi (N)” | |
| LOC-P2 | Atinge QR-ul din antet | Dialog cu QR mare + cod; „Închide” îl închide | |
| LOC-P3 | Locație fără itemi | Text „Niciun item în această locație.” + buton „Adaugă item” | |
| LOC-P4 | „Adaugă” → Item | Formular cu locația curentă precompletată | |
| LOC-P5 | „Adaugă” → Sub-locație | Formular; după salvare apare în „Sub-locații” cu codul următor (`lbr-000-002`) | |
| LOC-P6 | ✏ Editează → schimbă numele → Salvează | Titlul și lista se actualizează; **codul rămâne același** | |
| LOC-P7 | Atinge o sub-locație / un item | Navighează la detaliile respective | |
| LOC-P8 | 🗑 Șterge → confirmă | Locația, sub-locațiile ei și toți itemii din ele dispar; revii la ecranul anterior | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| LOC-N1 | 🗑 → Renunță | Nimic nu se șterge | |
| LOC-N2 | Rol Vizualizare | Fără ✏, 🗑, „Adaugă”, „Adaugă item” | |
| LOC-N3 | Șterge `lbr-000-001`, apoi creează o locație nouă | Primește din nou `lbr-000-001` (numerele libere sunt refolosite – comportament cunoscut) | |
| LOC-N4 | Scanează QR-ul unei locații șterse | Mesaj „Obiectul nu există sau nu ai acces la el.” | |

---

## 5. Formular locație (nouă / editare)

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| LFORM-P1 | Locație nouă | Câmpul Cod arată „Generat automat la salvare” (needitabil) | |
| LFORM-P2 | Nume + descriere → Salvează | Se salvează și revii la ecranul anterior | |
| LFORM-P3 | Editare | Câmpurile sunt precompletate; codul real e afișat | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| LFORM-N1 | Nume gol / doar spații | „Salvează” dezactivat | |
| LFORM-N2 | ✕ (Renunță) sau Înapoi | Nu se salvează nimic | |
| LFORM-N3 | Apasă „Salvează” de 2 ori rapid | Se creează o singură locație | |
| LFORM-N4 | Nume cu spații la capete „  Raft B  ” | Se salvează „Raft B” (trim) | |

---

## 6. Formular item (nou / editare)

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| IFORM-P1 | Item nou | Cod „Generat automat la salvare”, cantitate implicită 1 | |
| IFORM-P2 | Completează Nume, Categorie, Cantitate, Locație, Note → Salvează | Itemul apare în locația aleasă, cu cod `000-000-000-000` | |
| IFORM-P3 | Selector „Locație *” | Listează toate locațiile inventarului sortate după cod (`cod · nume`) | |
| IFORM-P4 | Editare → schimbă locația → Salvează | Itemul se mută: dispare din vechea locație, apare în cea nouă; codul rămâne | |
| IFORM-P5 | Editare → modifică nume / cantitate / note | Modificările apar în detalii | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| IFORM-N1 | Nume gol | „Salvează” dezactivat | |
| IFORM-N2 | Cantitate ștearsă (gol) | Câmp marcat cu eroare, „Salvează” dezactivat | |
| IFORM-N3 | Tastează litere / „-5” / „3.5” la cantitate | Se păstrează doar cifrele | |
| IFORM-N4 | Tastează > 6 cifre | Se limitează la 6 cifre | |
| IFORM-N5 | Item nou din rădăcină fără locație aleasă | „Salvează” dezactivat; câmpul Locație arată „Alege locația” (nu e marcat ca eroare) | |
| IFORM-N9 | Deschide tastatura în formular | Bara de sus rămâne pe loc; formularul se poate derula deasupra tastaturii | |
| IFORM-N6 | ✕ / Înapoi | Nu se salvează nimic | |
| IFORM-N7 | „Salvează” de 2 ori rapid | Un singur item creat | |
| IFORM-N8 | Text cu virgule, ghilimele, „=SUM(A1)” în note | Se salvează exact; în CSV apare corect (vezi EXP-P3/EXP-N4) | |

---

## 7. Detalii item

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| ITEM-P1 | Deschide un item | QR, cod, cantitate, locație, categorie, note, ultima modificare | |
| ITEM-P2 | „+” / „−” | Cantitatea se modifică imediat; „(nesincronizat)” apare scurt și dispare când e online | |
| ITEM-P3 | Atinge QR-ul | Dialog cu QR mare | |
| ITEM-P4 | Atinge cardul Locație | Navighează la locația itemului | |
| ITEM-P5 | ✏ Editează | Se deschide formularul precompletat | |
| ITEM-P6 | 🗑 → confirmă | Itemul e șters; revii la ecranul anterior | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| ITEM-N1 | Cantitate 0 | „−” dezactivat (nu există cantitate negativă) | |
| ITEM-N2 | 🗑 → Renunță | Itemul rămâne | |
| ITEM-N3 | Rol Vizualizare | Fără ✏, 🗑, „+”, „−”; doar citire | |
| ITEM-N4 | Itemul e șters de pe alt dispozitiv cât timp e deschis | Mesaj „Itemul nu mai există sau nu ai acces la el.” | |
| ITEM-N5 📶 | Offline → „+” | „(nesincronizat)” rămâne până la reconectare, apoi dispare | |

---

## 8. Scanner (doar pe telefon fizic)

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| SCAN-P1 | Prima deschidere | Cerere de permisiune pentru cameră | |
| SCAN-P2 | Permite camera | Previzualizare cu chenar verde și „Scanează un cod QRaft…” | |
| SCAN-P3 | Scanează QR-ul unei locații (de pe ecran sau din PDF printat) | Vibrație scurtă → detaliile locației; Înapoi revine unde erai înainte de scanare | |
| SCAN-P4 | Scanează QR-ul unui item | Vibrație → detaliile itemului | |
| SCAN-P5 | Scanează o etichetă din PDF-ul exportat | Același rezultat ca P3/P4 | |
| SCAN-P6 | ✕ | Scannerul se închide | |
| SCAN-P7 | B scanează un cod dintr-un inventar partajat cu el | Se deschide obiectul | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| SCAN-N1 | Refuză permisiunea | Mesaj + butoanele „Permite accesul la cameră”, „Deschide setările aplicației”, „Închide” | |
| SCAN-N7 | Refuză de 2 ori → „Deschide setările aplicației” → activează Camera → Înapoi | Scannerul pornește automat, fără redeschidere | |
| SCAN-N2 | Scanează un QR străin (link web, Wi-Fi) | Este ignorat: fără vibrație, fără mesaj; scanarea continuă | |
| SCAN-N3 | QR cu text `qraft://ceva` sau `qraft://item/` | Mesaj „Codul QR nu aparține QRaft.”, apoi scanarea se reia | |
| SCAN-N4 | QR al unui item/locații șterse | „Obiectul nu există sau nu ai acces la el.”, scanarea se reia | |
| SCAN-N5 | B scanează un cod dintr-un inventar **nepartajat** cu el | „Obiectul nu există sau nu ai acces la el.” | |
| SCAN-N6 | Ține codul în fața camerei mai multe secunde | Se deschide un singur ecran (o singură vibrație) | |

---

## 9. Setări și partajare – informații și export

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| SET-P1 | Deschide setările | Nume inventar, „Prefix locații: lbr”, cardurile Export / Partajează / Membri | |
| SET-P2 | Proprietar: ✏ Redenumește → nume nou → Salvează | Numele se schimbă peste tot; **prefixul rămâne** | |
| EXP-P1 | „Itemi (CSV)” | Se deschide salvarea cu nume propus `QRaft_<inventar>_<dată>.csv` | |
| EXP-P2 | Salvează | Snackbar „Exportat CSV: N itemi” (sau „1 item”) | |
| EXP-P3 | Deschide CSV-ul (Sheets / LibreOffice) | Coloane: Cod item, Nume, Categorie, Cantitate, Cod locație, Locație (cale `Raft A / Sertar 1`), Note, Ultima modificare; diacritice corecte | |
| EXP-P4 | „Etichete QR” → Salvează | Snackbar „Exportat PDF: N etichete” | |
| EXP-P5 | Deschide PDF-ul | A4, grilă 3×7, linii punctate, întâi locațiile apoi itemii; antet „QRaft · <inventar> · pagina X/Y” | |
| EXP-P6 | Inventar cu > 21 obiecte | PDF pe mai multe pagini, numerotare corectă | |
| EXP-P7 | Rol Vizualizare → export | Funcționează (exportul e permis tuturor membrilor) | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| SET-N1 | Membru (Editare / Vizualizare) | Fără ✏ Redenumește, fără „Partajează”, fără „Șterge inventarul” | |
| SET-N2 | Redenumire cu nume gol | „Salvează” dezactivat | |
| EXP-N1 | Export → în dialogul de salvare apasă Înapoi | Nu se creează fișier, fără mesaj | |
| EXP-N2 | Inventar fără itemi → CSV | Fișier doar cu antetul; „Exportat CSV: 0 itemi” | |
| EXP-N3 | Inventar gol → PDF | O pagină goală doar cu antet | |
| EXP-N4 | Item cu nota `=SUM(A1)` → CSV deschis în Excel / Sheets | Apare ca text (`'=SUM(A1)`), nu ca formulă | |
| EXP-N5 📶 | Offline → export | Funcționează (folosește datele locale) | |
| EXP-N6 | Apasă CSV și PDF rapid unul după altul | Butoanele sunt dezactivate cât rulează un export | |

---

## 10. Setări și partajare – membri

### Pozitiv
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| SHARE-P1 | A: email B + „Editare” → Adaugă membru | Snackbar „Inventar partajat cu …”; B apare în Membri cu chip „Editare” | |
| SHARE-P2 | B (alt dispozitiv) | Inventarul apare la B în câteva secunde, cu locații și itemi | |
| SHARE-P3 | A: ⋮ la B → „Schimbă în: Vizualizare” | Chip-ul se schimbă; la B dispar butoanele de editare | |
| SHARE-P4 | A: ⋮ la B → „Elimină din inventar” | B dispare din Membri; inventarul dispare din lista lui B | |
| SHARE-P5 | Partajează din nou cu B, alt rol | B nu se dublează; rolul se actualizează | |
| SHARE-P6 | Email cu majuscule / spații (`  B@Example.com `) | Găsit corect (normalizat) | |
| DEL-P1 | A: „Șterge inventarul” → confirmă | Revii la listă; inventarul dispare la A **și** la membri; dispare și din Firestore | |

### Negativ
| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| SHARE-N1 | Email gol sau fără „@” | „Adaugă membru” dezactivat | |
| SHARE-N2 | Email fără cont QRaft | „Nu există niciun cont QRaft cu emailul …” | |
| SHARE-N3 | Propriul email (al proprietarului) | „Acesta este proprietarul inventarului.” | |
| SHARE-N4 📶 | Offline → partajare | „Nu s-a putut verifica emailul. Este nevoie de conexiune la internet.” | |
| SHARE-N5 | Cont B creat, dar B nu s-a logat niciodată | „Nu există niciun cont…” (emailul se înregistrează la primul login) | |
| SHARE-N6 | Proprietarul în lista Membri | Fără meniul ⋮ (nu poate fi eliminat / schimbat) | |
| DEL-N1 | „Șterge inventarul” → Renunță | Nimic nu se șterge | |

---

## 11. Permisiuni pe roluri (rezumat)

| Acțiune | Proprietar | Editare | Vizualizare |
|---|:-:|:-:|:-:|
| Vede inventarul, locațiile, itemii | ✓ | ✓ | ✓ |
| Scanează | ✓ | ✓ | ✓ |
| Export CSV / PDF | ✓ | ✓ | ✓ |
| Creează / editează / șterge locații și itemi, +/− cantitate | ✓ | ✓ | ✗ |
| Redenumește inventarul | ✓ | ✗ | ✗ |
| Partajează / schimbă roluri / elimină membri | ✓ | ✗ | ✗ |
| Șterge inventarul | ✓ | ✗ | ✗ |

| ID | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|
| ROLE-P1 | Verifică fiecare celulă a tabelului cu B în rolurile Editare și Vizualizare | UI-ul afișează / ascunde acțiunile conform tabelului | |
| ROLE-N1 | B Editare creează un item | Itemul apare la A (regulile Firestore permit) | |
| ROLE-N2 | B trece din Editare în Vizualizare în timp ce are ecranul de detalii deschis | Butoanele de editare dispar fără repornire | |

---

## 12. Sincronizare și offline 📶

| ID | Tip | Pași | Rezultat așteptat | ✓ |
|---|---|---|---|---|
| SYNC-P1 | P | A creează o locație → verifică Firestore Console | Documentul apare în `inventories/{id}/locations` | |
| SYNC-P2 | P | A și B deschid același inventar; A modifică un item | Modificarea apare la B fără refresh | |
| SYNC-P3 | P | Offline: creează locație + item, modifică cantitatea → online | Toate ajung în cloud; „(nesincronizat)” dispare | |
| SYNC-P4 | P | Logout → login cu același cont | Datele sunt descărcate din cloud | |
| SYNC-P5 | P | Instalează aplicația pe al doilea dispozitiv, același cont | Toate inventarele apar | |
| SYNC-N1 | N | Offline: modifică itemul pe A; online: modifică același item pe B; reconectează A | Ultima scriere câștigă; fără crash, fără duplicate | |
| SYNC-N2 | N | A șterge o locație cât timp B o are deschisă | La B locația dispare / ecranul devine gol, fără crash | |
| SYNC-N3 | N | Două dispozitive offline creează câte o locație nouă în același inventar | Pot primi **același cod** (limitare cunoscută); verifică că ambele locații există | |
| SYNC-N4 | N | Omoară aplicația imediat după o modificare offline → repornește online | Modificarea se trimite (persistă în Room cu `pendingSync`) | |

---

## 13. Smoke test rapid (≈ 5 minute, înainte de fiecare livrare)

- [ ] Login cu un cont existent
- [ ] Creează inventar → verifică prefixul
- [ ] Creează locație și sub-locație → coduri consecutive
- [ ] Creează item din rădăcină și din locație
- [ ] +/− cantitate în detalii item
- [ ] Editează item și mută-l în altă locație
- [ ] Scanează QR-ul unei locații și al unui item
- [ ] Export CSV și PDF, deschide ambele fișiere
- [ ] Partajează cu B → B vede inventarul
- [ ] Șterge item, locație, inventar
- [ ] Logcat fără `FATAL EXCEPTION` / `E/AndroidRuntime`

## Probleme cunoscute (la data scrierii)

- Numerele de locație se refolosesc după ștergere (LOC-N3); două dispozitive offline pot genera același cod (SYNC-N3).
- Membrii nu pot părăsi singuri un inventar (doar proprietarul îi poate elimina).
- Erorile Firebase Auth mai rare (în afara celor din §1) apar cu mesajul generic „Autentificarea a eșuat.”
