package com.xin.dev.tech.trigger.http;

import com.xin.dev.tech.api.IAiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/aiaicoder">  小新
 * @version 1.0
 * @date 2025/6/21 19:40
 */
@RequestMapping("/api/v1/openAi")
@CrossOrigin("*")
@RestController()
@Slf4j
public class OpenAiController implements IAiService {
    @Resource
    private OpenAiChatClient chatClient;
    @Resource
    private PgVectorStore pgVectorStore;

    @Override
    @RequestMapping(value = "/generate" ,method = RequestMethod.GET)
    public ChatResponse generate(String model, String message) {
       return  chatClient.call(new Prompt(message, OpenAiChatOptions.builder().withModel(model).build()));
    }

    @Override
    @RequestMapping(value = "/generate_stream",method = RequestMethod.GET)
    public Flux<ChatResponse> generateStream(String model, String message) {
        return chatClient.stream(new Prompt(message, OpenAiChatOptions.builder().withModel(model).build()));
    }

    @Override
    @RequestMapping(value = "/generate_stream_rag",method = RequestMethod.GET)
    public Flux<ChatResponse> generateStreamRag(String model, String ragTag, String message) {
        final String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;
        //SearchRequest.query(message)：以message内容作为查询语句。
        //.withTopK(5)：限制返回结果为最相关的前5条。
        //.withFilterExpression("knowledge == ‘eth价格’")：添加过滤条件，只返回knowledge字段等于“eth价格”的结果。
        SearchRequest request = SearchRequest.query(message).withTopK(5).withFilterExpression("knowledge == \"" + ragTag + "\"");
        List<Document> documents = pgVectorStore.similaritySearch(request);
        //获取搜索结果，因为文档是切割过的，所以需要拼接起来
        String documentsCollectors  = documents.stream().map(Document::getContent).collect(Collectors.joining());
        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);
        return chatClient.stream(new Prompt(messages, OpenAiChatOptions.builder().withModel(model).build()));
    }
}
