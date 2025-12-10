# METS Generator - Användarhandledning

## Översikt

METS Generatorn skapar och uppdaterar METS-filer för Logiska enheter och Representationer.
Detta möjliggör export av fullständiga E-ARK CSIP paket med profilerna SIP, AIP & DIP.

---

## Så här använder du METS Generatorn

1. **Köra METS Generatorn**: Börja med att välja ut de Logiska enheter du vill skapa METS-filer för via Katalog sidan eller via Sök sidan, tryck därefter på meny-knappen "**⋮**" och välj "Starta ny process". På sidan "Ny process" väljer du jobbet **METS Generator** under "Åtgärder".
2. **Välj IP-profil**: Välj önskad IP-profil för den nya METS-filen: SIP, AIP eller DIP
3. **Välj ifall du vill inkludera 'Ancestors'**: Med checkrutan "Include Ancestor IDs" kan du välja ifall den logiska enhetens plats i arkivet skall inkluderas i den skapade METS-filen
4. **Skapa METS-filen**: Tryck på knappen "SKAPA" i sidomenyn på höger sida för att starta jobbet och skapa METS-filen

---

## IP Profiler

E-ARK CSIP paket finns tillgängligt i olika profiler för olika ändamål. METS Generatorn stöder profilerna SIP, AIP & DIP.

- **E-ARK SIP**: Submission Information Package eller Överföringspaket används för paket som överförs från verksamheten till arkivet
- **E-ARK AIP**: Archival Information Package eller Arkivpaket representerar information så som den är lagrad hos arkivet
- **E-ARK DIP**: Dissemination Information Package eller Utlämningspaket är paket avsedda för export till annan part

Innan export ur arkivet bör man generera medföljande METS-filer för de paket man ämnar att exportera, vanligtvis med profilen **DIP** om inte ändamålet med exporten är direkt återimportering i annat arkiv, i vilket fall **SIP** kan underlätta processen för återimportering.

---

## Include Ancestor IDs

Med checkrutan "Include Ancestor IDs" kan du välja ifall den logiska enhetens plats i arkivet skall inkluderas i den skapade METS-filen. Används denna funktion kommer paketet placeras på samma plats i arkivet som originalet vid ny inleverans. Detta är användbart ifall en hel struktur skall exporteras då strukturen bevaras i paketinformationen.

---

## Vad hanterar METS Generatorn?

METS Generatorn inkluderar följande paketinformation i METS-filerna:
- AIP-nivå:
    - Skapat datum
    - Skapare
    - Plats i arkivet (Ancestors)
    - Beskrivandemetadata
    - Bevarandemetadata
    - Teknisk metadata
    - Källmetadata
    - Rättighetsmetadata
    - Annan metadata
    - Scheman
    - Dokumentation
- Representation-nivå:
    - Bevarandedata
    - Beskrivandemetadata
    - Bevarandemetadata
    - Teknisk metadata
    - Källmetadata
    - Rättighetsmetadata
    - Annan metadata
    - Scheman
    - Dokumentation

---

## Begränsningar

METS Generatorn hanterar i dagsläget inte:
- **Shallow IPs**: Paket där bevarandeinformation inte är lagrad i paketet utan paketet pekar på en extern plats där informationen är lagrad

- **Representationstyp**: Representationstyp är i METS-filen uppdelad i två fält: `TYPE` och `CONTENTINFORMATIONTYPE` (och eventuella `OTHERTYPE` & `OTHERCONTENTINFORMATIONTYPE` ifall de huvudsakligen fälten har värdet `OTHER`).
  När commons-ip användes som commandline-verktyg för paketering innan v2.11.0 lagrades vald representationstyp i fältet `TYPE` (och ev. `OTHERTYPE`) men vid inleverans läste den värdet från `CONTENTINFORMATIONTYPE` (och ev. `OTHERCONTENTINFORMATIONTYPE`).
  Detta innebär att paket skapade med de versionerna kan ha sparat fel representationstyp vid inleverans.
  I nya versioner av commons-ip kan man sätta de båda värdena oberoende utav varandra men `CONTENTINFORMATIONTYPE` är fortfarande det fält som kommer läsas in vid inleverans, det andra värdet går förlorat.
  Detta innebär att vid återimport av paketen med de nya METS-filerna så kan original-representationstypen saknas eller vara felaktigt.
  I framtida version av ETERNA ämnar vi utöka datamodellerna för AIP & Representation med fältet `contentInformationType` för att kunna bevara båda typer så som de är angivna i METS-filen.

