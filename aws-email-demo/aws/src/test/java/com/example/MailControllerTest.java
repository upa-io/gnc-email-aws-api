/*
 * Copyright 2023 Oracle and/or its affiliates
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.email.AsyncTransactionalEmailSender;
import io.micronaut.email.Email;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.micronaut.email.BodyType.HTML;
import static io.micronaut.http.HttpStatus.ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "spec.name", value = "MailControllerTest")
@Property(name = "micronaut.email.from.email", value = "mo@gcn.example")
@MicronautTest
class MailControllerTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    BeanContext beanContext;

    @Test
    void getMailSendEndpointSendsAnEmail() {

        HttpResponse<?> response = httpClient.toBlocking().exchange(
                HttpRequest.POST("/mail/send", Map.of("to", "jo@gcn.example")));
        assertEquals(ACCEPTED, response.status());

        AsyncTransactionalEmailSender<?, ?> sender = beanContext.getBean(AsyncTransactionalEmailSender.class);
        assertTrue(sender instanceof EmailSenderReplacement);

        EmailSenderReplacement sendgridSender = (EmailSenderReplacement) sender;
        assertTrue(CollectionUtils.isNotEmpty(sendgridSender.getEmails()));
        assertEquals(1, sendgridSender.getEmails().size());

        Email email = sendgridSender.getEmails().get(0);
        assertEquals("mo@gcn.example", email.getFrom().getEmail());
        assertNotNull(email.getTo());
        assertTrue(email.getTo().stream().findFirst().isPresent());
        assertEquals("jo@gcn.example", email.getTo().stream().findFirst().get().getEmail());
        assertEquals("Sending email with Amazon SES is Fun", email.getSubject());
        assertNotNull(email.getBody());
        assertTrue(email.getBody().get(HTML).isPresent());
        assertEquals("and <em>easy</em> to do anywhere with <strong>Micronaut Email</strong>", email.getBody().get(HTML).get());
    }
}
