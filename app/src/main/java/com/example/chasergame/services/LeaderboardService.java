package com.example.chasergame.services;

import com.example.chasergame.models.LeaderboardEntry;
import com.example.chasergame.models.User;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardService {
    private final IUserRepository userRepository;

    public LeaderboardService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void loadAllLeaderboards(LeaderboardCallback callback) {
        userRepository.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users == null) {
                    callback.onFailed(new Exception("No users found"));
                    return;
                }

                List<LeaderboardEntry> rhythm = new ArrayList<>();
                List<LeaderboardEntry> online = new ArrayList<>();
                List<LeaderboardEntry> botEasy = new ArrayList<>();
                List<LeaderboardEntry> botNormal = new ArrayList<>();
                List<LeaderboardEntry> botHard = new ArrayList<>();
                List<LeaderboardEntry> oneDevice = new ArrayList<>();

                for (User user : users) {
                    addIfGreater(rhythm, user.getUsername(), user.getTotalRhythmScore());
                    addIfGreater(online, user.getUsername(), user.getOnlineWins());
                    addIfGreater(botEasy, user.getUsername(), user.getBotWinsEasy());
                    addIfGreater(botNormal, user.getUsername(), user.getBotWinsNormal());
                    addIfGreater(botHard, user.getUsername(), user.getBotWinsHard());
                    addIfGreater(oneDevice, user.getUsername(), user.getOneDeviceWins());
                }

                sortAndRank(rhythm);
                sortAndRank(online);
                sortAndRank(botEasy);
                sortAndRank(botNormal);
                sortAndRank(botHard);
                sortAndRank(oneDevice);

                callback.onDataReady(rhythm, online, botEasy, botNormal, botHard, oneDevice);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }

    private void addIfGreater(List<LeaderboardEntry> list, String name, int score) {
        if (score > 0) {
            list.add(new LeaderboardEntry(0, name, score));
        }
    }

    private void sortAndRank(List<LeaderboardEntry> leaderboard) {
        leaderboard.sort((e1, e2) -> Integer.compare(e2.getScore(), e1.getScore()));
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }
    }

    public interface LeaderboardCallback {
        void onDataReady(List<LeaderboardEntry> rhythm, List<LeaderboardEntry> online,
                         List<LeaderboardEntry> botEasy, List<LeaderboardEntry> botNormal,
                         List<LeaderboardEntry> botHard, List<LeaderboardEntry> oneDevice);

        void onFailed(Exception e);
    }
}
