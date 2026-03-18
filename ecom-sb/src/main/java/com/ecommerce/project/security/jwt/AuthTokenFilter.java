package com.ecommerce.project.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils  jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    private static final Logger logger= LoggerFactory.getLogger(AuthTokenFilter.class);


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        logger.debug("AuthTokenFilter called for URI: {}",request.getRequestURI());
        try{
            String jwt=parseJwt(request);
            if(jwt!=null  &&  jwtUtils.validateJwtToken(jwt)){
                String username=jwtUtils.getUserNameFromJwtToken(jwt);
                UserDetails userDetails=userDetailsService.loadUserByUsername(username);            //This only fetches username,password,roles from db but it doesn't authenticate the user yet
                UsernamePasswordAuthenticationToken authentication=new UsernamePasswordAuthenticationToken(
                        userDetails,null,userDetails.getAuthorities());                   //This creates authenticated object,Password = null (because already authenticated via token)
                                                                                                    // & fetching roles

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));//Adds extra info like:IP address  Session ID
                SecurityContextHolder.getContext().setAuthentication(authentication);//set into security context this tells spring the user is authenticated
                logger.debug("Roles from JWT: {}",userDetails.getAuthorities());
            }
        }catch (Exception e){
            logger.error("Cannot set user authentication: {}",e);

        }
        filterChain.doFilter(request,response);//This is always required otherwise filtering stops here it tells spring now continue filtering
    }

    private String parseJwt(HttpServletRequest request) {
        String jwt=jwtUtils.getJwtFromHeader(request);
        logger.debug("AuthTokenFilter.java: {}",jwt);
        return jwt;
    }

}
