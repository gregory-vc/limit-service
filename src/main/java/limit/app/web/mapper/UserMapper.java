package limit.app.web.mapper;

import org.springframework.stereotype.Component;
import limit.domain.User;
import limit.app.web.dto.UserResponse;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getUsername()
        );
    }
}
