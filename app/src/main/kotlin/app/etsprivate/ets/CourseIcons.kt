package app.etsprivate.ets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.DesignServices
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Foundation
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SolarPower
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.Normalizer

object CourseIcons {
    private val icons: Map<String, ImageVector> by lazy {
        mapOf(
            "internship" to Icons.Outlined.Work,
            "project" to Icons.Outlined.Handyman,
            "communication" to Icons.Outlined.RecordVoiceOver,
            "safety" to Icons.Outlined.HealthAndSafety,
            "security" to Icons.Outlined.Security,
            "integrity" to Icons.Outlined.Gavel,
            "ai" to Icons.Outlined.Psychology,
            "database" to Icons.Outlined.Storage,
            "data" to Icons.Outlined.Insights,
            "renewable" to Icons.Outlined.SolarPower,
            "power" to Icons.Outlined.Bolt,
            "aero" to Icons.Outlined.Flight,
            "fluid" to Icons.Outlined.WaterDrop,
            "telecom" to Icons.Outlined.CellTower,
            "network" to Icons.Outlined.Lan,
            "signal" to Icons.Outlined.GraphicEq,
            "cloud" to Icons.Outlined.Cloud,
            "robot" to Icons.Outlined.PrecisionManufacturing,
            "game" to Icons.Outlined.SportsEsports,
            "web" to Icons.Outlined.Language,
            "embedded" to Icons.Outlined.DeveloperBoard,
            "software" to Icons.Outlined.Code,
            "electronics" to Icons.Outlined.Memory,
            "auto" to Icons.Outlined.DirectionsCar,
            "thermo" to Icons.Outlined.Thermostat,
            "env" to Icons.Outlined.Eco,
            "geo" to Icons.Outlined.Terrain,
            "structure" to Icons.Outlined.Foundation,
            "biomed" to Icons.Outlined.MonitorHeart,
            "bim" to Icons.Outlined.Architecture,
            "construction" to Icons.Outlined.Construction,
            "urban" to Icons.Outlined.Map,
            "materials" to Icons.Outlined.Category,
            "manufacturing" to Icons.Outlined.Factory,
            "finance" to Icons.Outlined.AttachMoney,
            "quality" to Icons.Outlined.Verified,
            "mechanics" to Icons.Outlined.Settings,
            "logistics" to Icons.Outlined.LocalShipping,
            "ergonomics" to Icons.Outlined.Accessibility,
            "design" to Icons.Outlined.DesignServices,
            "entrepreneur" to Icons.Outlined.RocketLaunch,
            "optimization" to Icons.Outlined.Hub,
            "stats" to Icons.Outlined.QueryStats,
            "math" to Icons.Outlined.Functions,
            "physics" to Icons.Outlined.Science,
            "management" to Icons.Outlined.ManageAccounts,
            "research" to Icons.AutoMirrored.Outlined.MenuBook,
        )
    }

