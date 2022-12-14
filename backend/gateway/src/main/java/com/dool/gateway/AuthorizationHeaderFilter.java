package com.dool.gateway;

import com.netflix.discovery.converters.Auto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;


@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    private static final String SALT = "doolSecret";
    public AuthorizationHeaderFilter() {
        super(Config.class);
    }

    public static class Config {
        // application.yml 파일에서 지정한 filer의 Argument값을 받는 부분
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if(!request.getHeaders().containsKey("token")){
                log.error("token is not found");
                return onError(exchange, "token is not found", HttpStatus.BAD_REQUEST);
            }
            String authorizationHeader = request.getHeaders().get("token").get(0);
            String jwt = authorizationHeader.replace("Bearer", "");
            if(!isJwtValid(jwt)){
                log.error("token is not valid");
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            log.info("token is valid!");
            return chain.filter(exchange);
        }));
    }

    private boolean isJwtValid(String jwt){
        boolean returnValue = true;

        try{
            Jws<Claims> claims = Jwts.parser().setSigningKey(this.generateKey())
                    .parseClaimsJws(jwt);
        }catch(Exception e) {
            e.printStackTrace();
            returnValue = false;
        }
        System.out.println(returnValue);
        return returnValue;
    }

    private byte[] generateKey() {
        byte[] key = null;
        try {
            //byte 코드로 인코딩 해주기
            //캐릭터셋 인자로 안주면 사용자 플랫폼의 기본 인코딩 설정대로 인코딩
            key = SALT.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return key;
    }

    private Mono onError(ServerWebExchange exchange, String err, HttpStatus httpStatus){
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().set("message",err);
        byte[] bytes = err.getBytes(StandardCharsets.UTF_8);    
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer));
    }

    public String get(String jwt, String key) {
        Jws<Claims> claims = null;
        try {
            claims = Jwts.parser().setSigningKey(SALT.getBytes("UTF-8")).parseClaimsJws(jwt);
        } catch (Exception e) {
            e.getMessage();
        }
        Map<String, Object> value = claims.getBody();
        System.out.println("claims :" + claims);
        return (String)value.get(key);
    }

}