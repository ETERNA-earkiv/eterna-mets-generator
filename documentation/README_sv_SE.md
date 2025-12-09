# METS Generator - Användarhandledning

## Översikt

METS Generatorn skapar och uppdaterar METS-filer för Logiska enheter och Representationer.
Detta möjliggör export av fullständiga E-ARK CSIP paket med profilerna SIP, AIP & DIP.

## Så här använder du METS Generatorn

1. **Köra METS Generatorn**: Börja med att välja ut de Logiska enheter du vill skapa METS-filer för via Katalog sidan eller via Sök sidan, tryck därefter på meny-knappen "**⋮**" och välj "Starta ny process". På sidan "Ny process" väljer du jobbet **METS Generator** under "Åtgärder".
2. **Välj IP-profil**: Välj önskad IP-profil för den nya METS-filen: SIP, AIP eller DIP
3. **Välj ifall du vill inkludera 'Ancestors'**: Med checkrutan "Include Ancestor IDs" kan du välja ifall den logiska enhetens plats i arkivet skall inkluderas i den skapade METS-filen
4. **Skapa METS-filen**: Tryck på knappen "SKAPA" i sidomenyn på höger sida för att starta jobbet och skapa METS-filen

### IP Profiler

E-ARK CSIP paket finns tillgängligt i olika profiler för olika ändamål. METS Generatorn stöder profilerna SIP, AIP & DIP.

- **E-ARK SIP**: Submission Information Package eller Överföringspaket används för paket som överförs från verksamheten till arkivet
- **E-ARK AIP**: Archival Information Package eller Arkivpaket representerar information så som den är lagrad hos arkivet
- **E-ARK DIP**: Dissemination Information Package eller Utlämningspaket är paket avsedda för export till annan part

Innan export ur arkivet bör man generera medföljande METS-filer för de paket man ämnar att exportera, vanligtvis med profilen **DIP** om inte ändamålet med exporten är direkt återimportering i annat arkiv, i vilket fall **SIP** kan underlätta processen för återimportering.

### Include Ancestor IDs

Med checkrutan "Include Ancestor IDs" kan du välja ifall den logiska enhetens plats i arkivet skall inkluderas i den skapade METS-filen. Används denna funktion kommer paketet placeras på samma plats i arkivet som originalet vid ny inleverans. Detta är användbart ifall en hel struktur skall exporteras då strukturen bevaras i paketinformationen.

### Vad hanterar METS Generatorn?

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

### Begränsningar

METS Generatorn hanterar i dagsläget inte:
- **Shallow IPs**: Paket där bevarandeinformation inte är lagrad i paketet utan paketet pekar på en extern plats där informationen är lagrad
- Andra filer bifogade i originalpaketet än de angivna ovan, jobbet tar endast med de informationsmängder definierade i E-ARK CSIP, SIP, AIP & DIP standarderna

### Att tänka på

Ifall alternativet "Include Ancestor IDs" används så inkluderas platsen paketet har i arkivet som "Ancestor"-information.
Detta innebär att när paketet med den nya METS-filen sedan läses in så kommer den i standardläget alltid återläsas till samma plats i arkivet som den exporterades från. Finns inte denna plats i arkivet som paketet läses in i så kommer ETERNA försöka återskapa hierarkin dit paketet läses in.

Om paketet var lagrad på följande plats i arkivet: `/ Nivå1 / Nivå2 / PaketAttExportera` så kommer den läsa in det till samma plats i arkivet och placeras bredvid originalpaketet.

Saknas `/ Nivå1 / Nivå2` så kommer den skapa tomma Logiska Enheter för att representera Nivå1 & Nivå2, dessa kommer dock inte ha något namn eller metadata då endast ID är inkluderat i "Ancestor"-informationen.

I arkivet kommer det se ut som följande: `/ 👻 ghost / 👻 ghost / PaketAttExportera`.

Försöker man läsa in paketet till en annan plats i ursprungsarkivet, t.ex: `/ AnnanEnhet /` som inte innehåller original "Nivå1" & "Nivå2" så kommer den även återskapa de logiska enheterna på samma sätt och paketet kommer då läsas in till: `/ AnnanEnhet / 👻 ghost / 👻 ghost / PaketAttExportera`.

För att hantera detta vid inleverans kan man kryssa i checkrutan "Force parent node" vilket säger till inleveransen att ignorera paketerad "Ancestor"-information och paketet importeras då till antingen roten av katalogen eller till vald plats om man använt funktionen: Välj Parent node.