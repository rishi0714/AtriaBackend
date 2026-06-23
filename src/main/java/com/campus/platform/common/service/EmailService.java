package com.campus.platform.common.service;

import com.campus.platform.registration.entity.Registration;
import com.campus.platform.registration.repository.RegistrationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final QrCodeService qrCodeService;
    private final RegistrationRepository registrationRepository;

    @Async
    public void sendRegistrationEmail(UUID registrationId) {
        try {
            Registration registration = registrationRepository
                    .findByIdWithDetails(registrationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Registration not found: " + registrationId));

            byte[] qrBytes = qrCodeService.generateQrBytes(registration.getQrCode());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(registration.getUser().getEmail());
            helper.setSubject("You're registered! – " + registration.getEvent().getTitle());

            String eventDate = registration.getEvent().getEventDate()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

            helper.setText("""
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background-color:#EBD5AB;font-family:Arial,sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#EBD5AB;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <table width="560" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(27,33,26,0.15);">
                                    <tr>
                                        <td style="background-color:#1B211A;padding:40px 40px 30px;text-align:center;">
                                            <h1 style="margin:0;color:#8BAE66;font-size:26px;font-weight:700;letter-spacing:1px;">CAMPUS CONNECT</h1>
                                            <p style="margin:8px 0 0;color:#628141;font-size:13px;letter-spacing:2px;text-transform:uppercase;">Event Registration</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#1B211A;padding:0 40px 32px;text-align:center;">
                                            <div style="display:inline-block;background-color:#628141;border-radius:50px;padding:10px 28px;">
                                                <span style="color:#EBD5AB;font-weight:700;font-size:14px;letter-spacing:1px;">✓ REGISTRATION CONFIRMED</span>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#1B211A;height:20px;"></td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#f7f5f0;padding:32px 40px 8px;text-align:center;">
                                            <h2 style="margin:0 0 6px;color:#1B211A;font-size:22px;font-weight:700;">You're in, %s!</h2>
                                            <p style="margin:0;color:#628141;font-size:14px;">Your spot has been reserved.</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#f7f5f0;padding:24px 40px;">
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="border-radius:10px;overflow:hidden;border:1px solid #d4c9a8;">
                                                <tr>
                                                    <td style="background-color:#628141;padding:16px 20px;">
                                                        <h3 style="margin:0;color:#EBD5AB;font-size:17px;font-weight:700;">%s</h3>
                                                        <p style="margin:5px 0 0;color:#8BAE66;font-size:13px;">by %s</p>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="background-color:#ffffff;padding:20px;">
                                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                                            <tr>
                                                                <td style="padding:10px 0;border-bottom:1px solid #f0ebe0;">
                                                                    <span style="color:#8BAE66;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">Date & Time</span><br>
                                                                    <span style="color:#1B211A;font-size:14px;font-weight:600;margin-top:4px;display:block;">%s</span>
                                                                </td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:10px 0;border-bottom:1px solid #f0ebe0;">
                                                                    <span style="color:#8BAE66;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">Venue</span><br>
                                                                    <span style="color:#1B211A;font-size:14px;font-weight:600;margin-top:4px;display:block;">%s</span>
                                                                </td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:10px 0;">
                                                                    <span style="color:#8BAE66;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:0.5px;">Club</span><br>
                                                                    <span style="color:#1B211A;font-size:14px;font-weight:600;margin-top:4px;display:block;">%s</span>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#f7f5f0;padding:0 40px 36px;text-align:center;">
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#1B211A;border-radius:10px;">
                                                <tr>
                                                    <td style="padding:28px;text-align:center;">
                                                        <p style="margin:0 0 4px;color:#8BAE66;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:2px;">Entry Pass</p>
                                                        <p style="margin:0 0 20px;color:#EBD5AB;font-size:13px;">Show this QR code at the entrance</p>
                                                        <div style="display:inline-block;background-color:#EBD5AB;padding:12px;border-radius:8px;">
                                                            <img src="cid:qrcode" width="180" height="180" style="display:block;border-radius:4px;"/>
                                                        </div>
                                                        <p style="margin:16px 0 0;color:#628141;font-size:12px;">Screenshot this email for quick access</p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#1B211A;padding:24px 40px;text-align:center;border-radius:0 0 12px 12px;">
                                            <p style="margin:0;color:#628141;font-size:12px;">This is an automated email from Campus Connect.</p>
                                            <p style="margin:6px 0 0;color:#628141;font-size:12px;">Please do not reply to this email.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                    registration.getUser().getFullName(),
                    registration.getEvent().getTitle(),
                    registration.getEvent().getClub().getName(),
                    eventDate,
                    registration.getEvent().getVenue(),
                    registration.getEvent().getClub().getName()
            ), true);

            helper.addInline("qrcode", new ByteArrayResource(qrBytes), "image/png");

            mailSender.send(message);

        } catch (Exception e) {
            // Log instead of throwing — async exceptions are swallowed anyway
            // Replace with your logger if you have one
            System.err.println("Failed to send registration email: " + e.getMessage());
        }
    }
}