    private val rules: List<Pair<String, List<String>>> = listOf(
        "internship" to listOf("carriere", "stage", "stages", "alternance", "cooperatif", "insertion professionnelle"),
        "project" to listOf("projet de fin", "projets speciaux", "projet multidisciplinaire", "projet integrateur", "projet synthese", "activite preparatoire", "projet d'ingenierie", "projet en", "projets en"),
        "communication" to listOf("redaction", "methodes de communication", "communication technique", "communication en", "communication ecrite", "communication orale", "anglais", "francais", "langue"),
        "safety" to listOf("sante et securite", "securite au travail", "securite du travail", "securite en milieu", "securite, facteurs", "securite des systemes electriques", "prevention", "sst", "sante et de la securite", "sante, securite", "securite et sante"),
        "security" to listOf("securite informatique", "securite des logiciels", "securite logicielle", "securite des reseaux", "securite des systemes d'information", "securite des ti", "cyber", "cryptograph", "securite reseau", "renseignements personnels", "vie privee"),
        "integrity" to listOf("integrite", "ethique", "droit", "juridique", "legal", "propriete intellectuelle", "deontolog"),
        "ai" to listOf("intelligence artificielle", "apprentissage machine", "apprentissage automatique", "apprentissage profond", "reseaux de neurones", "vision par ordinateur", "machine learning", "biometri", "connected objects"),
        "database" to listOf("base de donnees", "bases de donnees", "sql", "entrepots de donnees"),
        "data" to listOf("donnees", "big data", "megadonnees", "analytique", "forage", "exploration de donnees", "visualisation", "intelligence d'affaires"),
        "renewable" to listOf("renouvelable", "renouvelables", "solaire", "solaires", "eolien", "eolienne", "hydrogene", "batterie", "batteries", "photovoltai", "stockage d'energie"),
        "power" to listOf("electrique", "electriques", "electricite", "electrotechnique", "puissance", "energie", "energies", "hydroelectri", "machines electriques", "reseaux electriques", "reseau electrique"),
        "aero" to listOf("aero", "aeronautique", "aerospatial", "avion", "avions", "aeronef", "aeronefs", "spatial", "spatiaux", "turbomachine", "propulsion", "helicoptere", "avioni", "en vol"),
        "fluid" to listOf("fluide", "fluides", "hydraulique", "hydrologi", "eau", "eaux", "aqueduc", "hydrodynamique", "remblai", "assainissement"),
        "telecom" to listOf("telecommunication", "telecom", "sans fil", "antenne", "antennes", "radiofrequence", "hyperfrequence", "communications numeriques", "communication numerique", "systemes de communication", "optique", "optiques", "laser", "telephonie", "micro-ondes"),
        "network" to listOf("reseau", "reseaux", "reseautique", "reseautage", "internet des objets", "objets connectes", "ido", "iot", "protocole"),
        "signal" to listOf("signal", "signaux", "audio", "image", "images", "ondelettes", "traitement d", "audiovisuel"),
        "cloud" to listOf("infonuagique", "nuage", "cloud", "distribue", "distribues", "distribuee", "parallele", "paralleles", "teleinformatique"),
        "robot" to listOf("robot", "robotique", "automatisation", "automate", "mecatronique", "commande", "asservissement"),
        "game" to listOf("jeu", "jeux", "ludique", "realite virtuelle", "realite augmentee", "multimedia", "physique des jeux"),
        "web" to listOf("web", "mobile", "mobiles", "application mobile", "commerce electronique", "developpement internet", "services internet"),
        "embedded" to listOf("embarque", "embarques", "microcontroleur", "microprocesseur", "microprocesseurs", "architecture des ordinateurs", "ordinateurs", "fpga", "systemes numeriques", "vlsi", "circuits integres"),
        "software" to listOf("logiciel", "logiciels", "programmation", "algorithm", "oriente", "genie logiciel", "informatique", "langage", "compilat", "structures de donnees", "test", "tests", "developpement logiciel", "systemes d'exploitation", "logique"),
        "electronics" to listOf("electronique", "circuit", "circuits", "semi-conducteur", "composants", "instrumentation", "capteur", "capteurs", "microelectronique", "mesures informatisees", "mesures"),
        "auto" to listOf("automobile", "vehicule", "vehicules", "motorisation"),
        "thermo" to listOf("thermo", "thermique", "chaleur", "climatisation", "refrigeration", "cvca", "chauffage", "ventilation"),
        "env" to listOf("environnement", "environnemental", "environnementaux", "environnementales", "durable", "durables", "developpement durable", "ecologi", "pollution", "dechets", "climat", "climatique", "carbone", "cycle de vie"),
        "geo" to listOf("geotechni", "sol", "sols", "geologie", "fondation", "fondations", "roc", "geomatique", "excavation", "excavations", "souterrain", "mines"),
        "structure" to listOf("structure", "structures", "beton", "acier", "bois", "charpente", "pont", "ponts", "resistance des materiaux", "statique", "sismique", "assemblages"),
        "biomed" to listOf("biomedical", "biomedicale", "biomecanique", "medical", "medicale", "sante", "hopital", "clinique", "chirurgica", "biomateriau", "orthese", "protheses", "aides techniques"),
        "bim" to listOf("bim", "dessin", "modelisation des donnees du batiment", "plans et devis"),
        "construction" to listOf("construction", "chantier", "batiment", "batiments", "genie civil", "infrastructure", "infrastructures", "route", "routes", "chaussee", "chaussees", "municipaux", "estimation et soumissions", "travaux"),
        "urban" to listOf("urbain", "urbaine", "transport", "transports", "circulation", "mobilite durable", "amenagement", "territoire"),
        "materials" to listOf("materiaux", "materiau", "metallurg", "polymere", "polymeres", "composite", "composites", "corrosion", "soudage", "chimie", "alliages", "bitumineux"),
        "manufacturing" to listOf("fabrication", "procede", "procedes", "usinage", "production", "manufactur", "fonderie", "mise en forme", "outillage"),
        "finance" to listOf("economi", "finance", "financier", "financiere", "financement", "comptab", "couts", "cout", "investissement", "rentabilite"),
        "quality" to listOf("qualite", "fiabilite", "metrologie", "inspection", "six sigma", "lean", "amelioration continue", "tolerancement", "controle non destructif", "maintenance"),
        "mechanics" to listOf("mecanique", "mecaniques", "dynamique", "vibration", "vibratoire", "cinematique", "machines", "mecanismes", "element fini", "elements finis", "conception mecanique", "tribologie", "fatigue", "cao", "modelisation solide"),
        "logistics" to listOf("logistique", "chaine d'approvisionnement", "approvisionnement", "entreposage", "distribution", "inventaire", "stocks"),
        "ergonomics" to listOf("ergonom", "facteurs humains", "interaction humain", "utilisabilite", "ux", "interface", "interfaces", "inclusif"),
        "design" to listOf("design", "conception de produits", "ideation", "creativite", "creative", "innovation", "architecture de l'information"),
        "entrepreneur" to listOf("entrepreneur", "entrepreneuriat", "entreprise", "entreprendre", "demarrage", "startup", "affaires", "commercialisation", "marketing"),
        "optimization" to listOf("recherche operationnelle", "optimisation", "simulation", "modelisation", "systemes industriels", "ordonnancement", "planification"),
        "stats" to listOf("statistique", "statistiques", "probabilit", "analyse de donnees", "methodes quantitatives", "plan d'experience"),
        "math" to listOf("mathemat", "calcul", "algebre", "equations", "differentiel", "integral", "geometrie", "vecteur", "matric", "methodes numeriques", "analyse numerique"),
        "physics" to listOf("physique", "electromagnetisme", "electromagnetiques", "ondes", "biologie", "experimentale"),
        "management" to listOf("gestion", "management", "gestionnaire", "leadership", "organisation", "ressources humaines", "strategie", "gouvernance", "projet", "projets", "travail"),
        "research" to listOf("seminaire", "lecture", "recherche", "memoire", "these", "essai", "methodologie", "methodologies", "etude de cas", "histoire", "readings", "thesis", "lectures dirigees"),
    )

