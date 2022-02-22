package wx.org.entity;


import wx.org.intercepter.annotation.BigDecimalScale;

import java.math.BigDecimal;

public class Student {

	private Long id;
	private String name;

	@BigDecimalScale(value = "2",roundingMode = BigDecimal.ROUND_HALF_UP)
	private BigDecimal age;

	private Integer gender;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getAge() {
    return age;
  }

  public void setAge(BigDecimal age) {
    this.age = age;
  }

  public Integer getGender() {
    return gender;
  }

  public void setGender(Integer gender) {
    this.gender = gender;
  }

  @Override
  public String toString() {
    return "Student{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", age=" + age +
      ", gender=" + gender +
      '}';
  }
}
