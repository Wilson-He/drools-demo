package io.github.test.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Wilson
 * @date 2019/3/29
 */
@Data
@Accessors(chain = true)
public class Customer {
    private Integer sex;
    private String name;
    private Integer age;
}
