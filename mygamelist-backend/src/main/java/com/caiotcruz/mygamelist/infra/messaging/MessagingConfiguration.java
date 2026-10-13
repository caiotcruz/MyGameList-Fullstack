package com.caiotcruz.mygamelist.infra.messaging;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.amqp.core.*;
@Configuration @EnableScheduling
public class MessagingConfiguration {
 public static final String QUEUE="mgl.mail.v1";
 @Bean public Queue mailQueue() { return QueueBuilder.durable(QUEUE).withArgument("x-max-length",10000).withArgument("x-overflow","reject-publish").build(); }
}
