package ru.proshik.english.quizlet.telegramBot.service.model

enum class ModeType(val title: String,
                    val designations: List<String>) {

    LEARN("Learn", listOf("learning_assistant")),
    FLASHCARDS("Flashcards", listOf("flashcards", "mobile_cards")),
    WRITE("Write", listOf("learn", "mobile_learn")),
    SPELL("Spell", listOf("speller")),
    TEST("Test", listOf("test")),
    MATCH("Match", listOf("scatter", "mobile_scatter")),
    GRAVITY("Gravity", listOf("gravity"));

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
    }

}