import com.xin.dev.tech.Application;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ai.document.Document;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;


/**
 * @author <a href="https://github.com/aiaicoder">  小新
 * @version 1.0
 * @date 2025/6/20 20:52
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class GitTest {
    @Resource
    private OllamaChatClient ollamaChatClient;

    @Resource
    private SimpleVectorStore simpleVectorStore;

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;


    //从git上拉取文件
    @Test
    public void test() throws Exception{
        String repoUrl = "https://github.com/aiaicoder/SuperTraveller.git";

        String localPath = "./cloned-repo";

        log.info("克隆路径：" + new File(localPath).getAbsolutePath());

        FileUtils.deleteDirectory(new File(localPath));

        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localPath))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("username", "password"))
                .call();

        git.close();
    }

    @Test
    public void test_file() throws Exception {
        Files.walkFileTree(Paths.get("./cloned-repo"),new SimpleFileVisitor<>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.info("文件路径:{}", file.toString());
                //读取拉去下来的文件
                // 忽略空文件或非普通文件
                if (attrs.size() == 0 || !Files.isRegularFile(file)) {
                    return FileVisitResult.CONTINUE;
                }
                PathResource pathResource = new PathResource(file);
                TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(pathResource);
                List<Document> documents = tikaDocumentReader.get();
                // 如果没有提取到内容，跳过该文件
                if (documents == null || documents.isEmpty()) {
                    log.warn("未从文件中提取到有效内容: {}", file);
                    return FileVisitResult.CONTINUE;
                }
                List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
                //添加元数据，打标
                documents.forEach(doc -> doc.getMetadata().put("knowledge", "super-travel"));
                documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", "super-travel"));
                //添加到向量数据库
                pgVectorStore.add(documentSplitterList);
                return FileVisitResult.CONTINUE;
            }
        });
    }



}
