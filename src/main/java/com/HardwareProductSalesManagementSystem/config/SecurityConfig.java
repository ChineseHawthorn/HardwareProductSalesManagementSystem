package com.HardwareProductSalesManagementSystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.HardwareProductSalesManagementSystem.commons.APIRes;
import com.HardwareProductSalesManagementSystem.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;
import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Resource
    private UserDetailsService userDetailsService;

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            // 无状态会话（JWT模式不需要Session）
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling()
                // 未登录时返回JSON
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(401);
                    PrintWriter writer = response.getWriter();
                    writer.write(new ObjectMapper().writeValueAsString(
                            APIRes.fail(401, "未登录或Token已过期，请重新登录")));
                    writer.flush();
                    writer.close();
                })
                // 无权限时返回JSON
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(403);
                    PrintWriter writer = response.getWriter();
                    writer.write(new ObjectMapper().writeValueAsString(
                            APIRes.fail(403, "无权限访问该资源")));
                    writer.flush();
                    writer.close();
                })
            .and()
            .authorizeRequests()
                // 静态资源和公开页面
                .antMatchers(
                    "/", "/index.html", "/category.html", "/404.html",
                    "/css/**", "/js/**", "/images/**",
                    "/pages/**"
                ).permitAll()
                // 认证接口（登录/注册）
                .antMatchers(
                    "/user/login", "/user/regist",
                    "/api/v1/user/login", "/api/v1/user/regist"
                ).permitAll()
                // 商品浏览（消费者公开）
                .antMatchers("GET",
                        "/api/v1/product/list",
                        "/api/v1/product/detail/**",
                        "/api/v1/product/category/list",
                        "/api/v1/product/spec/list",
                        "/api/v1/product/hot",
                        "/api/v1/category/list").permitAll()
                // 其余所有API需要认证
                .antMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            .and()
            .formLogin().disable()
            .httpBasic().disable()
            // 在UsernamePasswordAuthenticationFilter之前插入JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
