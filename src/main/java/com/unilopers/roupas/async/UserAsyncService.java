package com.unilopers.roupas.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * UserAsyncService — Serviço assíncrono de notificação real pós-cadastro.
 *
 * <p>Este serviço agora utiliza {@link JavaMailSender} para o envio de e-mails reais
 * via SMTP configurado no {@code application.yaml}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAsyncService {

    private final JavaMailSender mailSender; // Injetado automaticamente pelo Spring Boot Mail Starter

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ==============================
    // PUBLIC ASYNC METHODS
    // ==============================

    /**
     * Dispara, de forma assíncrona, a notificação real pós-cadastro do usuário.
     *
     * @param userId    UUID do usuário recém-criado
     * @param userName  Nome do usuário
     * @param userEmail E-mail do usuário
     */
    @Async("userTaskExecutor")
    public void notifyUserCreated(UUID userId, String userName, String userEmail) {
        try {
            log.info("[UserAsync] Iniciando processamento de envio de e-mail REAL. userId={}", userId);

            validateInput(userId, userName, userEmail);

            sendWelcomeEmail(userId, userName, userEmail);
            registerNotificationLog(userId, userName, userEmail);

            log.info("[UserAsync] E-mail enviado com sucesso. userId={}", userId);

        } catch (IllegalArgumentException e) {
            log.warn("[UserAsync] Dados inválidos — notificação cancelada. userId={}, motivo={}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("[UserAsync] FALHA CRÍTICA ao enviar e-mail. Verifique as credenciais SMTP. userId={}", userId, e);
        }
    }

    // ==============================
    // PRIVATE HELPERS
    // ==============================

    private void validateInput(UUID userId, String userName, String userEmail) {
        if (userId == null) throw new IllegalArgumentException("userId nulo");
        if (userName == null || userName.isBlank()) throw new IllegalArgumentException("userName vazio");
        if (userEmail == null || userEmail.isBlank()) throw new IllegalArgumentException("userEmail vazio");
    }

    /**
     * Realiza o envio do e-mail de boas-vindas utilizando JavaMailSender.
     */
    private void sendWelcomeEmail(UUID userId, String userName, String userEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setTo(userEmail);
        message.setSubject("Bem-vindo(a) ao sistema Roupas Unilopers!");
        message.setText(String.format(
            "Olá %s!\n\nSeu cadastro foi realizado com sucesso.\nID do Usuário: %s\n\nAtenciosamente,\nEquipe Louis Vittao",
            userName, userId
        ));

        // Envia o e-mail (bloqueia apenas esta thread secundária do executor assíncrono)
        mailSender.send(message);
        
        log.info("[UserAsync] SimpleMailMessage entregue ao servidor SMTP para {}", userEmail);
    }

    /**
     * Registra a notificação no log do sistema.
     */
    private void registerNotificationLog(UUID userId, String userName, String userEmail) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        log.info("[UserAsync] [LOG DE ENVIO]");
        log.info("[UserAsync]   Destinatário : {} <{}>", userName, userEmail);
        log.info("[UserAsync]   Timestamp    : {}", timestamp);
    }
}
