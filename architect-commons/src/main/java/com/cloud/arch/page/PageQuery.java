package com.cloud.arch.page;

import com.google.common.collect.Lists;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.cloud.arch.page.PageConstant.*;

@Data
public class PageQuery implements Serializable {

    private Integer sort  = 1;
    @Min(value = 1, message = "page must greater than 1")
    private Integer page  = 1;
    @Range(min = 1, max = 1000, message = "page size range in 1-1000")
    private Integer limit = 10;

    public PageCondition from() {
        Class<?>    clazz  = this.getClass();
        List<Field> fields = Lists.newArrayList();
        while (clazz != null && !Object.class.equals(clazz)) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        final PageCondition condition = new PageCondition();
        fields.stream()
              .filter(v -> Modifier.isPrivate(v.getModifiers())
                      && !Modifier.isStatic(v.getModifiers())
                      && !Modifier.isFinal(v.getModifiers())
                      && Objects.isNull(v.getAnnotation(Ignore.class)))
              .forEach(v -> {
                  v.setAccessible(true);
                  switch (v.getName()) {
                      case LIMIT:
                          PageEnums.LIMIT.setValue(condition, v, this);
                          break;
                      case PAGE:
                          PageEnums.PAGE.setValue(condition, v, this);
                          break;
                      case SORT:
                          PageEnums.SORT.setValue(condition, v, this);
                          break;
                      default:
                          PageEnums.PARAM.setValue(condition, v, this);
                  }
              });
        return condition;
    }

}
