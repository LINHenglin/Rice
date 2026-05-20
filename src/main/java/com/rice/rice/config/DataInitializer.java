package com.rice.rice.config;

import com.rice.rice.entity.User;
import com.rice.rice.mapper.UserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.findByUsername("admin").isEmpty()) {
            // 开发/演示环境默认创建管理员账号，便于直接进入管理端联调。
            // 生产环境建议移除或改为受控的初始化方式（例如仅在特定 profile 下启用）。
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("admin");
            admin.setDisabled(false);
            userMapper.save(admin);
        }
    }
}
