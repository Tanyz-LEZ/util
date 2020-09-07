package com.lez.rank;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;

/**
 * @author tanyz
 * @date 2020-09-07 07:28:00
 */
public class RedBlackTreeRankImplTest {
    private Function<UserRankEntry, Integer> keyExtractor = UserRankEntry::getUserId;
    private Comparator<UserRankEntry> comparator = Comparator.comparing(UserRankEntry::getRankScore).thenComparing(UserRankEntry::getUserId);
    private Function<UserRankEntry, UserRankEntry> valueCopier = UserRankEntry::new;

    private RedBlackTreeRankImpl<Integer, UserRankEntry> redBlackTreeRank = new RedBlackTreeRankImpl<>(comparator, valueCopier,keyExtractor);
    private SimpleRank simpleRank = new SimpleRank(comparator);
    private Set<Integer> userIdSet = new HashSet<>();

    private int length = 100;

    @Before
    public void prepareData() {
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int userId = i + 1;
            double score = random.nextDouble() * length;
            userIdSet.add(userId);
            redBlackTreeRank.put(userId, new UserRankEntry(userId, score));
            simpleRank.put(userId, new UserRankEntry(userId, score));
        }

    }

    @Test
    public void testGet() {
        for (Integer userId : userIdSet) {
            Assert.assertEquals(redBlackTreeRank.get(userId), simpleRank.get(userId));
        }
    }

    @Test
    public void testGetRank() {
        for (Integer userId : userIdSet) {
            Assert.assertEquals(redBlackTreeRank.getRank(userId), simpleRank.getRank(userId));
        }
    }

    @Test
    public void testRankSize() {
        Assert.assertEquals(redBlackTreeRank.rankSize(), simpleRank.rankSize());
    }

    @Test
    public void testRankIn() {
        for (int i = 1; i <= userIdSet.size(); i++) {
            Assert.assertEquals(redBlackTreeRank.rankIn(i), simpleRank.rankIn(i));
        }
    }

    @Test
    public void testPut() {
        Random random = new Random();

        //add new data
        for (int userId = length + 1; userId < length / 2; userId++) {
            userIdSet.add(userId);
            double score = random.nextDouble() * length;
            redBlackTreeRank.put(userId, new UserRankEntry(userId, score));
            simpleRank.put(userId, new UserRankEntry(userId, score));
            testAfterModified();
        }
        length = userIdSet.size();

        //modify
        for (Integer userId : userIdSet) {
            {
                UserRankEntry entry = redBlackTreeRank.get(userId);
                entry.setRankScore(entry.getRankScore() * random.nextDouble());
                redBlackTreeRank.put(entry.getUserId(), entry);
            }
            {
                UserRankEntry entry1 = simpleRank.get(userId);
                entry1.setRankScore(entry1.getRankScore() * random.nextDouble());
                simpleRank.put(entry1.getUserId(), entry1);
                simpleRank.put(entry1.getUserId(), entry1);
            }
        }
    }

    @Test
    public void testRemove() {
        for (Integer userId : userIdSet) {
            redBlackTreeRank.remove(userId);
            simpleRank.remove(userId);

            testAfterModified();
        }
    }

    @Test
    public void testRankRange() {
        Random random = new Random();
        for (int i = 0; i < userIdSet.size(); i++) {
            int from = random.nextInt(userIdSet.size());
            int to = random.nextInt(userIdSet.size());
            if (from >= to || from < 1) {
                break;
            }

            Assert.assertEquals(redBlackTreeRank.rankRange(from, to), simpleRank.rankRange(from, to));
        }
    }

    private void testAfterModified() {
        testGet();
        testGetRank();
        testRankSize();
        testRankIn();
    }
}

class UserRankEntry {
    private int userId;
    private double rankScore;

    public UserRankEntry(UserRankEntry entry) {
        this.userId = entry.userId;
        this.rankScore = entry.rankScore;
    }

    public UserRankEntry(int userId, double rankScore) {
        this.userId = userId;
        this.rankScore = rankScore;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getRankScore() {
        return rankScore;
    }

    public void setRankScore(double rankScore) {
        this.rankScore = rankScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRankEntry entry = (UserRankEntry) o;
        return userId == entry.userId &&
                Double.compare(entry.rankScore, rankScore) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, rankScore);
    }

    @Override
    public String toString() {
        return "Entry{" +
                "userId=" + userId +
                ", rankScore=" + rankScore +
                '}';
    }
}

class SimpleRank {
    List<UserRankEntry> rankList = new LinkedList<>();
    private Comparator<UserRankEntry> comparator;

    public SimpleRank(Comparator<UserRankEntry> comparator) {
        this.comparator = comparator;
    }

    public void sort() {
        rankList.sort(comparator);
    }

    public UserRankEntry get(int userId) {
        for (UserRankEntry userRankEntry : rankList) {
            if (userRankEntry.getUserId() == userId) {
                return userRankEntry;
            }
        }
        return null;
    }

    public Integer getRank(int userId) {
        boolean contains = false;
        int result = 0;
        for (UserRankEntry userRankEntry : rankList) {
            result += 1;
            if (userRankEntry.getUserId() == userId) {
                contains = true;
                break;
            }
        }
        return contains ? result : null;
    }

    public int rankSize() {
        return rankList.size();
    }

    public Integer rankIn(int n) {
        if (n > rankList.size() || n < 1) {
            return null;
        }

        Iterator<UserRankEntry> iterator = rankList.iterator();
        UserRankEntry result = null;
        for (int i = 0; i < n; i++) {
            result = iterator.next();
        }

        return result == null ? null : result.getUserId();
    }

    public List<UserRankEntry> rankRange(int fromInclusive, int toExclusive) {
        return rankList.subList(fromInclusive, toExclusive);
    }

    public UserRankEntry put(int key, UserRankEntry value) {
        if (value.getUserId() != key) {
            throw new RuntimeException();
        }

        UserRankEntry entry = get(key);
        UserRankEntry result;
        if (entry == null) {
            entry = value;
            result = new UserRankEntry(entry);
            rankList.add(entry);
        } else {
            result = new UserRankEntry(entry);
            entry.setRankScore(value.getRankScore());
        }
        sort();
        return result;
    }

    public UserRankEntry remove(int userId) {
        Iterator<UserRankEntry> iterator = rankList.iterator();
        while (iterator.hasNext()) {
            UserRankEntry next = iterator.next();
            if (next.getUserId() == userId) {
                iterator.remove();
                return next;
            }
        }
        return null;
    }
}