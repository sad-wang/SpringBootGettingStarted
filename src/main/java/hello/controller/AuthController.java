package hello.controller;

import hello.entity.LoginResult;
import hello.service.AuthService;
import hello.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import java.util.Map;

@Controller
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    @Inject
    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          AuthService authService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.authService = authService;
    }

    @GetMapping("/auth")
    @ResponseBody
    public LoginResult auth() {
        return authService.getCurrentUser()
                .map(LoginResult::success)
                .orElse(LoginResult.success("用户没有登录", false));
    }

    @GetMapping("/auth/logout")
    @ResponseBody
    public LoginResult logout() {
        SecurityContextHolder.clearContext();
        return authService.getCurrentUser()
                .map(user -> LoginResult.failure("用户没有登录"))
                .orElse(LoginResult.failure("注销成功"));
    }

    @PostMapping("/auth/register")
    @ResponseBody
    public LoginResult register(@RequestBody Map<String, String> usernameAndPassword) {
        String username = usernameAndPassword.get("username");
        String password = usernameAndPassword.get("password");
        if (username == null || password == null) {
            return LoginResult.failure("username/password == null");
        }
        if (username.length() < 1 || username.length() > 15) {
            return LoginResult.failure("invalid username");
        }
        if (password.length() < 1 || password.length() > 15) {
            return LoginResult.failure("invalid password");
        }

        try {
            userService.save(username, password);
        } catch (DuplicateKeyException e) {
            return LoginResult.failure("user already exists");
        }
        return LoginResult.success("注册成功", userService.getUserByUsername(username));
    }

    @PostMapping("/auth/login")
    @ResponseBody
    public LoginResult login(@RequestBody Map<String, Object> usernameAndPassword) {
        String username = usernameAndPassword.get("username").toString();
        String password = usernameAndPassword.get("password").toString();

        UserDetails userDetails;
        try {
            userDetails = userService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            return LoginResult.failure("用户不存在");
        }

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());

        try {
            authenticationManager.authenticate(token);

            SecurityContextHolder.getContext().setAuthentication(token);

            return LoginResult.success("登录成功", userService.getUserByUsername(username));
} catch (BadCredentialsException e) {
        return LoginResult.failure("密码不正确");
        }
        }
}
