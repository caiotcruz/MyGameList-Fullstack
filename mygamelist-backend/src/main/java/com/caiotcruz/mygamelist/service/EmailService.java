package com.caiotcruz.mygamelist.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void enviarEmailVerificacao(String to, String code) {
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = 
                new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Verifique sua conta no MyGameList 🎮");

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

            helper.setText(htmlContent, true); // O 'true' indica que é HTML
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Falha ao montar e-mail HTML: " + e.getMessage());
        }
    }
}