    private val prefixes: Map<String, String> = mapOf(
        "ERG" to "ergonomics", "GSY" to "optimization", "SCE" to "physics", "ETH" to "integrity", "MGE" to "management", "LOG" to "software",
        "GTI" to "software", "MGL" to "software", "INF" to "software", "ELE" to "electronics", "GES" to "management", "MEC" to "mechanics",
        "CTN" to "construction", "SYS" to "management", "STA" to "internship", "MGC" to "construction", "GPA" to "manufacturing", "MTI" to "software",
        "DSN" to "design", "GOL" to "logistics", "GTS" to "biomed", "TCH" to "research", "ENR" to "renewable", "IND" to "manufacturing",
        "MGA" to "aero", "AER" to "aero", "GIU" to "urban", "MAT" to "math", "DGN" to "research", "ENV" to "env",
        "BIM" to "bim", "GCI" to "construction", "MTR" to "manufacturing", "ENT" to "entrepreneur", "TEL" to "telecom", "QUA" to "quality",
        "SST" to "safety", "ATE" to "integrity", "COM" to "communication", "MGR" to "management", "PGM" to "management", "PRE" to "safety",
        "PHY" to "physics", "IMM" to "construction", "SSC" to "research", "DGA" to "design", "EEC" to "electronics", "GIA" to "ai",
        "GPO" to "manufacturing", "ENM" to "env", "DST" to "research", "CHM" to "physics",
    )

