package message.transaction.action;

import com.pingplusplus.model.Charge;
import message.transaction.PayException;
import message.transaction.channel.Pay;
import message.transaction.domain.Payment;
import message.transaction.enums.TradeChannel;
import message.transaction.enums.TradeResult;
import message.transaction.enums.TradeType;
import message.transaction.repository.PaymentRepository;
import message.transaction.utils.TradeRouting;
import message.transaction.utils.data.PayData;
import message.utils.EncryptUtils;
import message.utils.ParamsHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 支付.
 *
 * @author sunhao(sunhao.java@gmail.com)
 * @version V1.0, 15/9/21 下午1:01
 */
@Component
public abstract class AbstractPayAction implements PayAction {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private TradeRouting tradeRouting;

    @Override
    public Object pay(String targetId, TradeType tradeType, TradeChannel tradeChannel, Long userId, Map<String, Object> extra) throws Exception {
        Long amount = getAmount(targetId);

        // 处理各个支付的一些情况
        Pay pay = tradeRouting.pay(tradeChannel);
        Assert.notNull(pay, "支付渠道错误!");

        // 支付前校验
        pay.validatePay(amount, extra);

        // 对targetId MD5加密
        String unionId = EncryptUtils.encodeMD5(targetId);
        // 判断是否支付
        Payment payment = this.paymentRepository.findByUnionId(unionId);
        if (payment == null) {
            payment = this.prepare(targetId, tradeType, amount, tradeChannel, userId);
        } else {
            if (TradeResult.YES == payment.getTradeResult()) {
                throw new PayException(10017, "this order is paid!");
            }
        }

        beforePayment(targetId);

        PayData payData = new PayData();

        payData.setPaymentId(payment.getId());
        payData.setAmount(amount);
        payData.setProductName(payment.getProductName());
        payData.setPayDesc(payment.getDescription());
        payData.setTradeChannel(tradeChannel);

        // 将交易类型放入ThreadLocal
        ParamsHolder.set("tradeType", tradeType);

        Object result;
        try {
            result = pay.pay(payData);
        } catch (Exception e) {
            occurException(targetId, userId, e);
            throw e;
        }

        // ping++支付
        String chargeId = ((Charge) result).getId();
        payment.setChargeId(chargeId);

        this.paymentRepository.insert(payment);

        return result;
    }

    private Payment prepare(String targetId, TradeType tradeType, Long amount, TradeChannel payCode, Long userId) {
        Assert.notNull(targetId, "targetId 不能为空");
        Assert.notNull(tradeType, "交易类型不能为空");
        Assert.notNull(amount, "金额不能为空");
        Assert.notNull(payCode, "支付渠道不能为空");
        Assert.notNull(userId, "用户Id不能为空");

        Payment payment = build(targetId, tradeType, amount, payCode, userId);
        // 对targetId MD5加密
        String unionId = EncryptUtils.encodeMD5(targetId);
        payment.setUnionId(unionId);

        this.paymentRepository.insert(payment);

        afterBuild(payment);
        return payment;
    }

    /**
     * 创建支付单
     *
     * @param targetId  订单号
     * @param tradeType 交易类型
     * @param amount    交易金额
     * @param payCode   支付渠道
     * @param userId    用户ID
     * @return
     */
    public abstract Payment build(String targetId, TradeType tradeType, Long amount, TradeChannel payCode, Long userId);

    /**
     * 创建支付单之后执行
     *
     * @param payment 支付单
     */
    public abstract void afterBuild(Payment payment);

    /**
     * 获取待支付的金额
     *
     * @param targetId 要支付对象的ID
     * @return
     */
    public abstract Long getAmount(String targetId);

    /**
     * 发生异常
     *
     * @param targetId 要支付对象的ID
     * @param userId   用户ID
     * @param e        异常
     */
    public abstract void occurException(String targetId, Long userId, Exception e);
}
