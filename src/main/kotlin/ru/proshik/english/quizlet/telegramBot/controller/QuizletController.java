package ru.proshik.english.quizlet.telegramBot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("redirect")
public class QuizletController {

    @GetMapping
    public void authRedirect(@RequestParam("state") String state,
                             @RequestParam("code") String code) {



    }

}
