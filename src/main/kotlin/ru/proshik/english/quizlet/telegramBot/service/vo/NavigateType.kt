package ru.proshik.english.quizlet.telegramBot.service.vo

enum class NavigateType(val key: String) {

    // navigate by items only with paging buttons (items will change, not buttons). Usually, use for iterate by result items
    PAGING_BY_ITEMS("pbi"),
    // navigate by buttons with paging by buttons (buttons will change, no items). Usually, use for the intermediate steps
    PAGING_BY_BUTTONS("pbb"),
    // update items. I use that type to update the result message with help extra buttons
    UPDATE_ITEMS("ui"),
    // update buttons.
    UPDATE_BUTTONS("ub"),
    // select next step.
    NEXT_STEP("ns"),
    // select previous step
    PREV_STEP("ps")

}