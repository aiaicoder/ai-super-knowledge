package com.xin.dev.tech.trigger.http;
import com.xin.dev.tech.api.IRagService;
import com.xin.dev.tech.api.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
    @RequestMapping(value = "analyze_git_repository", method = RequestMethod.POST)
    public Response<String> analyzeGitRepository(String repoUrl) {
        String localPath = "./cloned-repo";
        String repoProjectName = extractProjectName(repoUrl);
        log.info("克隆路径：" + new File(localPath).getAbsolutePath());
        Git git = null;
        try {
            FileUtils.deleteDirectory(new File(localPath));
            git = Git.cloneRepository().setURI(repoUrl).setDirectory(new File(localPath)).call();
            Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    log.info("文件路径:{}", file.toString());
                    //读取拉去下来的文件
                    PathResource pathResource = new PathResource(file);
                    TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(pathResource);
                    List<Document> documents = tikaDocumentReader.get();
                    List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
                    //添加元数据，打标
                    documents.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
                    documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
                    //添加到向量数据库
                    pgVectorStore.add(documentSplitterList);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
            FileUtils.deleteDirectory(new File(localPath));
        } catch (Exception e) {
            log.error("Git clone error: " + e.getMessage());
        } finally {
            if (git != null) {
                git.close();
            }

        }
        log.info("遍历解析路径，上传完成:{}", repoUrl);
        RList<String> elements = redissonClient.getList("ragTag");
        if (!elements.contains(repoProjectName)) {
            elements.add(repoProjectName);
        }
        return Response.<String>builder().code("0000").info("调用成功").build();
    }

    @Override
    @RequestMapping(value = "analyze_git_repository_token", method = RequestMethod.POST)
    public Response<String> analyzeGitRepository(String repoUrl, String userName, String token) throws Exception {
        String localPath = "./git-cloned-repo";
        String repoProjectName = extractProjectName(repoUrl);
        log.info("克隆路径：{}", new File(localPath).getAbsolutePath());
        FileUtils.deleteDirectory(new File(localPath));
        Git git = Git.cloneRepository().setURI(repoUrl).setDirectory(new File(localPath)).setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token)).call();

        Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.info("{} 遍历解析路径，上传知识库:{}", repoProjectName, file.getFileName());
                try {
                    TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
                    List<Document> documents = reader.get();
                    List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
                    documents.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
                    documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
                    pgVectorStore.accept(documentSplitterList);
                } catch (Exception e) {
                    log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        FileUtils.deleteDirectory(new File(localPath));

        RList<String> elements = redissonClient.getList("ragTag");
        if (!elements.contains(repoProjectName)) {
            elements.add(repoProjectName);
        }
        git.close();
        log.info("遍历解析路径，上传完成:{}", repoUrl);
        return Response.<String>builder().code("0000").info("调用成功").build();
    }

    @Override
    @RequestMapping(value = "file/upload", method = RequestMethod.POST, headers = "content-Type=multipart/form-data")
    public Response<String> uploadFile(@RequestParam String ragTag, @RequestParam(value = "file") List<MultipartFile> files) {
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

    private String extractProjectName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String projectNameWithGit = parts[parts.length - 1];
        return projectNameWithGit.replace(".git", "");
    }


}
