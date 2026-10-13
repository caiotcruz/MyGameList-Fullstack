package com.caiotcruz.mygamelist.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final com.caiotcruz.mygamelist.infra.messaging.MailOutbox outbox;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailService(com.caiotcruz.mygamelist.infra.messaging.MailOutbox outbox) {
        this.outbox = outbox;
    }

    public void enviarEmailVerificacao(String to, String code) {
        String htmlContent = """
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #121212; color: #ffffff; padding: 40px; text-align: center; border-radius: 15px;">
                <h1 style="color: #6200ea; font-size: 28px;">Bem-vindo ao MyGameList!</h1>
                <p style="font-size: 16px; color: #b0b0b0;">Falta pouco para você organizar sua coleção. Use o código abaixo para validar seu e-mail:</p>
                <div style="background-color: #1e1e1e; border: 2px dashed #6200ea; display: inline-block; padding: 20px 40px; margin: 20px 0; border-radius: 10px;">
                    <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #00e5ff;">%s</span>
                </div>
                <p style="font-size: 12px; color: #666;">Este código expira em 15 minutos.</p>
                <hr style="border: 0; border-top: 1px solid #333; margin: 30px 0;">
                <p style="font-size: 11px; color: #444;">MyGameList © 2026 - Desenvolvido com ❤️ por Caio Teixeira</p>
            </div>
            """.formatted(code);

        sendHtmlEmail(to, "Verifique sua conta no MyGameList 🎮", htmlContent, 15);
    }

    public void enviarEmailRecuperacao(String to, String token) {
        String link = frontendUrl + "/reset-password?token=" + token + "&email="
                + java.net.URLEncoder.encode(to, java.nio.charset.StandardCharsets.UTF_8);

        String htmlContent = """
            <div style="font-family: sans-serif; background-color: #121212; color: #ffffff; padding: 40px; text-align: center;">
                <h2 style="color: #6200ea;">Recuperar Senha</h2>
                <p>Clique no botão abaixo para definir uma nova senha para sua conta:</p>
                <a href="%s" style="background-color: #6200ea; color: white; padding: 15px 25px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; margin: 20px 0;">
                    Redefinir Senha
                </a>
                <p style="font-size: 12px; color: #666;">Este link expira em 1 hora.</p>
            </div>
            """.formatted(link);

        sendHtmlEmail(to, "Recuperação de Senha - MyGameList 🔑", htmlContent, 60);
    }

    private void sendHtmlEmail(String to, String subject, String html, int minutes) {
        outbox.enqueue(to, subject, html, minutes);
    }
}
