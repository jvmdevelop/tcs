package com.jvmd.transationapp.service;

import com.jvmd.transationapp.model.Transactions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class LLMService {

    private final OllamaChatModel chatModel;

    public String analyzeTransaction(Transactions transaction) {
        return chatModel.call("Ты помощник админа. Тебе нужно определить тип транзакции мошенническая/немошенническая. Ответь в максимум 400 символов. Начни со слов 'Данная операция являеться ... потому , что'. Amount операции измеряется в рублях. Вот транзакция: " + transaction.toString());
    }

}
