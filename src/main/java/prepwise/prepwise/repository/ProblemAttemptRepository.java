package prepwise.prepwise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import prepwise.prepwise.entity.ProblemAttempt;
import prepwise.prepwise.entity.User;

import java.util.List;

public interface ProblemAttemptRepository extends JpaRepository<ProblemAttempt, Long>, JpaSpecificationExecutor<ProblemAttempt> {
    List<ProblemAttempt> findByUserOrderBySolvedAtDesc(User user);
    List<ProblemAttempt> findTop5ByUserOrderBySolvedAtDesc(User user);
    long countByUser(User user);
}
