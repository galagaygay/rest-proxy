package com.vwmin.restproxy;

import com.vwmin.restproxy.annotations.Body;
import com.vwmin.restproxy.annotations.Query;
import jdk.nashorn.internal.objects.annotations.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.util.logging.Logger;

/**
 * @author vwmin
 * @version 1.0
 * @date 2020/4/6 11:31
 */
public class RestRequestFactory {

    private static final Log logger = LogFactory.getLog(RestRequestFactory.class);

    private final String url;
    private final HttpMethod httpMethod;
    private final Annotation[] parameterAnnotations;
    private final Class<?> returnType;
    private final Method serviceMethod;
    private Object requestBody;

    private boolean logRequest = false;

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

            //Query
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

            //Body
            else if (annotation instanceof Body){
                String queryName = ((Body) annotation).value();
                if (arg == null){
                    if (((Body) annotation).required()){
                        throw Utils.parameterError(serviceMethod, i, "不能为空的Body参数(%s)！", queryName);
                    }else {
                        continue;
                    }
                }
                this.requestBody = arg;
            }

        }

        URI uri = uriComponentsBuilder.buildAndExpand(uriVariables).toUri();

        if (logRequest){
            logger.info("going to request >>> " + uri.toString());
        }

        return uri;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public RequestCallback requestCallback(RestTemplate restTemplate) {
        switch (httpMethod){
            case GET:
                return restTemplate.acceptHeaderRequestCallback(returnType);
            case POST:
                return restTemplate.httpEntityCallback(requestBody, returnType);
            default:
                return restTemplate.acceptHeaderRequestCallback(returnType);

        }
    }

    public <T> ResponseExtractor<T> responseExtractor(RestTemplate restTemplate) {
        return new HttpMessageConverterExtractor<T>(returnType, restTemplate.getMessageConverters());
    }

    public void setLogRequest(boolean logRequest) {
        this.logRequest = logRequest;
    }
}
