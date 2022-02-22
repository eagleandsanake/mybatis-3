package wx.org.dto;

import wx.org.intercepter.annotation.BigDecimalScale;

import java.math.BigDecimal;

/**
 * @author wuxin
 * @date 2022/02/22 13:13:40
 */
public class UpdateDTO {

  @BigDecimalScale(value = "2",roundingMode = BigDecimal.ROUND_UP)
  private BigDecimal age;

  private Long id;

  public BigDecimal getAge() {
    return age;
  }

  public void setAge(BigDecimal age) {
    this.age = age;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
