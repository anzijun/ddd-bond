package com.bond.infrastructure.facadeimpl;

import com.bond.domain.exception.BizException;
import com.bond.domain.exception.Error;
import com.bond.domain.exception.net.NetErrorContext;
import com.bond.domain.exception.net.NetErrorManager;
import com.bond.domain.model.trade.Order;
import com.bond.domain.model.trade.PaymentAccount;
import com.bond.domain.model.trade.ability.facade.PaymentFacade;
import com.bond.domain.model.trade.type.PaymentResult;
import com.bond.infrastructure.common.Constant;
import com.bond.infrastructure.external.payment.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.rmi.RemoteException;

/**
 * @author anzj
 * @date 2022/12/19 14:47
 */
@Component
public class PaymentFacadeImpl implements PaymentFacade {

    private static final Logger logger = LoggerFactory.getLogger(PaymentFacadeImpl.class);

    @Override
    public PaymentResult Pay(Order order, PaymentAccount sourceAccount, PaymentAccount targetAccount) {
        PaymentResult result = null;
        String url = "https://0.0.0.1:8080/pay";
        try {
            //调用三方支付接口进行支付
            PayClient payClient = new PayClient("0.0.0.1","8080");
            PayRequest request = new PayRequest();
            request.setOrderId(order.getOrderId());
            request.setSourceAccount(sourceAccount.getAccountNo());
            request.setTargetAccount(targetAccount.getAccountNo());
            request.setAmount(order.getOrderAmount());

            PayResponse payResponse = payClient.toPay(request);

            result = new PaymentResult();

            if(Constant.PAY_SUCCEED.equals(payResponse.getCode())){
                result.setResultCode(PaymentResult.SUCCESS_CODE);
            }else{
                result.setResultCode(PaymentResult.FAILURE_CODE);
            }
        } catch (RemoteException e) {
            //支付失败则查询订单支付情况
            logger.error("订单支付出现错误");
            query(order);
        }
        return result;
    }

    public PaymentResult query(Order order){
        PaymentResult result = null;
        String url = "https://0.0.0.1:8080/query";

        try {
            QueryClient client = new QueryClient("0.0.0.1","8080");
            QueryRequest request = new QueryRequest();
            request.setOrderId(order.getOrderId());
            QueryResponse response = client.toQuery(request);
            result = new PaymentResult();
            if(Constant.PAY_SUCCEED.equals(response.getCode())){
                result.setResultCode(PaymentResult.SUCCESS_CODE);
            }else{
                result.setResultCode(PaymentResult.FAILURE_CODE);
            }
            return result;
        } catch (RemoteException e) {
            Throwable t = e.getCause();
            NetErrorContext context = new NetErrorContext();
            context.setConnectTimeout(3000);
            context.setThrowable(t);
            context.setUrl(url);
            context.setReadTimeout(3000);
            Error error = NetErrorManager.parse(context);
            throw BizException.buildBizException(error,t);
        }


    }
}
