package com.vwmin.restproxy;

import com.vwmin.restproxy.annotations.Query;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vwmin
 * @version 1.0
 * @date 2020/4/6 11:31
 */
public class RestRequestFactory {

    private final String url;
    private final HttpMethod httpMethod;
    private final Annotation[] parameterAnnotations;
    private final Class<?> returnType;
    private final Method serviceMethod;

    public static RestRequestFactory parseAnnotations(String baseUrl, Method serviceMethod) {
        return new RestRequestFactoryBuilder(baseUrl, serviceMethod).build();
    }

    RestRequestFactory(String url, HttpMethod httpMethod,
                       Annotation[] parameterAnnotations,
                       Class<?> returnType, Method serviceMethod){
        this.url = url;
        this.httpMethod = httpMethod;
        this.parameterAnnotations = parameterAnnotations;
        this.returnType = returnType;
        this.serviceMethod = serviceMethod;
    }


    public URI create(Object[] args) {
        Map<String, Object> uriVariables = new HashMap<>(args.length);
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);
        for (int i=0; i<args.length; i++){
            Annotation annotation = parameterAnnotations[i];
            Object arg = args[i];
            if (annotation instanceof Query){
                String queryName = ((Query) annotation).value();
                if (arg == null){
                    if (((Query) annotation).required()){
                        throw Utils.parameterError(serviceMethod, i, "不能为空的Query参数(%s)！", queryName);
                    }else {
                        continue;
                    }
                }
                uriVariables.put(queryName, arg);
                uriComponentsBuilder.query(String.format("%s={%s}", queryName, queryName));
            }

        }
        return uriComponentsBuilder.buildAndExpand(uriVariables).toUri();
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public RequestCallback requestCallback(RestTemplate restTemplate) {
        return restTemplate.acceptHeaderRequestCallback(returnType);
    }

    public <T> ResponseExtractor<T> responseExtractor(RestTemplate restTemplate) {
        return new HttpMessageConverterExtractor<T>(returnType, restTemplate.getMessageConverters());
    }

}
