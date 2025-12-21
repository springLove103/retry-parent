package org.example.client;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
public class BaseRetryableApiClient {

    @Autowired
    private RetryTemplate retryTemplate;

    @SuppressWarnings("rawtypes")
    protected final <T> T get(RestTemplate restTemplate, String url, HttpEntity httpEntity,
                              Class<T> responseType, Object... urlVariables) {
        ResponseEntity<T> responseEntity = retryTemplate
                .execute(context -> restTemplate.exchange(url, HttpMethod.GET, httpEntity,
                        responseType, urlVariables));

        return responseEntity.getBody();
    }

    /* ---------- 1. 简单返回（Class） ---------- */
    protected final <T> T post(RestTemplate restTemplate,
                               String url,
                               HttpEntity<?> httpEntity,
                               Class<T> responseType,
                               Object... urlVariables) {
        return post0(restTemplate, url, httpEntity, responseType, urlVariables);
    }

    /* ---------- 2. 泛型返回（ParameterizedTypeReference） ---------- */
    protected final <T> T post(RestTemplate restTemplate,
                               String url,
                               HttpEntity<?> httpEntity,
                               ParameterizedTypeReference<T> typeRef,
                               Object... urlVariables) {
        return post0(restTemplate, url, httpEntity, typeRef, urlVariables);
    }

    /* ---------- 3. 统一实现 ---------- */
    private <T> T post0(RestTemplate restTemplate,
                        String url,
                        HttpEntity<?> httpEntity,
                        Object typeToken,          // Class 或 ParameterizedTypeReference
                        Object... urlVariables) {

        // 3.1 保证 JSON 头
        HttpHeaders headers = httpEntity.getHeaders();
        if (headers == null) {
            headers = new HttpHeaders();
        }
        if (headers.getContentType() == null ||
                !headers.getContentType().includes(MediaType.APPLICATION_JSON)) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<?> newEntity = new HttpEntity<>(httpEntity.getBody(), headers);

        // 3.2 根据 url 有无占位符选重载
        ResponseEntity<T> rsp;
        boolean hasVars = urlVariables != null && urlVariables.length > 0 && url.contains("{");
        if (typeToken instanceof Class) {
            Class<T> clazz = (Class<T>) typeToken;
            rsp = retryTemplate.execute(context -> hasVars
                    ? restTemplate.exchange(url, HttpMethod.POST, newEntity, clazz, urlVariables)
                    : restTemplate.exchange(url, HttpMethod.POST, newEntity, clazz));
        } else {
            ParameterizedTypeReference<T> typeRef = (ParameterizedTypeReference<T>) typeToken;
            rsp = retryTemplate.execute(context -> hasVars
                    ? restTemplate.exchange(url, HttpMethod.POST, newEntity, typeRef, urlVariables)
                    : restTemplate.exchange(url, HttpMethod.POST, newEntity, typeRef));
        }

        return rsp.getBody();
    }
}
