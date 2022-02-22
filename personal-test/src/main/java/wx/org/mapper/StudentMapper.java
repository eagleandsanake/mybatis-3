package wx.org.mapper;


import org.apache.ibatis.annotations.Param;
import wx.org.dto.UpdateDTO;
import wx.org.entity.Student;
import wx.org.intercepter.annotation.BigDecimalScale;

import java.math.BigDecimal;

/**
 * @author wuxin
 * @date 2022/02/14 22:09:49
 */
public interface StudentMapper {


  Student getStudentById(Long id);

  int updateStudentAge(@BigDecimalScale(value = "2",roundingMode = BigDecimal.ROUND_HALF_UP) @Param("dto") UpdateDTO dto);

}
