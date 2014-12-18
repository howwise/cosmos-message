package message.mvc.convert;

import message.base.convert.ConvertGetter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * spring mvc将传递的参数转换成枚举.
 *
 * @author sunhao(sunhao.java@gmail.com)
 * @version V1.0
 * @createTime 2014-12-17 20:23
 */
public class EnumConverterFactory implements ConverterFactory<String, Enum<?>>, ConditionalConverter {
    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        Class<?> clazz = targetType.getType();
        return Enum.class.isAssignableFrom(clazz) && ConvertGetter.class.isAssignableFrom(clazz);
    }

    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        if(!ConvertGetter.class.isAssignableFrom(targetType)){
            return null;
        }

        return new ValueToEnum(targetType);
    }

    private class ValueToEnum<T extends Enum<?> & ConvertGetter> implements Converter<String, T> {
        private T[] enums;

        public ValueToEnum(Class<T> enumType) {
            this.enums = enumType.getEnumConstants();
        }

        @Override
        public T convert(String source) {
            for (T em : enums) {
                if (em.getValue().equals(source)) {
                    return em;
                }
            }
            return null;
        }
    }
}
