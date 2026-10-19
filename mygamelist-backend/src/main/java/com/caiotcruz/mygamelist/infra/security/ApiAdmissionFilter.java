package com.caiotcruz.mygamelist.infra.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.json.JsonMapper;
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+10)
public class ApiAdmissionFilter extends OncePerRequestFilter {
    private final RateLimits limits;
    private final ClientAddress address;
    private final SecurityEvents events;
    private final Semaphore slots;
    private final int globalRate;
    @Value("${app.cors-origins:http://localhost:4200}") private String allowedOrigins="http://localhost:4200";
    @Value("${management.server.port:9091}") private int managementPort;
    private final JsonMapper json=JsonMapper.builder(JsonFactory.builder()
        .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(24).maxStringLength(200000).maxNumberLength(32).build())
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build()).enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    public ApiAdmissionFilter(RateLimits limits,SecurityEvents events,ClientAddress address,@Value("${app.security.max-inflight:32}") int maxInflight,
        @Value("${app.security.requests-per-second:120}") int globalRate){
        if(maxInflight<1||maxInflight>128||globalRate<1)throw new IllegalArgumentException("Invalid admission limits");
        this.address=address;this.limits=limits;this.events=events;this.slots=new Semaphore(maxInflight);this.globalRate=globalRate;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest req){return req.getLocalPort()==managementPort;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws IOException,ServletException {
        res.setHeader("X-Request-ID",UUID.randomUUID().toString());res.setHeader("X-Content-Type-Options","nosniff");
        res.setHeader("Referrer-Policy","no-referrer");res.setHeader("Cache-Control","no-store");
        if(!slots.tryAcquire()){addRejectionCorsHeaders(req,res);events.record("inflight_rejected");SecurityResponses.reject(res,new SecurityRejection(503,"BUSY","Servidor ocupado. Tente novamente.",2));return;}
        try {
            limits.check("global","all",globalRate,1);
            // Only explicit proxy peers may supply X-Real-IP; X-Forwarded-For is ignored.
            limits.check("ip_burst",address.of(req),80,10);
            limits.check("ip_minute",address.of(req),600,60);
            if(!Set.of("GET","HEAD","POST","PUT","PATCH","DELETE","OPTIONS").contains(req.getMethod()))
                throw new SecurityRejection(405,"METHOD_NOT_ALLOWED","Método não permitido.",0);
            String path=req.getRequestURI();
            if(path.contains("%")||path.contains(";")||path.contains("\\")||path.contains("//"))
                throw new SecurityRejection(400,"INVALID_PATH","Caminho inválido.",0);
            if(req.getRequestURI().length()>1024 || (req.getQueryString()!=null && req.getQueryString().length()>2048))
                throw new SecurityRejection(414,"URI_TOO_LONG","Endereço de requisição muito longo.",0);
            String auth=req.getHeader("Authorization");
            if(auth!=null && auth.length()>4096)throw new SecurityRejection(431,"HEADERS_TOO_LARGE","Cabeçalhos muito grandes.",0);
            if(req.getContentLengthLong()>RequestLimits.MAX_BODY)throw tooLarge();
            String encoding=req.getHeader("Content-Encoding");
            if(encoding!=null && !encoding.equalsIgnoreCase("identity"))throw new SecurityRejection(415,"UNSUPPORTED_ENCODING","Compressão de requisições não suportada.",0);
            HttpServletRequest bounded=req;
            if(RequestLimits.write(req.getMethod())) {
                byte[] bytes=readBounded(req.getInputStream());
                if(bytes.length>RequestLimits.MAX_BODY)throw tooLarge();
                if(bytes.length>0){
                    String type=req.getContentType();
                    if(type==null||!type.split(";",2)[0].trim().equalsIgnoreCase("application/json"))
                        throw new SecurityRejection(415,"JSON_REQUIRED","Envie os dados em JSON.",0);
                    try{
                        var root=json.readTree(bytes);
                        if(root==null || !root.isObject())throw new IllegalArgumentException();
                        var email=root.get("email");
                        if(email!=null && email.isString() && email.asString().length()<=254)
                            req.setAttribute("mgl.auth.email",RequestLimits.email(email.asString()));
                    }catch(Exception e){throw new SecurityRejection(400,"INVALID_JSON","JSON inválido, duplicado ou excessivamente complexo.",0);}
                }
                bounded=new BodyRequest(req,bytes);
            }
            chain.doFilter(bounded,res);
        }catch(SecurityRejection e){addRejectionCorsHeaders(req,res);events.record(e.code);SecurityResponses.reject(res,e);}
        finally{slots.release();}
    }
    // Normal requests must reach Spring CORS without a pre-set Allow-Origin header.
    // Only responses terminated here need manual CORS headers for the allowed frontend.
    private void addRejectionCorsHeaders(HttpServletRequest req,HttpServletResponse res) {
        String origin=req.getHeader("Origin");
        if(origin!=null && Arrays.stream(allowedOrigins.split(",")).map(String::trim).anyMatch(origin::equals)){
            res.setHeader("Access-Control-Allow-Origin",origin);
            res.addHeader("Vary","Origin");
            res.setHeader("Access-Control-Expose-Headers","Retry-After,X-Request-ID");
        }
    }
    private byte[] readBounded(InputStream input)throws IOException {
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        var output=new ByteArrayOutputStream();byte[] buffer=new byte[8192];
        while(output.size()<=RequestLimits.MAX_BODY){
            if(System.nanoTime()>deadline)throw new SecurityRejection(408,"BODY_TIMEOUT","Tempo de envio excedido.",0);
            int read=input.read(buffer,0,Math.min(buffer.length,RequestLimits.MAX_BODY+1-output.size()));
            if(read<0)break;output.write(buffer,0,read);
        }
        return output.toByteArray();
    }
    private SecurityRejection tooLarge(){return new SecurityRejection(413,"PAYLOAD_TOO_LARGE","Conteúdo maior que o limite permitido.",0);}
    static final class BodyRequest extends HttpServletRequestWrapper {
        private final byte[] bytes;
        BodyRequest(HttpServletRequest request,byte[] bytes){super(request);this.bytes=bytes;}
        @Override public ServletInputStream getInputStream(){
            var input=new ByteArrayInputStream(bytes);
            return new ServletInputStream(){public int read(){return input.read();}public boolean isFinished(){return input.available()==0;}
                public boolean isReady(){return true;}public void setReadListener(ReadListener listener){throw new UnsupportedOperationException("Synchronous JSON endpoint");}};
        }
        @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}
    }
}
