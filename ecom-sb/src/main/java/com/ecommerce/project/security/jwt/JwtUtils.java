package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import com.ecommerce.project.security.services.UserDetailsServiceImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.nio.file.attribute.UserPrincipal;
import java.security.Key;
import java.util.Date;


@Component
public class JwtUtils {
    private static final Logger logger= LoggerFactory.getLogger(JwtUtils.class);

            @Value("${spring.app.jwtExpirationMs}")
            private int jwtExpirationMs;

            @Value("${spring.app.jwtSecret}")
            private String jwtSecret;

            @Value("${spring.ecom.app.jwtCookieName}")
            private String jwtCookie;


    //Getting JWT From Header


//    public String getJwtFromHeader(HttpServletRequest request){
//        String bearerToken=request.getHeader("Authorization");
//        logger.debug("Authorization Header: {}",bearerToken);//It prints the value of the Authorization header in the console for debugging purposes.
//        if(bearerToken!=null  &&  bearerToken.startsWith("Bearer ")){
//            return bearerToken.substring(7);//Remove Bearer prefix
//        }
//        return null;
//
//    }


//To get Jwt from Cookies
    public String getJwtFromCookies(HttpServletRequest request){
        Cookie cookie=WebUtils.getCookie(request,jwtCookie);
        if(cookie!=null){
            System.out.println("Cookie "+cookie.getValue());
            return cookie.getValue();
        }
        else{
            return null;
        }
    }


    //To create cookies
    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal){
        String jwt=generateTokenFromUserName(userPrincipal.getUsername());
        ResponseCookie cookie= ResponseCookie.from(jwtCookie,jwt)  //Here the cookie will be created with name jwtCookie & value jwt token
                .path("/api")  //Cookie will be sent ONLY for:this endpoint like ex /api/products
                .maxAge(24*60*60)  //Cookie valid for:24 hours
                .httpOnly(false)  //  frontend(Js) can access token can & can cause malicious attack & Your JWT gets stolen so always make it as true
                .build();
        return cookie;
    }

    //To delete cookie used for signout
    public ResponseCookie getCleanJwtCookie(){
        ResponseCookie cookie= ResponseCookie.from(jwtCookie,null)//Here the cookie will be created with name jwtCookie & value jwt token
                .path("/api")
                .maxAge(0)
                .build();
        return cookie;
    }


    //Generating Token from Username

    public String generateTokenFromUserName(String username){
       // String username=userDetails.getUsername();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date().getTime()+jwtExpirationMs)))
                .signWith(key())// It accepts generic Key So no casting needed  Here the Signature will be created
                .compact();
    }
    //Getting Username from JWT Token

    public String getUserNameFromJwtToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())  //Requires specific type  That’s why you had to cast
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }
    //Generate Signing Key

    private Key key(){  //Here the method return type is key(we can convert the return type to SecretKey then no type cast required) but the method actually returning seceretkey object
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));  //Decoders.BASE64.decode(jwtSecret)  this will 1st decode the jwtsecret then it will return byte[] keys then applying Keys.hmacShaKeyFor() it will converted to secretkey object
    }//Key k = Keys.hmacShaKeyFor(...);  here upcasting happening seceretkey object is stored in key refernce
    //Validate JWT Token

    public boolean validateJwtToken(String authToken){
        try{
            System.out.println("Validate");
            Jwts.parser()
                    .verifyWith((SecretKey) key())  //the key() return type is key but it actually holds secretkey object but the verifyWith() requires a specific type so we have to typecaste to secretkey to convince that this key is secretkey,here downcasting happening
                    .build()
                    .parseSignedClaims(authToken);

            return true;

        }catch (MalformedJwtException e){
            logger.error("Invalid JWT token: {}",e.getMessage());
        }
        catch (ExpiredJwtException e){
            logger.error("JWT token is expired: {}",e.getMessage());
        }
        catch (UnsupportedJwtException e){
            logger.error("JWT token is unsupported: {}",e.getMessage());
        }
        catch (IllegalArgumentException e){
            logger.error("JWT claims string is empty: {}",e.getMessage());
        }
        return false;
    }
}
