package com.caiotcruz.mygamelist.infra.security;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
@Component
public class SecurityEvents {
    private final MeterRegistry metrics;
    private final AtomicLong nextLog=new AtomicLong();
    public SecurityEvents(MeterRegistry metrics){this.metrics=metrics;}
    // Callers supply fixed event names only; never IPs, paths, emails or tokens as metric labels.
    public void record(String event){
        metrics.counter("mgl.security.events","event",event).increment();
        long now=System.currentTimeMillis(),next=nextLog.get();
        if(now>=next && nextLog.compareAndSet(next,now+10000))
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Security event={} (logs sampled; counters are complete)",event);
    }
}
