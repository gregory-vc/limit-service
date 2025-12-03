package limit.app.service;

import limit.app.repository.UserRepository;
import limit.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User create(User user) {
        return repo.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(long id) {
        return repo.findById(id);
    }

    public User update(User patch) {
        var user = repo.findById(patch.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found " + patch.getId()));

        if (patch.getUsername() != null) {
            user.setUsername(patch.getUsername());
        }

        return repo.save(user);
    }

    public void deleteById(long id) {
        repo.deleteById(id);
    }
}