    private const val TABLE = "AER140:aero,AER200:aero,AER300:aero,AER400:aero,AER450:aero,AER500:aero,AER600:aero,AER610:aero,AER627:aero,AER650:aero,AER660:aero,AER670:aero,AER671:aero,AER710:aero,AER720:aero,AER725:aero,AER729:aero,AER730:aero,AER732:aero,AER735:aero,AER740:aero,AER750:aero,AER760:aero,AER770:aero,AER780:aero,AER785:aero,AER791:project,AER795:aero,ATE029:mechanics,ATE030:thermo,ATE075:management,ATE085:integrity,ATE100:integrity,ATE150:safety,ATE800:integrity,ATE800E:integrity,ATE850:safety,ATE850E:integrity,BIM810:construction,BIM820:construction,BIM830:data,BIM840:data,BIM845:bim,BIM850:bim,BIM860:construction,BIM870:bim,BIM880:construction,BIM895:env,BIM921:bim,CHM015:materials,CHM130:thermo,CHM131:materials,COM120:communication,COM127:communication,COM130:communication,COM140:communication,COM210:communication,COM400:communication,COM410:communication,COM800:communication,CTN100:construction,CTN112:construction,CTN114:construction,CTN210:construction,CTN248:structure,CTN318:structure,CTN320:construction,CTN336:fluid,CTN346:fluid,CTN404:materials,CTN408:structure,CTN446:fluid,CTN456:fluid,CTN466:env,CTN504:geo,CTN508:structure,CTN603:thermo,CTN609:integrity,CTN610:construction,CTN626:construction,CTN647:construction,CTN701:construction,CTN702:construction,CTN703:construction,CTN705:construction,CTN731:construction,CTN732:construction,CTN733:power,CTN734:thermo,CTN735:env,CTN736:env,CTN737:construction,CTN741:geo,CTN743:geo,CTN744:geo,CTN752:fluid,CTN760:env,CTN761:construction,CTN763:construction,CTN764:env,CTN765:management,CTN766:env,CTN768:env,CTN769:construction,CTN771:construction,CTN772:construction,CTN774:construction,CTN781:structure,CTN783:structure,CTN784:structure,CTN785:structure,CTN791:project,CTN793:project,DGN810:research,DGN820:design,DGN830:design,DGN840:design,DGN850:design,DGN855:management,DGN860:design,DGN870:design,DGN880:design,DGN885:design,DGN910:design,DGN920:design,DGN935:design,DSN110:research,DSN120:ergonomics,DSN140:ergonomics,DSN170:design,DSN171:design,DSN210:research,DSN220:design,DSN230:ergonomics,DSN240:design,DSN250:ergonomics,DSN260:ergonomics,DSN270:design,DSN330:project,DSN340:design,DSN360:design,DSN370:ergonomics,DSN380:ai,DSN420:data,DSN450:ergonomics,DSN460:electronics,DSN470:game,DSN480:ergonomics,DSN510:data,DSN530:env,DSN570:design,DSN580:ergonomics,DSN610:project,DSN620:design,DSN630:ergonomics,DSN640:design,DSN650:ergonomics,DSN660:ergonomics,DSN670:robot,DSN680:game,DSN795:design,DST202:research,DST404:research,DST801:research,DST802:management,EEC110:construction,EEC202:construction,EEC303:power,EEC404:management,EEC550:construction,EEC707:construction,ELE104:power,ELE105:electronics,ELE140:embedded,ELE200:electronics,ELE216:power,ELE265:signal,ELE275:electronics,ELE312:physics,ELE344:electronics,ELE355:power,ELE400:power,ELE413:physics,ELE431:electronics,ELE440:software,ELE452:aero,ELE462:telecom,ELE474:robot,ELE543:embedded,ELE550:power,ELE641:aero,ELE649:network,ELE652:power,ELE653:power,ELE654:power,ELE656:power,ELE664:telecom,ELE666:signal,ELE667:telecom,ELE672:electronics,ELE673:electronics,ELE674:embedded,ELE675:electronics,ELE676:manufacturing,ELE682:embedded,ELE692:security,ELE704:robot,ELE716:software,ELE732:cloud,ELE735:math,ELE736:cloud,ELE739:embedded,ELE747:signal,ELE749:embedded,ELE752:power,ELE756:network,ELE757:power,ELE758:power,ELE759:power,ELE760:power,ELE767:ai,ELE771:electronics,ELE772:telecom,ELE773:robot,ELE777:mechanics,ELE784:embedded,ELE785:telecom,ELE788:telecom,ELE791:project,ELE793:telecom,ELE794:project,ELE795:project,ELE796:cloud,ELE797:electronics,ELE798:telecom,ELE889:renewable,ENM800:manufacturing,ENM810:env,ENM820:network,ENM820E:ai,ENM830:entrepreneur,ENM840:env,ENM850:env,ENM860:data,ENM865:power,ENM880:entrepreneur,ENR801:renewable,ENR811:renewable,ENR815:renewable,ENR830:power,ENR835:renewable,ENR840:power,ENR845:renewable,ENR848:renewable,ENR850:power,ENR855:power,ENR859:renewable,ENR860:urban,ENR865:power,ENR880:renewable,ENR881:renewable,ENR889:renewable,ENT201:finance,ENT202:entrepreneur,ENT301:project,ENT302:project,ENT303:project,ENT304:project,ENT601:entrepreneur,ENT800:entrepreneur,ENT805:entrepreneur,ENT811:finance,ENV802:env,ENV803:fluid,ENV805:env,ENV810:env,ENV820:env,ENV825:manufacturing,ENV830:env,ENV835:urban,ENV840:env,ENV850:env,ENV867:env,ENV881:env,ENV882:env,ERG801:ergonomics,ETH300:integrity,ETH310:integrity,ETH440:integrity,ETH610:integrity,GCI101:software,GCI102:software,GCI105:construction,GCI312:safety,GCI320:construction,GCI350:construction,GCI411:construction,GCI420:construction,GCI425:construction,GCI500:construction,GCI800:bim,GES800:management,GES802:management,GES804:management,GES808:design,GES809:management,GES811:management,GES815:finance,GES816:finance,GES817:management,GES818:finance,GES819:finance,GES821:management,GES822:entrepreneur,GES823:finance,GES824:ai,GES825:integrity,GES826:integrity,GES827:safety,GES828:integrity,GES830:finance,GES835:design,GES836:entrepreneur,GES837:design,GES838:design,GES839:design,GES840:integrity,GES845:management,GES846:entrepreneur,GES850:management,GES851:management,GES852:ai,GES855:design,GES856:ai,GES860:design,GES861:design,GES862:design,GES863:finance,GES864:design,GES865:design,GES866:entrepreneur,GES867:design,GES868:design,GES870:management,GES871:finance,GES872:integrity,GES873:env,GES874:management,GES875:management,GES880:management,GES881:design,GES884:integrity,GES885:management,GES886:management,GES892:finance,GES893:finance,GES895:finance,GIA400:finance,GIA410:finance,GIA450:optimization,GIA501:management,GIA602:safety,GIU801:urban,GIU802:construction,GIU803:urban,GIU804:management,GIU805:integrity,GIU806:urban,GIU807:urban,GIU808:urban,GIU809:fluid,GIU810:fluid,GIU811:network,GIU812:construction,GIU813:construction,GIU880:urban,GOL111:logistics,GOL201:logistics,GOL301:urban,GOL401:logistics,GOL451:logistics,GOL460:logistics,GOL465:logistics,GOL471:logistics,GOL491:data,GOL500:logistics,GOL504:logistics,GOL508:logistics,GOL509:logistics,GOL514:logistics,GOL605:logistics,GOL611:logistics,GOL616:logistics,GOL651:logistics,GOL661:logistics,GOL670:logistics,GOL676:logistics,GOL681:logistics,GOL690:logistics,GOL701:logistics,GOL711:logistics,GOL717:logistics,GOL720:logistics,GOL726:logistics,GOL740:logistics,GOL791:logistics,GOL793:project,GPA142:robot,GPA205:manufacturing,GPA210:manufacturing,GPA220:power,GPA305:structure,GPA325:electronics,GPA434:software,GPA445:manufacturing,GPA535:manufacturing,GPA546:software,GPA551:manufacturing,GPA659:software,GPA663:manufacturing,GPA664:manufacturing,GPA667:electronics,GPA668:electronics,GPA671:ai,GPA675:data,GPA678:math,GPA685:database,GPA710:ai,GPA725:manufacturing,GPA741:aero,GPA745:aero,GPA755:manufacturing,GPA764:manufacturing,GPA767:software,GPA771:robot,GPA772:manufacturing,GPA778:robot,GPA782:fluid,GPA783:manufacturing,GPA786:robot,GPA788:network,GPA791:project,GPA793:project,GPE450:management,GPO222:manufacturing,GPO232:optimization,GPO241:robot,GPO602:env,GPO661:quality,GSY400:stats,GSY500:manufacturing,GSY792:project,GSY793:project,GTI100:network,GTI210:software,GTI311:signal,GTI320:software,GTI350:ergonomics,GTI411:software,GTI510:quality,GTI515:entrepreneur,GTI525:web,GTI611:network,GTI619:software,GTI650:software,GTI660:database,GTI700:network,GTI710:web,GTI719:security,GTI720:security,GTI723:software,GTI727:management,GTI730:network,GTI745:ergonomics,GTI750:software,GTI755:ai,GTI771:ai,GTI778:cloud,GTI780:software,GTI785:web,GTI791:software,GTI795:project,GTS501:biomed,GTS502:biomed,GTS503:biomed,GTS504:biomed,GTS601:biomed,GTS602:biomed,GTS610:signal,GTS615:electronics,GTS620:biomed,GTS640:electronics,GTS802:biomed,GTS813:biomed,GTS814:biomed,GTS815:biomed,GTS816:biomed,GTS831:signal,GTS840:biomed,GTS845:biomed,GTS850:biomed,GTS860:ai,GTS880:biomed,GTS881:biomed,IMM100:construction,IMM105:construction,IMM110:construction,IMM125:management,IMM130:safety,IND105:embedded,IND220:web,IND235:software,IND250:cloud,IND280:software,IND300:manufacturing,IND500:database,IND520:network,IND540:manufacturing,IND560:project,IND580:software,IND590:cloud,IND780:cloud,IND791:project,IND795:project,INF111:software,INF130:embedded,INF136:software,INF147:software,INF155:software,INF270:web,ING150:structure,ING160:fluid,ING500:env,ING800:research,LOG100:network,LOG121:software,LOG200:data,LOG210:software,LOG240:software,LOG320:data,LOG410:software,LOG430:software,LOG450:web,LOG460:security,LOG530:software,LOG550:software,LOG635:software,LOG645:cloud,LOG660:database,LOG675:software,LOG680:software,LOG710:software,LOG721:software,LOG725:game,LOG736:cloud,LOG750:software,LOG780:software,LOG791:project,LOG795:project,LRC105:research,LRC110:management,LRC600:management,MAT001:math,MAT015:math,MAT144:math,MAT145:math,MAT165:math,MAT210:software,MAT215:software,MAT265:math,MAT321:software,MAT350:stats,MAT380:math,MAT472:math,MAT802:power,MAT805:math,MEC029:manufacturing,MEC111:structure,MEC129:mechanics,MEC200:materials,MEC222:mechanics,MEC240:thermo,MEC300:manufacturing,MEC329:structure,MEC335:fluid,MEC402:manufacturing,MEC423:mechanics,MEC525:structure,MEC528:mechanics,MEC532:thermo,MEC546:power,MEC555:mechanics,MEC556:mechanics,MEC558:fluid,MEC592:mechanics,MEC602:quality,MEC619:materials,MEC625:materials,MEC627:manufacturing,MEC628:fluid,MEC630:thermo,MEC634:materials,MEC636:mechanics,MEC652:manufacturing,MEC664:manufacturing,MEC702:quality,MEC723:manufacturing,MEC727:mechanics,MEC729:mechanics,MEC730:thermo,MEC732:thermo,MEC733:power,MEC734:thermo,MEC735:construction,MEC737:mechanics,MEC738:structure,MEC743:electronics,MEC744:robot,MEC745:robot,MEC754:manufacturing,MEC755:management,MEC757:mechanics,MEC758:aero,MEC761:quality,MEC781:manufacturing,MEC785:manufacturing,MEC786:materials,MEC788:robot,MEC791:project,MEC794:project,MEC795:mechanics,MGA800:aero,MGA802:aero,MGA803:aero,MGA804:aero,MGA805:software,MGA807:aero,MGA810:mechanics,MGA820:aero,MGA825:aero,MGA852:aero,MGA855:aero,MGA856:aero,MGA900:research,MGA902:internship,MGA961:aero,MGC800:optimization,MGC801:construction,MGC802:data,MGC803:construction,MGC805:construction,MGC814:construction,MGC817:construction,MGC818:construction,MGC820:construction,MGC821:construction,MGC822:env,MGC823:management,MGC824:construction,MGC825:construction,MGC826:structure,MGC830:construction,MGC835:construction,MGC837:materials,MGC840:construction,MGC842:structure,MGC843:geo,MGC844:geo,MGC852:management,MGC853:construction,MGC854:construction,MGC856:fluid,MGC857:construction,MGC859:fluid,MGC861:construction,MGC862:construction,MGC863:env,MGC866:fluid,MGC867:construction,MGC868:power,MGC870:construction,MGC921:construction,MGC922:construction,MGL800:project,MGL801:software,MGL802:software,MGL804:software,MGL805:software,MGL825:network,MGL835:ergonomics,MGL842:software,MGL843:software,MGL844:software,MGL845:software,MGL846:software,MGL848:software,MGL849:software,MGL850:software,MGL869:software,MGL870:software,MGR817:telecom,MGR820:network,MGR825:management,MGR840:telecom,MGR850:management,MGR860:telecom,MGR870:telecom,MGR880:management,MTI515:entrepreneur,MTI710:software,MTI727:software,MTI805:signal,MTI811:software,MTI812:software,MTI815:telecom,MTI820:database,MTI825:software,MTI830:data,MTI836:software,MTI840:cloud,MTI845:ergonomics,MTI850:data,MTI855:game,MTI860:game,MTI865:ai,MTI870:signal,MTI881:software,MTI882:software,MTR801:manufacturing,MTR801E:manufacturing,MTR871:research,MTR871E:research,MTR873:manufacturing,MTR874:manufacturing,MTR890:research,MTR890E:research,MTR891:manufacturing,MTR892:management,MTR893:project,MTR895:manufacturing,MTR896:management,MTR899:entrepreneur,PCH050:internship,PGM101:manufacturing,PGM105:quality,PGM110:finance,PGM116:quality,PGM121:construction,PGM125:quality,PGM180:management,PHY010:physics,PHY015:physics,PHY144:physics,PHY332:power,PHY335:physics,PRE011:safety,PRE012:safety,PRE013:safety,PRE811:internship,PRE815:safety,PRE816:safety,PRE817:communication,QUA121:quality,QUA134:quality,QUA142:quality,QUA151:quality,QUA152:quality,QUA165:quality,QUA181:quality,QUA192:quality,QUA202:project,SCE010:physics,SCE020:physics,SCE030:project,SDE880:research,SDE885:research,SDE886:research,SDE887:research,SSC810:construction,SSC820:construction,SSC830:construction,SSC840:env,SSC850:construction,SST801:safety,SST803:safety,SST805:manufacturing,SST815:mechanics,SST820:safety,SST825:safety,SST830:ergonomics,SST835:manufacturing,SST880:safety,STA101:internship,STA102:internship,STA103:internship,STA104:internship,STA105:internship,STA106:internship,STA107:internship,STA108:internship,STA109:internship,STA201:internship,STA202:internship,STA203:internship,STA204:internship,STA205:power,STA206:internship,STA207:internship,STA208:internship,STA209:internship,STA210:internship,STA301:internship,STA302:internship,STA303:internship,STA304:internship,STA305:internship,STA306:internship,STA307:internship,STA308:internship,STA321:internship,STA323:internship,STA350:internship,STA351:internship,STA352:internship,STA401:internship,STA402:internship,STA403:internship,STA404:internship,STA405:internship,STA406:internship,STA407:internship,STA408:internship,STA421:internship,STA423:internship,STA450:internship,STA451:internship,STA452:internship,STA800:internship,STA802:internship,STA806:internship,SYS800:quality,SYS801:robot,SYS802:management,SYS803:management,SYS804:mechanics,SYS805:management,SYS806:mechanics,SYS807:management,SYS808:embedded,SYS809:ai,SYS810:optimization,SYS811:electronics,SYS812:mechanics,SYS813:management,SYS814:physics,SYS815:management,SYS816:management,SYS817:urban,SYS818:ai,SYS819:ai,SYS823:robot,SYS824:robot,SYS825:management,SYS827:robot,SYS828:ai,SYS829:manufacturing,SYS833:signal,SYS835:signal,SYS836:telecom,SYS839:power,SYS840:signal,SYS843:ai,SYS848:management,SYS849:manufacturing,SYS856:management,SYS857:management,SYS861:management,SYS862:mechanics,SYS863:manufacturing,SYS864:power,SYS865:mechanics,SYS866:manufacturing,SYS872:env,SYS873:management,SYS874:manufacturing,SYS875:robot,SYS876:materials,SYS877:management,SYS878:renewable,SYS879:ai,SYS880:management,TCH007:bim,TCH008:materials,TCH009:software,TCH011:electronics,TCH013:robot,TCH016:web,TCH017:embedded,TCH020:safety,TCH023:construction,TCH025:construction,TCH030:power,TCH033:electronics,TCH035:electronics,TCH040:manufacturing,TCH043:research,TCH045:mechanics,TCH055:database,TCH056:web,TCH057:research,TCH097:env,TCH098:project,TCH099:project,TEL115:telecom,TEL126:telecom,TEL136:embedded,TEL141:telecom,TEL145:telecom,TEL147:network,TEL148:telecom,TEL151:network,TEL156:network,TEL160:telecom,TIN503:env,TIN504:biomed,TIN540:ergonomics"

    private val table: Map<String, String> by lazy {
        TABLE.split(',').associate { it.substringBefore(':') to it.substringAfter(':') }
    }

    private fun plain(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    fun guess(title: String): String? {
        val t = plain(title)
        for ((category, words) in rules) {
            for (w in words) {
                val pattern = if (w.length <= 5) "(?<![a-z])" + Regex.escape(w) + "(?![a-z])" else "(?<![a-z])" + Regex.escape(w)
                if (Regex(pattern).containsMatchIn(t)) return category
            }
        }
        return null
    }

    fun category(acronym: String, title: String? = null): String {
        val code = acronym.uppercase().substringBefore('-').trim()
        table[code]?.let { return it }
        title?.let { guess(it) }?.let { return it }
        val letters = code.takeWhile { it.isLetter() }
        return prefixes[letters.take(4)] ?: prefixes[letters.take(3)] ?: "research"
    }

    fun forCourse(acronym: String, title: String? = null): ImageVector =
        icons[category(acronym, title)] ?: Icons.Outlined.School
}
