package com.maharecruitment.gov.in.common.sms.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.maharecruitment.gov.in.common.sms.config.AclSmsProperties;
import com.maharecruitment.gov.in.common.sms.dto.SmsGatewayRequest;
import com.maharecruitment.gov.in.common.sms.exception.SmsGatewayException;

class AclSmsGatewayClientTest {

    @Test
    void sendsExpectedAclQueryParametersAndEncodesSmsText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        AclSmsGatewayClient client = new AclSmsGatewayClient(restClient, enabledProperties());
        String message = "OTP for Maharecruitment Portal login: 930712. Valid for 10 minutes. Do not share this OTP with anyone. - MAHGOV";

        server.expect(request -> {
            URI uri = request.getURI();
            assertThat(uri.getRawQuery()).doesNotContain("OTP for Maharecruitment");
            assertThat(uri.getRawQuery()).contains("text=OTP%20for%20Maharecruitment");
            var params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
            assertThat(params.getFirst("contenttype")).isEqualTo("1");
            assertThat(params.getFirst("from")).isEqualTo("MAHGOV");
            assertThat(params.getFirst("alert")).isEqualTo("1");
            assertThat(params.getFirst("selfid")).isEqualTo("true");
            assertThat(params.getFirst("appid")).isEqualTo("1707178340749813454");
            assertThat(params.getFirst("userId")).isEqualTo("configured-user");
            assertThat(params.getFirst("pass")).isEqualTo("configured-pass");
            assertThat(params.getFirst("to")).isEqualTo("7020186501");
            assertThat(URLDecoder.decode(params.getFirst("text"), StandardCharsets.UTF_8)).isEqualTo(message);
        }).andRespond(withSuccess("Message Submitted", MediaType.TEXT_PLAIN));

        client.send(new SmsGatewayRequest("+91 7020186501", message, "corr-1", "1707178340749813454"));

        server.verify();
    }

    @Test
    void parsesAcceptedAndRejectedProviderResponses() {
        AclSmsGatewayClient client = new AclSmsGatewayClient(RestClient.builder().build(), enabledProperties());

        assertThat(client.isAcceptedResponse("Message Submitted")).isTrue();
        assertThat(client.isAcceptedResponse("ERROR: invalid credentials")).isFalse();
        assertThat(client.isAcceptedResponse("failed")).isFalse();
        assertThat(client.isAcceptedResponse("")).isFalse();
        assertThat(client.isAcceptedResponse(null)).isFalse();
    }

    @Test
    void providerErrorResponseRaisesSmsGatewayException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AclSmsGatewayClient client = new AclSmsGatewayClient(builder.build(), enabledProperties());

        server.expect(request -> { })
                .andRespond(withSuccess("ERROR invalid mobile", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.send(new SmsGatewayRequest("7020186501", "hello", "corr-2", "APP")))
                .isInstanceOf(SmsGatewayException.class)
                .hasMessageContaining("provider rejected");
    }

    @Test
    void timeoutIsWrappedWithoutCallingRealGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AclSmsGatewayClient client = new AclSmsGatewayClient(builder.build(), enabledProperties());

        server.expect(request -> { })
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.send(new SmsGatewayRequest("7020186501", "hello", "corr-3", "APP")))
                .isInstanceOf(SmsGatewayException.class)
                .hasMessageContaining("Unable to submit SMS");
    }

    private AclSmsProperties enabledProperties() {
        return new AclSmsProperties(
                "https://push3.aclgateway.com/servlet/com.aclwireless.pushconnectivity.listeners.TextListener",
                "MAHGOV",
                "1707178340749813454",
                "configured-user",
                "configured-pass",
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
                1,
                1,
                true,
                Duration.ofMinutes(10),
                Duration.ofSeconds(60),
                5,
                3);
    }
}
