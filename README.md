# QRaft

Aplicatie Android de inventariere pe baza de coduri QR. Proiect individual pentru laboratorul de Aplicatii Mobile, UPT.

Autor: Rosca Bogdan Mihai

## Descriere

QRaft tine evidenta obiectelor organizate pe locatii (raft, sertar, cutie etc.). Pentru fiecare locatie si fiecare item aplicatia genereaza un numar de inventar si un cod QR care poate fi printat si lipit. Codurile se scaneaza cu camera telefonului, iar la fiecare scanare reusita telefonul vibreaza.

Inventarul este salvat local si sincronizat in cloud, si poate fi partajat cu alti utilizatori.

## Functionalitati

- autentificare cu email si parola
- adaugare, vizualizare, editare si stergere pentru locatii si itemi
- generare automata a numarului de inventar / pozitie
- generare cod QR pentru fiecare locatie si item
- scanare QR cu camera, cu vibratie la scanare reusita
- sincronizare in cloud si functionare offline
- partajarea unui inventar cu alti utilizatori
- export inventar (CSV) si etichete QR (PDF)

## Cerinte proiect

- CRUD: locatii si itemi, salvate in Room si sincronizate in Firestore
- senzori / actuatori: camera (scanare QR) si vibrator (feedback la scanare)
- complexitate: autentificare + ecranele de mai jos

## Ecrane

1. Login / Register
2. Lista de inventare
3. Lista de locatii
4. Detalii locatie (itemi + cod QR)
5. Detalii item
6. Adaugare / editare
7. Scanner
8. Setari si partajare

## Tehnologii

- Kotlin, Jetpack Compose
- MVVM
- Room pentru baza de date locala
- CameraX + ML Kit pentru scanarea codurilor
- ZXing pentru generarea codurilor QR
- Firebase Authentication si Cloud Firestore

## Numerotare

- **locatie**: primele 3 consoane din numele inventarului + 2 grupe de 3 cifre, de exemplu `lbr-000-007` pentru inventarul „Laborator”. Se foloseste primul numar liber din inventar.
- **item**: 4 grupe de 3 cifre, de exemplu `482-907-115-630`, unic in cadrul inventarului.

Prefixul (consoanele) se calculeaza o singura data, la crearea inventarului, si nu se schimba daca inventarul este redenumit. Diacriticele sunt ignorate (`Ș` -> `s`). Daca numele are mai putin de 3 consoane, prefixul se completeaza cu `x`.

Codul QR contine un link de forma `qraft://item/<id>` sau `qraft://location/<id>`, ca aplicatia sa stie ce tip de obiect a fost scanat.

## Model de date

**Location**: id, inventoryId, parentId, code, name, description, updatedAt

**Item**: id, inventoryId, locationId, code, name, category, quantity, notes, updatedAt

**Inventory**: id, name, ownerId, prefix, members (uid + email + rol)

## Sincronizare si partajare

Datele sunt tinute local in Room, astfel incat aplicatia functioneaza si fara internet. Modificarile sunt trimise in Firestore cand exista conexiune.

Structura in Firestore:

```
inventories/{inventoryId}                  name, ownerId, prefix, members[], updatedAt
inventories/{inventoryId}/locations/{id}   parentId, code, name, description, updatedAt
inventories/{inventoryId}/items/{id}       locationId, code, name, category, quantity, notes, updatedAt
```

Functionare (`SyncManager`):

- fiecare rand din Room are un flag `pendingSync`; o modificare locala il seteaza pe `true`, iar confirmarea serverului il readuce la `false`
- cat timp utilizatorul e logat, aplicatia asculta in timp real inventarele in care e membru si le reconciliaza cu Room
- randurile cu `pendingSync = true` nu sunt suprascrise din cloud
- la logout baza locala este golita

Regulile de securitate sunt in `firestore.rules` si se publica cu `npx -y firebase-tools@latest deploy --only firestore:rules` (sau prin copiere in Firebase Console -> Firestore Database -> Rules).

Proprietarul unui inventar poate adauga alti utilizatori dupa email, cu drept de vizualizare sau de editare (ecranul Setari si partajare). Persoana trebuie sa aiba deja cont. Emailurile sunt cautate in colectia `userEmails/{email}`, completata automat la login.

## Export

Din ecranul Setari si partajare al unui inventar (disponibil pentru toti membrii):

- **Itemi (CSV)**: toti itemii, cu cod, nume, categorie, cantitate, codul si calea locatiei (`Garaj / Raft 1`), note si data ultimei modificari. Fisierul e UTF-8 cu BOM, separat prin virgula.
- **Etichete QR (PDF)**: pagini A4 cu 21 de etichete (3 x 7) si linii de taiere, mai intai pentru locatii, apoi pentru itemi. Codurile QR sunt desenate vectorial.

Fisierul se salveaza unde alege utilizatorul (Storage Access Framework), deci nu sunt necesare permisiuni de stocare. Exportul foloseste datele locale si functioneaza si offline.

## Permisiuni

- `CAMERA`: scanare coduri QR
- `VIBRATE`: vibratie la scanare
- `INTERNET`: autentificare si sincronizare

## Rulare

1. Clonare repository
2. Build si rulare din Android Studio pe un telefon fizic

`google-services.json` este inclus in repository si conecteaza aplicatia la proiectul Firebase `qraft-anollex`. Cheia API din el nu este secreta; accesul la date este controlat de regulile din `firestore.rules`.

Pentru a folosi un alt proiect Firebase: se creeaza proiectul cu package name `ro.upt.qraft`, se activeaza Email/Password Authentication si Cloud Firestore, se inlocuieste `app/google-services.json` si se actualizeaza `.firebaserc`.

## Status

- [x] autentificare
- [x] CRUD locatii si itemi (Offline-First cu Room)
- [x] generare coduri si QR
- [x] scanner + vibratie
- [x] sincronizare cloud
- [x] partajare
- [x] export
