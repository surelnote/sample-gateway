package com.nuri.lguplus.authorization;

import java.io.UnsupportedEncodingException;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import com.auth0.jwt.exceptions.JWTVerificationException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;


@Component
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {
	
	public AuthorizationFilter() {
		super(Config.class);
	}
	
	public static class Config {
		// Put the configuration properties
	}
	
	// --> 이후 ConfigMap 처리
	@Value("${jwt.key}")
	private String key;
	
	@Value("${jwt.salt}")
	private String SALT;

	public GatewayFilter apply(Config config) {

		return (exchange, chain) -> {
			
			String token = this.extractJWTToken(exchange.getRequest());
			HashMap<String,?> userMap = this.getUserInfo(token);
			String userId = (String)userMap.get("userId");
			
			/** 인증키에 대한 redis 데이터 정합성 체크 */
			this.checkRedisData(token, userId);
			
			return chain.filter(exchange);
		};
	}

	private String extractJWTToken(ServerHttpRequest request) {
		
        if (!request.getHeaders().containsKey("Authorization")) {
            throw new JWTVerificationException("Authorization header is missing");
        }

        List<String> headers = request.getHeaders().get("Authorization");
        if (headers.isEmpty()) {
            throw new JWTVerificationException("Authorization header is empty");
        }

        String credential = headers.get(0).trim();
        String[] components = credential.split("\\s");

        if (components.length != 2) {
            throw new JWTVerificationException("Malformat Authorization content");
        }

        if (!components[0].equals("Bearer")) {
            throw new JWTVerificationException("Bearer is needed");
        }

        return components[1].trim();
    }
	
	public HashMap<String,?> getUserInfo(String jwt) {
		try{
			Jws<Claims> claims = Jwts.parser()
					  .setSigningKey(this.generateKey())
					  .parseClaimsJws(jwt);
			HashMap<String,?> hm = (HashMap)claims.getBody().get(this.key);
			return hm;
			
		} catch (ExpiredJwtException e) {
			throw new ExpiredJwtException(null, null, "유효기간이 만료되었습니다. \n다시 로그인해 주세요.");
		} 
	}
	
	private byte[] generateKey() {
		byte[] key = null;
		try {
			key = SALT.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.out.println("");
		}

		return key;
	}
	
	private void checkRedisData(String token, String userId) {
		// TODO Redis에 해당 값이 있는지 체크
		// 없으면 세션 타임 아웃으로 로그인 페이지로 이동하게 처리
		// 있으면 token 값과 일치하는지 체크
		// 다를경우 세션 타임 아웃으로 로그인 페이지로 이동하게 처리
		
		// throw new UnauthorizedException();
	}
	
}