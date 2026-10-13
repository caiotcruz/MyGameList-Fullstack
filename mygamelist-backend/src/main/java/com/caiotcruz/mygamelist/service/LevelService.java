package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.enums.ExperienceSource;
import com.caiotcruz.mygamelist.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@org.springframework.transaction.annotation.Transactional
public class LevelService {
    private final UserLocks locks;


    private static final int EXP_PER_LEVEL = 1000;
    private static final int MIN_LEVEL = 1;

    private final UserRepository userRepository;

    public LevelService(UserRepository userRepository, UserLocks locks) {
        this.locks = locks;
        this.userRepository = userRepository;
    }

    public void grant(User user, ExperienceSource source) {
        applyDelta(user, source.getAmount());
    }

    public void revoke(User user, ExperienceSource source) {
        applyDelta(user, -source.getAmount());
    }

    @org.springframework.transaction.annotation.Transactional
    public void applyDelta(User user, int amount) {
        if (amount == 0) return;
        locks.lock(user.getId());
        user = userRepository.findById(user.getId()).orElseThrow();

        int totalExp = user.getExperience() + amount;
        int level = user.getLevel();

        while (totalExp >= EXP_PER_LEVEL) {
            totalExp -= EXP_PER_LEVEL;
            level++;
        }

        while (totalExp < 0) {
            if (level <= MIN_LEVEL) {
                totalExp = 0;
                level = MIN_LEVEL;
                break;
            }
            level--;
            totalExp += EXP_PER_LEVEL;
        }

        user.setExperience(totalExp);
        user.setLevel(level);
        userRepository.saveAndFlush(user);
    }
}