package prepwise.prepwise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prepwise.prepwise.entity.Streak;
import prepwise.prepwise.entity.User;
import prepwise.prepwise.repository.StreakRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final StreakRepository streakRepository;

    @Transactional
    public Streak updateStreak(User user, LocalDate solvedDate) {
        Streak streak = streakRepository.findByUser(user)
                .orElseGet(() -> streakRepository.save(Streak.builder().user(user).build()));

        LocalDate last = streak.getLastSolvedDate();
        if (last == null) {
            streak.setCurrentStreak(1);
            streak.setLongestStreak(1);
        } else if (last.equals(solvedDate)) {
            // solved today already — no streak change
        } else if (last.equals(solvedDate.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            streak.setLongestStreak(Math.max(streak.getLongestStreak(), streak.getCurrentStreak()));
        } else {
            streak.setCurrentStreak(1);
            streak.setLongestStreak(Math.max(streak.getLongestStreak(), 1));
        }
        streak.setLastSolvedDate(solvedDate);
        return streakRepository.save(streak);
    }

    public Streak getStreak(User user) {
        return streakRepository.findByUser(user)
                .orElse(Streak.builder().user(user).currentStreak(0).longestStreak(0).build());
    }
}
