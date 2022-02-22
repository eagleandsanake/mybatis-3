package wx.org.mapper;


import org.apache.ibatis.annotations.Param;
import wx.org.dto.UpdateDTO;
import wx.org.entity.Student;

/**
 * @author wuxin
 * @date 2022/02/14 22:09:49
 */
public interface StudentMapper {


  Student getStudentById(Long id);

  int updateStudentAge(@Param("dto") UpdateDTO dto);

}
