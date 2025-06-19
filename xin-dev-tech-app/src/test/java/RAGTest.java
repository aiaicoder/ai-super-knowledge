import com.alibaba.fastjson2.JSON;
import com.xin.dev.tech.Application;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.record.chart.TickRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/aiaicoder">  小新
 * @version 1.0
 * @date 2025/6/18 20:44
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class RAGTest {
    @Resource
    private OllamaChatClient ollamaChatClient;

    @Resource
    private SimpleVectorStore simpleVectorStore;

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;


    @Test
    public void upload() {
        TikaDocumentReader reader = new TikaDocumentReader("./data.txt");
        List<Document> documents = reader.get();
        //按token进行切分，保证文本的token长度是符合模型的maxToken的要求
        List<Document> documentList = tokenTextSplitter.apply(documents);
        //为两个文档列表中的每个文档添加元数据标签 "knowledge": "eth价格"，用于后续检索或分类用途。
        documents.forEach(doc-> doc.getMetadata().put("knowledge", "eth价格"));
        documentList.forEach(doc-> doc.getMetadata().put("knowledge", "eth价格"));
        pgVectorStore.accept(documentList);
        log.info("上传成功");
    }

    @Test
    public void chat(){
        String message = "今天的eth价格是多少";
        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;
        //SearchRequest.query(message)：以message内容作为查询语句。
        //.withTopK(5)：限制返回结果为最相关的前5条。
        //.withFilterExpression("knowledge == ‘eth价格’")：添加过滤条件，只返回knowledge字段等于“eth价格”的结果。
        SearchRequest request = SearchRequest.query(message).withTopK(5).withFilterExpression("knowledge == 'eth价格'");
        List<Document> documents = pgVectorStore.similaritySearch(request);
        //获取搜索结果，因为文档是切割过的，所以需要拼接起来
        String documentsCollectors  = documents.stream().map(Document::getContent).collect(Collectors.joining());
        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);
        ChatResponse chatResponse = ollamaChatClient.call(new Prompt(messages, OllamaOptions.create().withModel("deepseek-r1:1.5b")));
        log.info("测试结果:{}", JSON.toJSONString(chatResponse));
    }



}
