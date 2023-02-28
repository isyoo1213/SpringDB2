package hello.itemservice.repository.mybatis;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
// *** MyBatis 매핑 XML을 호출해주는 인터페이스
//     - 이 인터페이스의 메서드가 호출되면 xml의 DML/DQL에 지정된 id의 sql을 실행
//       ex) save() -> xml의 <insert>의 id에 save로 지정된 sql 실행
// *** xml파일은 이 매핑 인터페이스의 이름/경로를 똑같이 resources에 위치해야함
public interface ItemMapper {

    void save(Item item);

    //parameter가 2개 이상일 경우 @Param을 꼭 지정해주어야 함
    void update(@Param("id") Long id, @Param("updateParam") ItemUpdateDto updateParam);

    //Mybatis는 Optional도 지원
    Optional<Item> findById(Long id);

    List<Item> findAll(ItemSearchCond cond);
}
