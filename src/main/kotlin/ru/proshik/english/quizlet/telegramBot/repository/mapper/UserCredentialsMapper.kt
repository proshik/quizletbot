package ru.proshik.english.quizlet.telegramBot.repository.mapper

import org.springframework.jdbc.core.RowMapper
import ru.proshik.english.quizlet.telegramBot.service.vo.UserCredentials
import java.sql.ResultSet

class UserCredentialsMapper : RowMapper<UserCredentials> {

    override fun mapRow(rs: ResultSet, rowNum: Int) =
            UserCredentials(rs.getString("login"), rs.getString("access_token"))

}