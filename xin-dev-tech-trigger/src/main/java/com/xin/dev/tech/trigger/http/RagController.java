package com.xin.dev.tech.trigger.http;

import com.xin.dev.tech.api.IRagService;
import com.xin.dev.tech.api.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author <a href="https://github.com/aiaicoder">  小新
 * @version 1.0
 * @date 2025/6/19 15:26
 */
@RequestMapping("/api/v1/rag")
@CrossOrigin("*")
@Slf4j
@RestController()
public class RagController implements IRagService {

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private RedissonClient redissonClient;


    @Override
    @RequestMapping(value = "query_rag_tag_list", method = RequestMethod.GET)
    public Response<List<String>> queryRagTagList() {
        RList<String> ragTag = redissonClient.getList("ragTag");
        return Response.<List<String>>builder().code("0000").info("调用成功").data(ragTag).build();
    }

    @Override
    @RequestMapping(value = "file/upload", method = RequestMethod.POST, headers = "content-Type=multipart/form-data")
    public Response<String> uploadFile(@RequestParam String ragTag, @RequestParam(value ="file")List<MultipartFile> files) {
        log.info("上传知识库开始");
        for (MultipartFile file : files) {
            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(file.getResource());
            List<Document> documents = tikaDocumentReader.get();
            //分割文本
            List<Document> documentTextSplitter = tokenTextSplitter.apply(documents);
            documents.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));

            documentTextSplitter.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
            pgVectorStore.add(documentTextSplitter);
            RList<String> elements = redissonClient.getList("ragTag");
            if (!elements.contains(ragTag)) {
                elements.add(ragTag);
            }
        }
        log.info("上传知识库完成 {}", ragTag);
        return Response.<String>builder().code("0000").info("调用成功").build();

    }


}
