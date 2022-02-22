package wx.org;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import wx.org.dto.UpdateDTO;
import wx.org.mapper.StudentMapper;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;

/**
 * @author wuxin
 * @date 2022/02/14 22:12:50
 */
public class TestMybatis {

  public static void main(String[] args) throws IOException {

    // 1. 配置读出为字节流
    Reader reader = Resources.getResourceAsReader("config.xml");
    // 2.建造者模式构建SqlSessionFactory，使用建造者模式，通过build的重载，精细化构造对象
    SqlSessionFactory build = new SqlSessionFactoryBuilder().build(reader);// SqlSessiongFactory构造器
    SqlSession openSession = build.openSession();
    StudentMapper mapper = openSession.getMapper(StudentMapper.class);
    UpdateDTO updateDTO = new UpdateDTO();
    updateDTO.setAge(new BigDecimal(9.495453));
    updateDTO.setId(1L);
    int i = mapper.updateStudentAge(updateDTO);
    openSession.commit();
    System.out.println(i);

  }

}



