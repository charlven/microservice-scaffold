package com.charlven.dummyservice.service;

public interface MailService {

    void sendSimpleMail(String from, String to, String subject, String content);

    void sendHTMLMail(String from, String to, String subject, String content);

    void sendAttachmentMail(String from, String to, String subject, String content, String filePath);

    void sendInlineResourceMail(String from, String to, String subject, String content, String rscPath, String rscId);

}
