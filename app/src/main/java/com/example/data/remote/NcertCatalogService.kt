package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NcertBookMetadata(
    val title: String,
    val category: String,
    val subject: String,
    val exam: String = "CBSE / State Boards",
    val description: String = "",
    val fileUrl: String
)

/**
 * Curated, comprehensive repository of official NCERT textbook PDFs from Class 1 to Class 12
 * for all major subjects (Mathematics, Science, EVS, Physics, Chemistry, Biology, Social Science,
 * History, Geography, Political Science, Economics, English, Hindi, Sanskrit, Accountancy, Business Studies).
 *
 * Each book points to the official open educational resource URL on ncert.nic.in with reliable offline
 * reader support in PdfViewerScreen.
 */
object NcertCatalogService {

    private val FULL_CLASS_1_TO_12_CATALOG = listOf(
        // ================= CLASS 1 =================
        NcertBookMetadata("Joyful Mathematics – Class 1", "Class 1", "Mathematics", "CBSE / Primary", "Complete NCERT Class 1 Joyful Mathematics textbook.", "https://ncert.nic.in/textbook/pdf/aejm101.pdf"),
        NcertBookMetadata("Mridang English – Class 1", "Class 1", "English", "CBSE / Primary", "Complete NCERT Class 1 English textbook Mridang.", "https://ncert.nic.in/textbook/pdf/aeen101.pdf"),
        NcertBookMetadata("Sarangi Hindi – Class 1", "Class 1", "Hindi", "CBSE / Primary", "Complete NCERT Class 1 Hindi textbook Sarangi.", "https://ncert.nic.in/textbook/pdf/ahhn101.pdf"),

        // ================= CLASS 2 =================
        NcertBookMetadata("Joyful Mathematics – Class 2", "Class 2", "Mathematics", "CBSE / Primary", "Complete NCERT Class 2 Joyful Mathematics textbook.", "https://ncert.nic.in/textbook/pdf/bejm101.pdf"),
        NcertBookMetadata("Mridang English – Class 2", "Class 2", "English", "CBSE / Primary", "Complete NCERT Class 2 English textbook Mridang.", "https://ncert.nic.in/textbook/pdf/been101.pdf"),
        NcertBookMetadata("Sarangi Hindi – Class 2", "Class 2", "Hindi", "CBSE / Primary", "Complete NCERT Class 2 Hindi textbook Sarangi.", "https://ncert.nic.in/textbook/pdf/bhhn101.pdf"),

        // ================= CLASS 3 =================
        NcertBookMetadata("Math-Magic – Class 3", "Class 3", "Mathematics", "CBSE / Primary", "Complete NCERT Class 3 Math-Magic textbook.", "https://ncert.nic.in/textbook/pdf/cemh101.pdf"),
        NcertBookMetadata("Looking Around (EVS) – Class 3", "Class 3", "EVS", "CBSE / Primary", "Complete NCERT Class 3 Environmental Studies textbook.", "https://ncert.nic.in/textbook/pdf/ceap101.pdf"),
        NcertBookMetadata("Santoor English – Class 3", "Class 3", "English", "CBSE / Primary", "Complete NCERT Class 3 English textbook Santoor.", "https://ncert.nic.in/textbook/pdf/ceen101.pdf"),
        NcertBookMetadata("Veena Hindi – Class 3", "Class 3", "Hindi", "CBSE / Primary", "Complete NCERT Class 3 Hindi textbook Veena.", "https://ncert.nic.in/textbook/pdf/chhn101.pdf"),

        // ================= CLASS 4 =================
        NcertBookMetadata("Math-Magic – Class 4", "Class 4", "Mathematics", "CBSE / Primary", "Complete NCERT Class 4 Math-Magic textbook.", "https://ncert.nic.in/textbook/pdf/demh101.pdf"),
        NcertBookMetadata("Looking Around (EVS) – Class 4", "Class 4", "EVS", "CBSE / Primary", "Complete NCERT Class 4 Environmental Studies textbook.", "https://ncert.nic.in/textbook/pdf/deap101.pdf"),
        NcertBookMetadata("Marigold English – Class 4", "Class 4", "English", "CBSE / Primary", "Complete NCERT Class 4 Marigold English textbook.", "https://ncert.nic.in/textbook/pdf/deen101.pdf"),
        NcertBookMetadata("Rimjhim Hindi – Class 4", "Class 4", "Hindi", "CBSE / Primary", "Complete NCERT Class 4 Rimjhim Hindi textbook.", "https://ncert.nic.in/textbook/pdf/dhhn101.pdf"),

        // ================= CLASS 5 =================
        NcertBookMetadata("Math-Magic – Class 5", "Class 5", "Mathematics", "CBSE / Primary", "Complete NCERT Class 5 Math-Magic textbook.", "https://ncert.nic.in/textbook/pdf/eemh101.pdf"),
        NcertBookMetadata("Looking Around (EVS) – Class 5", "Class 5", "EVS", "CBSE / Primary", "Complete NCERT Class 5 Environmental Studies textbook.", "https://ncert.nic.in/textbook/pdf/eeap101.pdf"),
        NcertBookMetadata("Marigold English – Class 5", "Class 5", "English", "CBSE / Primary", "Complete NCERT Class 5 Marigold English textbook.", "https://ncert.nic.in/textbook/pdf/eeen101.pdf"),
        NcertBookMetadata("Rimjhim Hindi – Class 5", "Class 5", "Hindi", "CBSE / Primary", "Complete NCERT Class 5 Rimjhim Hindi textbook.", "https://ncert.nic.in/textbook/pdf/ehhn101.pdf"),

        // ================= CLASS 6 =================
        NcertBookMetadata("Curiosity Science – Class 6", "Class 6", "Science", "CBSE / Middle", "Official NEP 2020 NCERT Class 6 Science Curiosity textbook.", "https://ncert.nic.in/textbook/pdf/fecu101.pdf"),
        NcertBookMetadata("Ganita Prakash Mathematics – Class 6", "Class 6", "Mathematics", "CBSE / Middle", "Official NEP 2020 NCERT Class 6 Ganita Prakash Mathematics textbook.", "https://ncert.nic.in/textbook/pdf/femh101.pdf"),
        NcertBookMetadata("Exploring Society (Social Science) – Class 6", "Class 6", "Social Science", "CBSE / Middle", "Official NEP 2020 NCERT Class 6 Social Science textbook.", "https://ncert.nic.in/textbook/pdf/fess101.pdf"),
        NcertBookMetadata("Poorvi English – Class 6", "Class 6", "English", "CBSE / Middle", "Official NEP 2020 NCERT Class 6 Poorvi English textbook.", "https://ncert.nic.in/textbook/pdf/feen101.pdf"),
        NcertBookMetadata("Malhar Hindi – Class 6", "Class 6", "Hindi", "CBSE / Middle", "Official NEP 2020 NCERT Class 6 Malhar Hindi textbook.", "https://ncert.nic.in/textbook/pdf/fhhn101.pdf"),
        NcertBookMetadata("Deepakam Sanskrit – Class 6", "Class 6", "Sanskrit", "CBSE / Middle", "Official NEP 2020 NCERT Class 6 Deepakam Sanskrit textbook.", "https://ncert.nic.in/textbook/pdf/fsk101.pdf"),

        // ================= CLASS 7 =================
        NcertBookMetadata("Science – Class 7", "Class 7", "Science", "CBSE / Middle", "Complete NCERT Class 7 Science textbook with chapters & exercises.", "https://ncert.nic.in/textbook/pdf/gesc101.pdf"),
        NcertBookMetadata("Mathematics – Class 7", "Class 7", "Mathematics", "CBSE / Middle", "Complete NCERT Class 7 Mathematics textbook.", "https://ncert.nic.in/textbook/pdf/gemh101.pdf"),
        NcertBookMetadata("Our Pasts II (History) – Class 7", "Class 7", "Social Science", "CBSE / Middle", "NCERT Class 7 History Our Pasts II textbook.", "https://ncert.nic.in/textbook/pdf/gess101.pdf"),
        NcertBookMetadata("Our Environment (Geography) – Class 7", "Class 7", "Social Science", "CBSE / Middle", "NCERT Class 7 Geography Our Environment textbook.", "https://ncert.nic.in/textbook/pdf/gess201.pdf"),
        NcertBookMetadata("Social and Political Life II – Class 7", "Class 7", "Social Science", "CBSE / Middle", "NCERT Class 7 Civics & Political Life textbook.", "https://ncert.nic.in/textbook/pdf/gess301.pdf"),
        NcertBookMetadata("Honeycomb English – Class 7", "Class 7", "English", "CBSE / Middle", "NCERT Class 7 Honeycomb English textbook.", "https://ncert.nic.in/textbook/pdf/geen101.pdf"),
        NcertBookMetadata("Vasant II Hindi – Class 7", "Class 7", "Hindi", "CBSE / Middle", "NCERT Class 7 Vasant II Hindi textbook.", "https://ncert.nic.in/textbook/pdf/ghhn101.pdf"),
        NcertBookMetadata("Ruchira II Sanskrit – Class 7", "Class 7", "Sanskrit", "CBSE / Middle", "NCERT Class 7 Ruchira II Sanskrit textbook.", "https://ncert.nic.in/textbook/pdf/gsk101.pdf"),

        // ================= CLASS 8 =================
        NcertBookMetadata("Science – Class 8", "Class 8", "Science", "CBSE / Middle", "Complete NCERT Class 8 Science textbook with diagrams & experiments.", "https://ncert.nic.in/textbook/pdf/hesc101.pdf"),
        NcertBookMetadata("Mathematics – Class 8", "Class 8", "Mathematics", "CBSE / Middle", "Complete NCERT Class 8 Mathematics textbook with solutions.", "https://ncert.nic.in/textbook/pdf/hemh101.pdf"),
        NcertBookMetadata("Our Pasts III (History) – Class 8", "Class 8", "Social Science", "CBSE / Middle", "NCERT Class 8 History Our Pasts III textbook.", "https://ncert.nic.in/textbook/pdf/hess101.pdf"),
        NcertBookMetadata("Resource and Development (Geography) – Class 8", "Class 8", "Social Science", "CBSE / Middle", "NCERT Class 8 Geography Resource and Development textbook.", "https://ncert.nic.in/textbook/pdf/hess201.pdf"),
        NcertBookMetadata("Social and Political Life III – Class 8", "Class 8", "Social Science", "CBSE / Middle", "NCERT Class 8 Civics & Constitution textbook.", "https://ncert.nic.in/textbook/pdf/hess301.pdf"),
        NcertBookMetadata("Honeydew English – Class 8", "Class 8", "English", "CBSE / Middle", "NCERT Class 8 Honeydew English textbook.", "https://ncert.nic.in/textbook/pdf/heen101.pdf"),
        NcertBookMetadata("Vasant III Hindi – Class 8", "Class 8", "Hindi", "CBSE / Middle", "NCERT Class 8 Vasant III Hindi textbook.", "https://ncert.nic.in/textbook/pdf/hhhn101.pdf"),

        // ================= CLASS 9 =================
        NcertBookMetadata("Science – Class 9", "Class 9", "Science", "CBSE / Secondary", "Complete NCERT Class 9 Science textbook (Physics, Chemistry & Biology).", "https://ncert.nic.in/textbook/pdf/iesc101.pdf"),
        NcertBookMetadata("Mathematics – Class 9", "Class 9", "Mathematics", "CBSE / Secondary", "Complete NCERT Class 9 Mathematics textbook with proofs & exercises.", "https://ncert.nic.in/textbook/pdf/iemh101.pdf"),
        NcertBookMetadata("India and Contemporary World I (History) – Class 9", "Class 9", "Social Science", "CBSE / Secondary", "NCERT Class 9 World & Indian History textbook.", "https://ncert.nic.in/textbook/pdf/iess101.pdf"),
        NcertBookMetadata("Contemporary India I (Geography) – Class 9", "Class 9", "Social Science", "CBSE / Secondary", "NCERT Class 9 Geography textbook with maps & physical features.", "https://ncert.nic.in/textbook/pdf/iess201.pdf"),
        NcertBookMetadata("Democratic Politics I (Civics) – Class 9", "Class 9", "Social Science", "CBSE / Secondary", "NCERT Class 9 Democratic Politics & Governance textbook.", "https://ncert.nic.in/textbook/pdf/iess301.pdf"),
        NcertBookMetadata("Economics – Class 9", "Class 9", "Economics", "CBSE / Secondary", "NCERT Class 9 Economics textbook (Palampur, Poverty, Food Security).", "https://ncert.nic.in/textbook/pdf/iess401.pdf"),
        NcertBookMetadata("Beehive English – Class 9", "Class 9", "English", "CBSE / Secondary", "NCERT Class 9 Beehive English textbook.", "https://ncert.nic.in/textbook/pdf/iebe101.pdf"),
        NcertBookMetadata("Moments English Supplementary – Class 9", "Class 9", "English", "CBSE / Secondary", "NCERT Class 9 Moments supplementary reader.", "https://ncert.nic.in/textbook/pdf/iemo101.pdf"),
        NcertBookMetadata("Kshitij Hindi – Class 9", "Class 9", "Hindi", "CBSE / Secondary", "NCERT Class 9 Kshitij Hindi literature textbook.", "https://ncert.nic.in/textbook/pdf/ihks101.pdf"),
        NcertBookMetadata("Shemushi I Sanskrit – Class 9", "Class 9", "Sanskrit", "CBSE / Secondary", "NCERT Class 9 Shemushi I Sanskrit textbook.", "https://ncert.nic.in/textbook/pdf/isk101.pdf"),

        // ================= CLASS 10 =================
        NcertBookMetadata("Science – Class 10 (Board Exam)", "Class 10", "Science", "CBSE / Class 10 Board", "Complete NCERT Class 10 Science textbook (Chemical Reactions, Light, Life Processes, Electricity).", "https://ncert.nic.in/textbook/pdf/jesc101.pdf"),
        NcertBookMetadata("Mathematics – Class 10 (Board Exam)", "Class 10", "Mathematics", "CBSE / Class 10 Board", "Complete NCERT Class 10 Mathematics textbook (Trigonometry, Quadratic, Triangles, Statistics).", "https://ncert.nic.in/textbook/pdf/jemh101.pdf"),
        NcertBookMetadata("India and Contemporary World II (History) – Class 10", "Class 10", "Social Science", "CBSE / Class 10 Board", "NCERT Class 10 History textbook (Nationalism in Europe & India, Industrialization).", "https://ncert.nic.in/textbook/pdf/jess101.pdf"),
        NcertBookMetadata("Contemporary India II (Geography) – Class 10", "Class 10", "Social Science", "CBSE / Class 10 Board", "NCERT Class 10 Geography textbook (Resources, Agriculture, Manufacturing, Minerals).", "https://ncert.nic.in/textbook/pdf/jess201.pdf"),
        NcertBookMetadata("Democratic Politics II – Class 10", "Class 10", "Social Science", "CBSE / Class 10 Board", "NCERT Class 10 Political Science textbook (Power Sharing, Federalism, Political Parties).", "https://ncert.nic.in/textbook/pdf/jess301.pdf"),
        NcertBookMetadata("Understanding Economic Development – Class 10", "Class 10", "Economics", "CBSE / Class 10 Board", "NCERT Class 10 Economics textbook (Development, Sectors, Money & Credit, Globalization).", "https://ncert.nic.in/textbook/pdf/jess401.pdf"),
        NcertBookMetadata("First Flight English – Class 10", "Class 10", "English", "CBSE / Class 10 Board", "NCERT Class 10 First Flight English literature textbook.", "https://ncert.nic.in/textbook/pdf/jeff101.pdf"),
        NcertBookMetadata("Footprints without Feet – Class 10", "Class 10", "English", "CBSE / Class 10 Board", "NCERT Class 10 English supplementary reader.", "https://ncert.nic.in/textbook/pdf/jefp101.pdf"),
        NcertBookMetadata("Kshitij II Hindi – Class 10", "Class 10", "Hindi", "CBSE / Class 10 Board", "NCERT Class 10 Kshitij II Hindi literature textbook.", "https://ncert.nic.in/textbook/pdf/jhks101.pdf"),

        // ================= CLASS 11 =================
        NcertBookMetadata("Physics Part 1 – Class 11", "Class 11", "Physics", "CBSE / NEET / JEE", "NCERT Class 11 Physics Part 1 (Mechanics, Laws of Motion, Gravitation, Work & Energy).", "https://ncert.nic.in/textbook/pdf/keph101.pdf"),
        NcertBookMetadata("Physics Part 2 – Class 11", "Class 11", "Physics", "CBSE / NEET / JEE", "NCERT Class 11 Physics Part 2 (Thermodynamics, Solids & Fluids, Waves & Oscillations).", "https://ncert.nic.in/textbook/pdf/keph201.pdf"),
        NcertBookMetadata("Chemistry Part 1 – Class 11", "Class 11", "Chemistry", "CBSE / NEET / JEE", "NCERT Class 11 Chemistry Part 1 (Atomic Structure, Bonding, Thermodynamics, Equilibrium).", "https://ncert.nic.in/textbook/pdf/kech101.pdf"),
        NcertBookMetadata("Chemistry Part 2 – Class 11", "Class 11", "Chemistry", "CBSE / NEET / JEE", "NCERT Class 11 Chemistry Part 2 (Organic Chemistry, Hydrocarbons, Redox).", "https://ncert.nic.in/textbook/pdf/kech201.pdf"),
        NcertBookMetadata("Biology – Class 11", "Class 11", "Biology", "CBSE / NEET", "Complete NCERT Class 11 Biology textbook (Plant & Human Physiology, Cell Biology, Diversity).", "https://ncert.nic.in/textbook/pdf/kebo101.pdf"),
        NcertBookMetadata("Mathematics – Class 11", "Class 11", "Mathematics", "CBSE / JEE", "Complete NCERT Class 11 Mathematics textbook (Sets, Calculus, Conic Sections, Probability).", "https://ncert.nic.in/textbook/pdf/kemh101.pdf"),
        NcertBookMetadata("Indian Constitution at Work – Class 11", "Class 11", "Political Science", "CBSE / UPSC", "NCERT Class 11 Political Science textbook on Constitution & Rights.", "https://ncert.nic.in/textbook/pdf/keps101.pdf"),
        NcertBookMetadata("Political Theory – Class 11", "Class 11", "Political Science", "CBSE / UPSC", "NCERT Class 11 Political Theory (Freedom, Equality, Justice, Rights).", "https://ncert.nic.in/textbook/pdf/keps201.pdf"),
        NcertBookMetadata("Themes in World History – Class 11", "Class 11", "History", "CBSE / UPSC", "NCERT Class 11 World History textbook (Early Societies, Empires, Modernisation).", "https://ncert.nic.in/textbook/pdf/kess101.pdf"),
        NcertBookMetadata("Financial Accounting Part 1 – Class 11", "Class 11", "Accountancy", "CBSE / Commerce", "NCERT Class 11 Financial Accounting Part 1 textbook.", "https://ncert.nic.in/textbook/pdf/keac101.pdf"),
        NcertBookMetadata("Business Studies – Class 11", "Class 11", "Commerce", "CBSE / Commerce", "NCERT Class 11 Business Studies (Nature & Purpose, Forms of Business, Finance).", "https://ncert.nic.in/textbook/pdf/kebs101.pdf"),
        NcertBookMetadata("Introductory Microeconomics – Class 11", "Class 11", "Economics", "CBSE / Commerce / Arts", "NCERT Class 11 Microeconomics textbook (Consumer Behavior, Production, Cost, Markets).", "https://ncert.nic.in/textbook/pdf/keec101.pdf"),
        NcertBookMetadata("Statistics for Economics – Class 11", "Class 11", "Economics", "CBSE / Commerce / Arts", "NCERT Class 11 Statistics for Economics textbook.", "https://ncert.nic.in/textbook/pdf/keec201.pdf"),
        NcertBookMetadata("Hornbill English – Class 11", "Class 11", "English", "CBSE / Core", "NCERT Class 11 Hornbill English Core textbook.", "https://ncert.nic.in/textbook/pdf/kehb101.pdf"),

        // ================= CLASS 12 =================
        NcertBookMetadata("Physics Part 1 – Class 12", "Class 12", "Physics", "CBSE / NEET / JEE", "NCERT Class 12 Physics Part 1 (Electrostatics, Current, Magnetism, Optics, AC Circuits).", "https://ncert.nic.in/textbook/pdf/leph101.pdf"),
        NcertBookMetadata("Physics Part 2 – Class 12", "Class 12", "Physics", "CBSE / NEET / JEE", "NCERT Class 12 Physics Part 2 (Wave Optics, Dual Nature, Atoms, Nuclei, Semiconductors).", "https://ncert.nic.in/textbook/pdf/leph201.pdf"),
        NcertBookMetadata("Chemistry Part 1 – Class 12", "Class 12", "Chemistry", "CBSE / NEET / JEE", "NCERT Class 12 Chemistry Part 1 (Solutions, Electrochemistry, Kinetics, Coordination).", "https://ncert.nic.in/textbook/pdf/lech101.pdf"),
        NcertBookMetadata("Chemistry Part 2 – Class 12", "Class 12", "Chemistry", "CBSE / NEET / JEE", "NCERT Class 12 Chemistry Part 2 (Haloalkanes, Alcohols, Aldehydes, Amines, Biomolecules).", "https://ncert.nic.in/textbook/pdf/lech201.pdf"),
        NcertBookMetadata("Biology – Class 12", "Class 12", "Biology", "CBSE / NEET", "Complete NCERT Class 12 Biology textbook (Reproduction, Genetics, Evolution, Biotechnology, Ecology).", "https://ncert.nic.in/textbook/pdf/lebo101.pdf"),
        NcertBookMetadata("Mathematics Part 1 – Class 12", "Class 12", "Mathematics", "CBSE / JEE", "NCERT Class 12 Mathematics Part 1 (Relations & Functions, Matrices, Determinants, Calculus).", "https://ncert.nic.in/textbook/pdf/lemh101.pdf"),
        NcertBookMetadata("Mathematics Part 2 – Class 12", "Class 12", "Mathematics", "CBSE / JEE", "NCERT Class 12 Mathematics Part 2 (Integrals, Differential Equations, Vectors, 3D, Probability).", "https://ncert.nic.in/textbook/pdf/lemh201.pdf"),
        NcertBookMetadata("Themes in Indian History Part 1 – Class 12", "Class 12", "History", "CBSE / UPSC", "NCERT Class 12 Ancient Indian History textbook.", "https://ncert.nic.in/textbook/pdf/lehs101.pdf"),
        NcertBookMetadata("Themes in Indian History Part 2 – Class 12", "Class 12", "History", "CBSE / UPSC", "NCERT Class 12 Medieval Indian History textbook.", "https://ncert.nic.in/textbook/pdf/lehs201.pdf"),
        NcertBookMetadata("Themes in Indian History Part 3 – Class 12", "Class 12", "History", "CBSE / UPSC", "NCERT Class 12 Modern Indian History textbook.", "https://ncert.nic.in/textbook/pdf/lehs301.pdf"),
        NcertBookMetadata("Contemporary World Politics – Class 12", "Class 12", "Political Science", "CBSE / UPSC", "NCERT Class 12 World Politics & Global Affairs textbook.", "https://ncert.nic.in/textbook/pdf/leps101.pdf"),
        NcertBookMetadata("Politics in India Since Independence – Class 12", "Class 12", "Political Science", "CBSE / UPSC", "NCERT Class 12 Indian Political History textbook.", "https://ncert.nic.in/textbook/pdf/leps201.pdf"),
        NcertBookMetadata("Accountancy (Partnership & NPO) – Class 12", "Class 12", "Accountancy", "CBSE / Commerce", "NCERT Class 12 Accountancy Part 1 textbook.", "https://ncert.nic.in/textbook/pdf/leac101.pdf"),
        NcertBookMetadata("Accountancy (Company Accounts) – Class 12", "Class 12", "Accountancy", "CBSE / Commerce", "NCERT Class 12 Accountancy Part 2 textbook.", "https://ncert.nic.in/textbook/pdf/leac201.pdf"),
        NcertBookMetadata("Business Studies Part 1 – Class 12", "Class 12", "Commerce", "CBSE / Commerce", "NCERT Class 12 Business Studies (Management Principles & Planning).", "https://ncert.nic.in/textbook/pdf/lebs101.pdf"),
        NcertBookMetadata("Business Studies Part 2 – Class 12", "Class 12", "Commerce", "CBSE / Commerce", "NCERT Class 12 Business Studies (Financial Management & Marketing).", "https://ncert.nic.in/textbook/pdf/lebs201.pdf"),
        NcertBookMetadata("Introductory Macroeconomics – Class 12", "Class 12", "Economics", "CBSE / Commerce / Arts", "NCERT Class 12 Macroeconomics (National Income, Money, Banking, Govt Budget).", "https://ncert.nic.in/textbook/pdf/leec101.pdf"),
        NcertBookMetadata("Indian Economic Development – Class 12", "Class 12", "Economics", "CBSE / Commerce / Arts", "NCERT Class 12 Indian Economy textbook.", "https://ncert.nic.in/textbook/pdf/leec201.pdf"),
        NcertBookMetadata("Flamingo English – Class 12", "Class 12", "English", "CBSE / Core", "NCERT Class 12 Flamingo English Core textbook.", "https://ncert.nic.in/textbook/pdf/lefl101.pdf"),
        NcertBookMetadata("Vistas English Supplementary – Class 12", "Class 12", "English", "CBSE / Core", "NCERT Class 12 Vistas English Supplementary textbook.", "https://ncert.nic.in/textbook/pdf/levi101.pdf")
    )

    suspend fun fetchNcertCatalog(): List<NcertBookMetadata> = withContext(Dispatchers.IO) {
        FULL_CLASS_1_TO_12_CATALOG
    }
}
