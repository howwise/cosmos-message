package message.config;

import message.base.utils.ApplicationContextUtil;
import message.base.utils.ApplicationHelper;
import message.config.exception.ConfigException;
import message.config.loader.i18n.DefaultResourceBundleMessageSource;
import message.utils.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.DelegatingMessageSource;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * 国际化资源文件工具类
 *
 * @author sunhao(sunhao.java@gmail.com)
 * @version V1.0
 * @createTime 2012-3-14 下午07:59:37
 */
public class MessageConfig {
    //国际化资源文件处理类在spring上下文中的key
    private static final String DEFAULT_MESSAGE_SOURCE = "messageSource";

    private static DefaultResourceBundleMessageSource bundleMessageSource = null;
    private static final Object lockObject = new Object();
    private static Map<Locale, Map<String, String>> allMessagesMap = new HashMap<>();
    private static ApplicationContext context;

    static {
        synchronized (lockObject) {
            Object obj = ApplicationHelper.getInstance().getBean(DEFAULT_MESSAGE_SOURCE);

            if (obj instanceof DefaultResourceBundleMessageSource)
                bundleMessageSource = (DefaultResourceBundleMessageSource) obj;
            else if (obj instanceof DelegatingMessageSource) {
                DelegatingMessageSource delegatingMessageSource = (DelegatingMessageSource) obj;
                Object obj2 = delegatingMessageSource.getParentMessageSource();
                if (obj2 instanceof DefaultResourceBundleMessageSource)
                    bundleMessageSource = (DefaultResourceBundleMessageSource) obj2;
                else
                    throw new ConfigException(10007, "can not find any messageSource what is DefaultResourceBundleMessageSource!");
            }

            context = ApplicationContextUtil.getContext();
        }
    }

    /**
     * get message by locale, and format with args
     *
     * @param code           键
     * @param locale         语言
     * @param defaultMessage String to return if the lookup fails
     * @param args           参数
     * @return
     */
    public static String getMessage(String code, Locale locale, String defaultMessage, String... args) {
        return context.getMessage(code, args, defaultMessage, locale);
    }

    /**
     * get message by locale, and format with args
     *
     * @param code   键
     * @param locale 语言
     * @param args   参数
     * @return
     */
    public static String getMessage(String code, Locale locale, String... args) {
        return context.getMessage(code, args, StringUtils.EMPTY, locale);
    }

    /**
     * get message by locale, and format with args
     *
     * @param code 键
     * @param args 参数
     * @return
     */
    public static String getMessage(String code, String... args) {
        return context.getMessage(code, args, StringUtils.EMPTY, Locale.getDefault());
    }

    /**
     * get message by locale, and format with args
     *
     * @param code 键
     * @return
     */
    public static String getMessage(String code) {
        return context.getMessage(code, Collections.emptyList().toArray(new String[0]), StringUtils.EMPTY, Locale.getDefault());
    }

    /**
     * 获取locale语言下所有的message
     *
     * @param locale
     * @return
     */
    public static Map<String, String> getMessages(Locale locale) {
        if (locale == null)
            locale = Locale.getDefault();

        Map<String, String> allMessages = allMessagesMap.get(locale);
        if (allMessages != null)
            return allMessages;

        ResourceBundle bundle = bundleMessageSource.getResourceBundle(locale);
        Enumeration<String> keys = bundle.getKeys();

        Map<String, String> messages = new HashMap<String, String>();
        for (; keys.hasMoreElements(); ) {
            String key = keys.nextElement();
            String message = bundle.getString(key);

            messages.put(key, message);
        }

        allMessagesMap.put(locale, messages);

        return messages;
    }
}
