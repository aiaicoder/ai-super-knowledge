package com.xin.dev.tech.api;

import com.xin.dev.tech.api.response.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author <a href="https://github.com/aiaicoder">  小新
 * @version 1.0
 * @date 2025/6/19 15:08
 */
public interface IRagService {

    Response<String> uploadFile(String ragTag, List<MultipartFile> files);

    Response<List<String>> queryRagTagList();

    /**
     * 分析git仓库,public无需提供密钥
     * @param repoUrl
     * @return
     */
    Response<String> analyzeGitRepository(String repoUrl);

    Response<String> analyzeGitRepository(String repoUrl, String userName, String token) throws Exception;

}
