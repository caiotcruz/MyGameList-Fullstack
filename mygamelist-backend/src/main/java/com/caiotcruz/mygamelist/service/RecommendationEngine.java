package com.caiotcruz.mygamelist.service;

import java.util.*;

/** Deterministic content-based ranking. No persistence, network or shared user state. */
public final class RecommendationEngine {
    private RecommendationEngine() {}
    public record History(long rawgId,List<String> genres,boolean played,int score) {}
    public record Preference(String genre,int played,int rated,double mean,double weight) {}
    public record Candidate(long rawgId,String title,String coverUrl,List<String> genres,double rating,int votes) {}
    public record Pick(long rawgId,String title,String coverUrl,List<String> matchedGenres,double affinity) {}

    public static List<Preference> preferences(List<History> history) {
        Map<String,int[]> stats=new TreeMap<>();
        for(var h:history) {
            if(!h.played()) continue;
            for(String genre:new HashSet<>(h.genres())) {
                var s=stats.computeIfAbsent(genre,k->new int[3]);s[0]++;
                // Zero is the application's sentinel for "not rated".
                if(h.score()>0 && h.score()<=10){s[1]++;s[2]+=h.score();}
            }
        }
        int max=stats.values().stream().mapToInt(s->s[0]).max().orElse(1);
        return stats.entrySet().stream().map(e->{
            int[] s=e.getValue();double smoothed=(s[2]+3.0*6)/(s[1]+3.0);
            double frequency=Math.log1p(s[0])/Math.log1p(max);
            return new Preference(e.getKey(),s[0],s[1],s[1]>0?(double)s[2]/s[1]:0,
                .45*frequency+.55*smoothed/10);
        }).sorted(Comparator.comparingDouble(Preference::weight).reversed().thenComparing(Preference::genre))
          .limit(2).toList();
    }

    public static List<Pick> rank(List<Preference> preferences,List<Candidate> candidates,Set<Long> excluded,int limit) {
        if(preferences.isEmpty()) return List.of();
        double total=preferences.stream().mapToDouble(Preference::weight).sum();
        Map<Long,Pick> unique=new HashMap<>();
        for(var c:candidates) {
            if(excluded.contains(c.rawgId())) continue;
            var matched=preferences.stream().filter(p->c.genres().contains(p.genre())).toList();
            if(matched.isEmpty()) continue;
            double affinity=matched.stream().mapToDouble(Preference::weight).sum()/total;
            double rating=Double.isFinite(c.rating())?Math.max(0,Math.min(5,c.rating())):0;
            int votes=rating>0?Math.max(0,c.votes()):0;
            double quality=(rating*votes+50*3.5)/(votes+50.0)/5;
            double score=.85*affinity+.15*quality;
            var pick=new Pick(c.rawgId(),c.title(),c.coverUrl(),matched.stream().map(Preference::genre).toList(),score);
            unique.merge(c.rawgId(),pick,(a,b)->a.affinity()>=b.affinity()?a:b);
        }
        return unique.values().stream().sorted(Comparator.comparingDouble(Pick::affinity).reversed()
            .thenComparingLong(Pick::rawgId)).limit(Math.max(0,Math.min(12,limit))).toList();
    }
}
