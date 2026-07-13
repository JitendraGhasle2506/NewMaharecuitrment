package com.maharecruitment.gov.in.common.sms.client;

import com.maharecruitment.gov.in.common.sms.dto.SmsGatewayRequest;
import com.maharecruitment.gov.in.common.sms.dto.SmsGatewayResponse;

public interface SmsGatewayClient {

    SmsGatewayResponse send(SmsGatewayRequest request);
}
