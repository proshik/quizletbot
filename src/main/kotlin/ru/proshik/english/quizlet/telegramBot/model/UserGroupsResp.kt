package ru.proshik.english.quizlet.telegramBot.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserGroupsResp(val id: Int,
                          val url: String,
                          val name: String,
                          @JsonProperty("created_date") val createdDate: Long,
                          val school: GroupSchoolResp,
                          val sets: List<SetResp>,
                          val members: List<GroupMemberResp>)