- **Representationsstatus**: Representationers status bevaras i dagsläget inte i den nya METS-filen

- Andra filer bifogade i originalpaketet än de angivna ovan, jobbet tar endast med de informationsmängder definierade i E-ARK CSIP, SIP, AIP & DIP standarderna

---

## Att tänka på

### Ancestor IDs

Ifall alternativet "Include Ancestor IDs" används så inkluderas platsen paketet har i arkivet som "Ancestor"-information.  
Detta innebär att när paketet med den nya METS-filen sedan läses in så kommer den i standardläget alltid återläsas till samma plats i arkivet som den exporterades från. Finns inte denna plats i arkivet som paketet läses in i så kommer ETERNA försöka återskapa hierarkin dit paketet läses in.

Om paketet var lagrad på följande plats i arkivet: `/ Nivå1 / Nivå2 / PaketAttExportera` så kommer den läsa in det till samma plats i arkivet och placeras bredvid originalpaketet.

Saknas `/ Nivå1 / Nivå2` så kommer den skapa tomma Logiska Enheter för att representera Nivå1 & Nivå2, dessa kommer dock inte ha något namn eller metadata då endast ID är inkluderat i "Ancestor"-informationen.

I arkivet kommer det se ut som följande: `/ 👻 ghost / 👻 ghost / PaketAttExportera`.

Försöker man läsa in paketet till en annan plats i ursprungsarkivet, t.ex: `/ AnnanEnhet /` som inte innehåller original "Nivå1" & "Nivå2" så kommer den även återskapa de logiska enheterna på samma sätt och paketet kommer då läsas in till: `/ AnnanEnhet / 👻 ghost / 👻 ghost / PaketAttExportera`.

För att hantera detta vid inleverans kan man kryssa i checkrutan "Force parent node" vilket säger till inleveransen att ignorera paketerad "Ancestor"-information och paketet importeras då till antingen roten av katalogen eller till vald plats om man använt funktionen: Välj Parent node.

---

### Preservation Metadata

METS Generatorn skapar inte några preservation events eller PREMIS:EVENT då vi gjort tolkningen att PREMIS:EVENT beskriver händelser kring innehållet av ett paket, inte förändringar av paketet självt.

Se: [E-ARK CSIP | Common Specification for Information Packages — 5.4. Use of PREMIS](https://earkcsip.dilcis.eu/#54-use-of-premis)
> 5.4. Use of PREMIS
>
> The CSIP recommends and advocates the use of the PREservation Metadata Implementation Strategies (PREMIS, [https://www.loc.gov/standards/premis/](https://www.loc.gov/standards/premis/)) metadata standard for recording preservation and technical metadata about digital objects contained within CSIP Information Packages. The use of PREMIS is described in the “E-ARK Common Specification for Preservation Metadata using PREMIS” (CS PREMIS) found at [https://citspremis.dilcis.eu/specification/](https://citspremis.dilcis.eu/specification/) .

Ifall förändringar av METS-filen hade inkluderats som PREMIS:EVENTs så hade detta skapat ett cirkulärt beroende där förändringen av METS-filen leder till en ny PREMIS:EVENT fil som i sin tur innebär att METS-filen behöver förändras för att inkludera den nya filen vilket då hade krävt en ytterligare PREMIS:EVENT fil o.s.v.

---

### Kompabilitet med paket skapade i RODA-In

När paket skapas i RODA-In med standardkonfiguration så får EAD-metadata filnamn som inte matchar RODA / ETERNAs förväntade metadata-namnstandard, specifikt: ead2002.xml & ead3.xml beroende på vilket metadataformat som används. RODA & ETERNA förväntar sig filnamnen: ead_2002.xml & ead_3.xml för att förstå att metadata-typen är EAD och metadata-versionen är "2002" eller "3".

När en ny METS-fil skall skapas med METS Generator utifrån en logisk enhet inläst från ett sådant paket saknas informationen om typ & version i ETERNA och metadatatypen tolkas istället som "ead2002" utan version eller "ead3" utan version. I METS-filen reflekteras detta i attributen MDTYPE="OTHER" MDOTHERTYPE="ead2002" och en avsaknad av MDVERSION. När detta paketet sedan återimporteras så finns det ingen metadata utav typ "ead2002" eller "ead3" i ETERNA så metadatan tolkas därför som en okänd XML-fil.

Detta felet är känt i RODA-In och är lyft i följande ärende hos Keep Solution:
[https://github.com/keeps/roda-in/issues/440](https://github.com/keeps/roda-in/issues/440)