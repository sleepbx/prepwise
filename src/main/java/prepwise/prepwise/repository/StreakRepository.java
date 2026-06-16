package prepwise.prepwise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import prepwise.prepwise.entity.Streak;
import prepwise.prepwise.entity.User;

import java.util.Optional;

public interface StreakRepository extends JpaRepository<Streak, Long> {
    Optional<Streak> findByUser(User user);
}
