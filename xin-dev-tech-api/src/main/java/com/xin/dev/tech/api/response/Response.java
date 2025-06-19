package com.xin.dev.tech.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author <a href="https://github.com/aiaicoder">  小新
 * @version 1.0
 * @date 2025/6/19 15:17
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Response <T>{
    private String code;
    private String info;
    private T data;
}
