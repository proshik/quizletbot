package ru.proshik.english.quizlet.telegramBot.service.vo

enum class ModeType(val title: String,
                    val designations: List<String>,
                    val enabled: Boolean) {

    // I don't know what mode type for follow modes: review, live, bismarck, spacerace, voicerace
    // I'm not sure about the mode vscatter, but I put that in the MATCH mode type

    LEARN("Learn", listOf("learning_assistant"), true),
    FLASHCARDS("Flashcards", listOf("flashcards", "mobile_cards"), true),
    WRITE("Write", listOf("learn", "mobile_learn"), true),
    SPELL("Spell", listOf("speller"), true),
    TEST("Test", listOf("test"), true),
    MATCH("Match", listOf("scatter", "mobile_scatter", "microscatter", "vscatter"), true),
    GRAVITY("Gravity", listOf("gravity"), true),
    SPACERACE("Space Race", listOf("spacerace"), false);

    companion object {

        fun allDesignations(): List<String> {
            return ModeType.values()
                    .flatMap { modeType -> modeType.designations }
        }

        fun designationsByModeTypes(modeTypes: List<ModeType>): List<String> {
            return ModeType.values()
                    .filter(modeTypes::contains)
                    .flatMap(ModeType::designations)
        }

        fun modeTypeByDesignation(designation: String): ModeType? {
            return modeTypeByDesignation().asSequence().firstOrNull { entry -> entry.key == designation }?.value
        }

        fun modeTypeByDesignation(): Map<String, ModeType> {
            return values().flatMap { modeType -> modeType.designations.map { s -> Pair(s, modeType) } }
                    .toMap()
        }

    }
}