package io.github.test.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Wilson
 * @date 2019/3/13
 */
@Data
@Accessors(chain = true)
public class Product {
    private String type;
    private Double discount;
    private Double price;
    private Integer percent;
}
