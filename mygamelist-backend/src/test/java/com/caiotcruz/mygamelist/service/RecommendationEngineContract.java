package com.caiotcruz.mygamelist.service;
import java.util.*;
import com.caiotcruz.mygamelist.service.RecommendationEngine.*;
/** Runnable with the JDK alone, and also called by the JUnit suite. */
public class RecommendationEngineContract {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 public static void main(String[] args){run();System.out.println("8 recommendation contracts passed");}
 public static void run(){
  check(RecommendationEngine.preferences(List.of()).isEmpty(),"empty history");
  var history=List.of(new History(1,List.of("action","action"),true,9),new History(2,List.of("action"),true,8),
    new History(3,List.of("puzzle"),true,10),new History(4,List.of("rpg"),false,10));
  var p=RecommendationEngine.preferences(history);
  check(p.size()==2 && p.get(0).genre().equals("action") && p.get(0).played()==2,"frequency, duplicates and plan-to-play");
  check(Math.abs(p.get(0).weight()-(.45+.55*7/10))<1e-9,"explicit smoothed formula");
  check(RecommendationEngine.preferences(List.of(new History(1,List.of("puzzle"),true,0))).get(0).rated()==0,"zero is unrated");
  var candidates=List.of(new Candidate(10,"A",null,List.of("action"),4.5,100),new Candidate(10,"A",null,List.of("action"),4.5,100),
   new Candidate(11,"B",null,List.of("action","puzzle"),4,100),new Candidate(12,"C",null,List.of("other"),5,1000));
  var picks=RecommendationEngine.rank(p,candidates,Set.of(),12);
  check(picks.size()==2 && picks.get(0).rawgId()==11,"matches, deduplication and multi-genre affinity");
  check(RecommendationEngine.rank(p,candidates,Set.of(10L,11L),12).isEmpty(),"all listed games excluded");
  var tie=RecommendationEngine.rank(p,List.of(new Candidate(22,"B",null,List.of("action"),4,50),new Candidate(21,"A",null,List.of("action"),4,50)),Set.of(),1);
  check(tie.size()==1 && tie.get(0).rawgId()==21,"deterministic ties and cap");
  var rated=RecommendationEngine.preferences(List.of(new History(1,List.of("a"),true,10),new History(2,List.of("b"),true,2)));
  check(rated.get(0).genre().equals("a"),"user ratings influence preferences");
 }
}
