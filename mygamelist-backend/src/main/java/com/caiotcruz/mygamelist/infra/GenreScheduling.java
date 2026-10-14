package com.caiotcruz.mygamelist.infra;
@org.springframework.context.annotation.Configuration
public class GenreScheduling {
    @org.springframework.context.annotation.Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler genreScheduler(){
        var scheduler=new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);scheduler.setThreadNamePrefix("genre-metadata-");return scheduler;
    }
    // Keep mail/outbox tasks separate from external catalog requests.
    @org.springframework.context.annotation.Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler(){
        var scheduler=new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);scheduler.setThreadNamePrefix("app-scheduling-");return scheduler;
    }
}
