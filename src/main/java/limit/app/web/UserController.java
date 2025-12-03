package limit.app.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import limit.app.exception.UserNotFoundException;
import limit.app.service.UserService;
import limit.app.web.dto.UserResponse;
import limit.app.web.mapper.UserMapper;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable("id") long id) {
        return userService.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(
                        "User %d not found".formatted(id)
                ));
    }
}
