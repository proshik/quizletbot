package ru.proshik.english.quizlet.telegramBot.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UserGroupsResp(val id: Long,
                          val url: String,
                          val name: String,
                          @JsonProperty("created_date") val createdDate: Long,
                          val school: GroupSchoolResp,
                          val sets: List<SetResp>,
                          val members: List<GroupMemberResp>)