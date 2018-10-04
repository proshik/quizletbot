package ru.proshik.english.quizlet.telegramBot.service.vo

enum class NavigateType(val key: String) {

    // navigate by items only with paging buttons (ItemPageKeyboard)
    PAGING_BY_ITEM("pbi"),
    // navigate by items with paging by items (StepPageKeyboard)
    PAGING_BY_BUTTON("pbb"),
    // select next step
    NEXT_STEP("ns")

}