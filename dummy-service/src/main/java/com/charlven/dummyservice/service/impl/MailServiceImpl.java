package com.charlven.dummyservice.service.impl;

import com.charlven.dummyservice.service.MailService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;

@Service
public class MailServiceImpl implements MailService {

    private static final Log LOG = LogFactory.getLog(MailServiceImpl.class.getName());

    @Resource
    private JavaMailSender mailSender;


    @Override
    public void sendSimpleMail(String from, String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        message.setFrom(from);
        mailSender.send(message);
    }

    @Override
    public void sendHTMLMail(String from, String to, String subject, String content) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            setMessage(from, to, subject, content, helper);
            mailSender.send(message);
        } catch (Exception e) {
            LOG.warn("Send mail failed：", e);
        }
    }

    private void setMessage(String from, String to, String subject, String content, MimeMessageHelper helper)
            throws MessagingException {
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
    }

    @Override
    public void sendAttachmentMail(String from, String to, String subject, String content, String filePath) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            setMessage(from, to, subject, content, helper);
            FileSystemResource file = new FileSystemResource(new File(filePath));
            String fileName = file.getFilename();
            helper.addAttachment(fileName, file);
            mailSender.send(message);
        } catch (Exception e) {
            LOG.warn("Send mail failed：", e);
        }
    }

    @Override
    public void sendInlineResourceMail(String from, String to, String subject, String content, String rscPath, String rscId) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            setMessage(from, to, subject, content, helper);
            FileSystemResource res = new FileSystemResource(new File(rscPath));
            helper.addInline(rscId, res);
            mailSender.send(message);
        } catch (Exception e) {
            LOG.warn("Send mail failed：", e);
        }
    }


}
