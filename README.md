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

Codurile au formatul `PREFIX-TIP-NUMAR`, de exemplu:

- locatie: `LAB-L-0007`
- item: `LAB-I-000123`

Prefixul se alege la crearea inventarului. Codul QR contine un link de forma `qraft://item/<id>` sau `qraft://location/<id>`, ca aplicatia sa stie ce tip de obiect a fost scanat.

## Model de date

**Location**: id, inventoryId, parentId, code, name, description, updatedAt

**Item**: id, inventoryId, locationId, code, name, category, quantity, notes, updatedAt

**Inventory**: id, name, ownerId, members, prefix

## Sincronizare si partajare

Datele sunt tinute local in Room, astfel incat aplicatia functioneaza si fara internet. Modificarile sunt trimise in Firestore cand exista conexiune.

Proprietarul unui inventar poate adauga alti utilizatori dupa email, cu drept de vizualizare sau de editare.

## Permisiuni

- `CAMERA`: scanare coduri QR
- `VIBRATE`: vibratie la scanare
- `INTERNET`: autentificare si sincronizare

## Rulare

1. Clonare repository
2. Creare proiect in Firebase Console, cu package name `ro.upt.qraft`
3. Activare Email/Password Authentication si Cloud Firestore
4. Copiere `google-services.json` in folderul `app/`
5. Build si rulare din Android Studio pe un telefon fizic

`google-services.json` nu este inclus in repository.

## Status

- [x] autentificare
- [x] CRUD locatii si itemi (Offline-First cu Room)
- [ ] generare coduri si QR
- [ ] scanner + vibratie
- [ ] sincronizare cloud
- [ ] partajare
- [ ] export
