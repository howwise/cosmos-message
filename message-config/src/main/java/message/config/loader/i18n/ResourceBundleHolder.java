package message.config.loader.i18n;

import message.config.exception.ConfigException;
import message.utils.FileUtils;
import message.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * 存放消息源的容器.
 *
 * @author sunhao(sunhao.java@gmail.com)
 * @version V1.0, 13-4-15 下午11:11
 */
public class ResourceBundleHolder implements Serializable {
    /**
     * for logger
     */
    private static final Logger logger = LoggerFactory.getLogger(ResourceBundleHolder.class);

    private static final String SUFFIX_OF_PROPERTIES = ".properties";
    private static final String SUFFIX_OF_TEXT = ".txt";
    private static final String SUFFIX_OF_HTML = ".html";

    private String fileEncoding = "UTF-8";

    /**
     * 存放资源名称与值，不包含locale信息.
     */
    private Map defaultBundleMap = new HashMap();

    /**
     * 存放资源的当前顺序.
     */
    private final Map defaultBundleOrderMap = new HashMap();

    /**
     * 存放每个locale对应的资源信息.
     */
    private Map bundledMap = new HashMap();

    /**
     * 存放资源的当前顺序.
     */
    private Map bundledOrderMap = new HashMap();

    /**
     * 根据baseName加资源文件.
     * 本方法会遍历资源文件所在目录的所有以basename开头的文件，并加载该文件内的资源.
     *
     * @param baseFilePath Base file path.
     * @param order        次序.
     */
    public void loadMessageResource(String baseFilePath, int order) {
        if (StringUtils.isEmpty(baseFilePath)) {
            logger.debug("give baseFilePath is null!");
            return;
        }

        File baseFile = new File(baseFilePath);
        if (!baseFile.exists() || !baseFile.canRead()) {
            logger.debug("give file '{}' not exist or can not read!", baseFilePath);
            return;
        }

        //父级目录
        File parentDir = baseFile.getParentFile();
        //获取文件除去后缀的名称
        //baseName.properties --> baseName
        //baseName.txt        --> baseName
        //baseName.html       --> baseName
        String baseName = baseFile.getName();
        baseName = StringUtils.substringBefore(baseName, ".");

        if (logger.isDebugEnabled())
            logger.debug("get baseName for give fileName '{}' is '{}'!", baseFile.getName(), baseName);

        File[] fileChildren = parentDir.listFiles();
        for (File file : fileChildren) {
            String fileName = file.getName();
            if (fileName.startsWith(baseName) && (fileName.endsWith(SUFFIX_OF_PROPERTIES) || fileName.endsWith(SUFFIX_OF_TEXT) || fileName.endsWith(SUFFIX_OF_HTML))) {
                //匹配baseName的所有语言的properties文件
                Locale locale = null;
                try {
                    locale = this.getLocaleFromFileName(fileName, baseName);
                } catch (Exception e) {
                    logger.warn("file '{}' can not get any locale, continue!", file.getAbsolutePath());
                    continue;
                }
                if (locale == null)
                    logger.warn("get locale is null!");

                Properties properties = this.getProperties(baseName, file);

                if (properties != null)
                    this.addProperties(properties, locale, order);
                else
                    logger.warn("can not get any properties from given file '{}'!", file.getAbsolutePath());
            }
        }
    }

    /**
     * 根据文件名取得文件所属的语言环境
     *
     * @param fileName 文件名(加后缀,并且可能是各种语言的properties),eg:message.properties or message_en_US.properties
     * @param baseName 文件名(不加后缀),eg:message(上面两个文件对应的baseName都是message)
     * @return 语言
     */
    private Locale getLocaleFromFileName(String fileName, String baseName) {
        //除去文件名
        String localName = StringUtils.substring(fileName, 0, fileName.length() - (FileUtils.getFileExt(fileName).length()));
        //只取local的信息(message.properties)
        localName = StringUtils.substring(localName, baseName.length() + 1);

        if (logger.isDebugEnabled())
            logger.debug("get local name for file '{}' is '{}'!", fileName, baseName);

        if (StringUtils.isEmpty(localName))
            //message.properties(默认是中文环境)
            return null;

        String locals[] = localName.split("_");
        Locale locale = null;

        if (locals.length == 2) {
            locale = new Locale(locals[0], locals[1]);
        }
        if (locals == null) {
            throw new ConfigException(10007, "can not get locale!");
        }

        if (logger.isDebugEnabled())
            logger.debug("get locale for file '{}' is '{}'!", fileName, locale);

        return locale;
    }

    private Properties getProperties(String baseName, File file) {
        Properties properties = new Properties();

        String fileType = FileUtils.getFileExt(file);

        try {
            if (SUFFIX_OF_PROPERTIES.equals(fileType)) {
                //1.properties
                properties.load(new FileInputStream(file));
            } else if (SUFFIX_OF_TEXT.equals(fileType) || SUFFIX_OF_HTML.equals(fileType)) {
                //2.txt/html
                properties.put(baseName, FileUtils.getFileText(file, this.fileEncoding));
            } else {
                //do nothing!
                logger.debug("this file '{}' is not properties, txt, html!", file.getName());
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }

        return properties;
    }

    /**
     * 将文件中的资源放入map中
     *
     * @param src
     * @param locale
     * @param order
     */
    private void addProperties(final Map src, Locale locale, final int order) {
        if (locale == null) {
            mergePropertiesToMap(defaultBundleMap, defaultBundleOrderMap, src, order);
            locale = Locale.CHINA;
        }

        Map propertiesMap = (Map) this.bundledMap.get(locale);
        Map orderMap = (Map) this.bundledOrderMap.get(locale);

        //各种语言的property存储map
        if (propertiesMap == null) {
            propertiesMap = new HashMap();
            this.bundledMap.put(locale, propertiesMap);
        }
        if (orderMap == null) {
            orderMap = new HashMap();
            this.bundledOrderMap.put(locale, orderMap);
        }

        //对各种语言的property进行处理
        mergePropertiesToMap(propertiesMap, orderMap, src, order);
    }

    /**
     * 将所给的资源Map融合到目标Map中.
     *
     * @param destMap  目标Map.
     * @param orderMap 存放次序的Map.
     * @param src      资源.
     * @param order    次序.
     */
    private void mergePropertiesToMap(final Map destMap, final Map orderMap, final Map src, final int order) {
        for (Object keyObj : src.keySet()) {
            String key = keyObj.toString();

            if (!destMap.containsKey(key)) {
                //不存在的话就直接添加
                destMap.put(key, src.get(key));     //存储值信息
                orderMap.put(key, order);           //存储顺序的信息
            } else {
                //存在的话,则开始判断顺序
                Integer oldOrder = (Integer) orderMap.get(key);
                if (oldOrder == null)
                    //如果不存在order的值,则默认为最小的整数
                    oldOrder = Integer.MIN_VALUE;

                //如果给定的order比已经存在的order大的话,则覆盖,否则跳过
                if (order >= oldOrder) {
                    destMap.put(key, src.get(key));
                    orderMap.put(key, order);
                }
            }
        }
    }

    public ResourceBundle getResourceBundle(Locale locale) {
        return new DefaultResourceBundle(this.defaultBundleMap, this.bundledMap, locale);
    }

    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }
}
